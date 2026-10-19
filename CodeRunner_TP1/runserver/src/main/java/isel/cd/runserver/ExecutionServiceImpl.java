package isel.cd.runserver;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import isel.cd.coderunner.execution.ExecutionServiceGrpc;
import isel.cd.coderunner.execution.ExecutionStatus;
import isel.cd.coderunner.execution.ProgramChunk;
import isel.cd.coderunner.execution.ResultChunk;
import isel.cd.coderunner.execution.ResultRequest;
import isel.cd.coderunner.execution.SubmitResponse;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutionServiceImpl extends ExecutionServiceGrpc.ExecutionServiceImplBase {

    private final String serverAddress;
    private final JedisPool jedisPool;
    private final DockerRunner dockerRunner;
    private final ExecutorService workerPool = Executors.newCachedThreadPool();
    private final Path baseWorkDir = Path.of("requests");

    public ExecutionServiceImpl(String host, int port, String redisHost, int redisPort) {
        this.serverAddress = host + ":" + port;
        this.jedisPool = new JedisPool(redisHost, redisPort);
        this.dockerRunner = new DockerRunner();
        try {
            Files.createDirectories(baseWorkDir);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao criar diretoria base de trabalho", e);
        }
    }

    @Override
    public StreamObserver<ProgramChunk> submitProgram(StreamObserver<SubmitResponse> responseObserver) {
        return new StreamObserver<>() {
            private final ByteArrayOutputStream progBuffer = new ByteArrayOutputStream();
            private final ByteArrayOutputStream inputBuffer = new ByteArrayOutputStream();

            @Override
            public void onNext(ProgramChunk chunk) {
                try {
                    if (chunk.hasProgContent()) {
                        chunk.getProgContent().writeTo(progBuffer);
                    } else if (chunk.hasInputContent()) {
                        chunk.getInputContent().writeTo(inputBuffer);
                    }
                } catch (IOException e) {
                    onError(Status.INTERNAL.withDescription("Erro ao receber chunks").asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("[RunServer] Erro no stream do cliente: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                String requestId = UUID.randomUUID().toString();
                Path reqDir = baseWorkDir.resolve(requestId);

                try {
                    Files.createDirectories(reqDir);
                    Files.write(reqDir.resolve("prog.py"), progBuffer.toByteArray());
                    Files.write(reqDir.resolve("input.txt"), inputBuffer.toByteArray());

                    // Registo imediato no Redis: estado PENDENTE com TTL de 24h (86400s)
                    try (Jedis jedis = jedisPool.getResource()) {
                        String key = "pedido:" + requestId;
                        jedis.hset(key, Map.of(
                                "estado", "PENDENTE",
                                "servidor", serverAddress
                        ));
                        jedis.expire(key, 86400);
                    }

                    // Resposta imediata ao cliente com o ID do pedido
                    responseObserver.onNext(SubmitResponse.newBuilder().setRequestId(requestId).build());
                    responseObserver.onCompleted();

                    // Execução assíncrona em segundo plano
                    workerPool.submit(() -> processExecution(requestId, reqDir));

                } catch (Exception e) {
                    responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
                }
            }
        };
    }

    private void processExecution(String requestId, Path reqDir) {
        DockerRunner.ExecutionOutcome outcome = dockerRunner.runCode(requestId, reqDir);

        // Atualização do estado no Redis
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "pedido:" + requestId;
            jedis.hset(key, Map.of(
                    "estado", outcome.status(),
                    "exitCode", String.valueOf(outcome.exitCode()),
                    "servidor", serverAddress
            ));
        }
        System.out.printf("[RunServer] Pedido %s concluído com estado %s (exit: %d)%n",
                requestId, outcome.status(), outcome.exitCode());
    }

    @Override
    public void getResult(ResultRequest request, StreamObserver<ResultChunk> responseObserver) {
        String requestId = request.getRequestId();
        Map<String, String> data;

        try (Jedis jedis = jedisPool.getResource()) {
            data = jedis.hgetAll("pedido:" + requestId);
        }

        if (data == null || data.isEmpty()) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Pedido desconhecido").asRuntimeException());
            return;
        }

        String statusStr = data.getOrDefault("estado", "PENDENTE");
        String execServer = data.getOrDefault("servidor", "");
        int exitCode = Integer.parseInt(data.getOrDefault("exitCode", "-1"));

        ExecutionStatus statusEnum = switch (statusStr) {
            case "PENDENTE" -> ExecutionStatus.PENDENTE;
            case "CONCLUIDO" -> ExecutionStatus.CONCLUIDO;
            case "TIMEOUT" -> ExecutionStatus.TIMEOUT;
            default -> ExecutionStatus.ERRO;
        };

        // Envia sempre um primeiro chunk com o estado consolidado
        responseObserver.onNext(ResultChunk.newBuilder()
                .setStatus(statusEnum)
                .setExitCode(exitCode)
                .setExecutionServer(execServer)
                .build());

        // Se ainda estiver pendente, termina aqui
        if (statusEnum == ExecutionStatus.PENDENTE) {
            responseObserver.onCompleted();
            return;
        }

        // Se o pedido correu neste servidor, envia os conteúdos de stdout e stderr
        Path reqDir = baseWorkDir.resolve(requestId);
        if (Files.exists(reqDir)) {
            sendFileStream(reqDir.resolve("out.txt").toFile(), true, statusEnum, exitCode, execServer, responseObserver);
            sendFileStream(reqDir.resolve("err.txt").toFile(), false, statusEnum, exitCode, execServer, responseObserver);

            deleteDirectory(reqDir);
        }

        responseObserver.onCompleted();
    }

    private void sendFileStream(File file, boolean isStdout, ExecutionStatus status, int exitCode,
                                String execServer, StreamObserver<ResultChunk> observer) {
        if (!file.exists()) return;
        byte[] buffer = new byte[4096];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read;
            while ((read = fis.read(buffer)) > 0) {
                ByteString chunkData = ByteString.copyFrom(buffer, 0, read);
                ResultChunk.Builder builder = ResultChunk.newBuilder()
                        .setStatus(status)
                        .setExitCode(exitCode)
                        .setExecutionServer(execServer);

                if (isStdout) {
                    builder.setStdoutChunk(chunkData);
                } else {
                    builder.setStderrChunk(chunkData);
                }
                observer.onNext(builder.build());
            }
        } catch (IOException e) {
            System.err.println("[RunServer] Erro ao enviar ficheiro: " + e.getMessage());
        }
    }

    private void deleteDirectory(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {
        }
    }
}