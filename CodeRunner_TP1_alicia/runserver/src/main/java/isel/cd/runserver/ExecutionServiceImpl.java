
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
import redis.clients.jedis.RedisClient;

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

/**
 * Esta classe implementa o serviço de execução do RunServer.
 *
 * Recebe os programas Python através de gRPC, guarda os ficheiros
 * necessários, regista os pedidos no Redis e executa os programas
 * através de contentores Docker.
 *
 * Para controlar os recursos, utilizo um número máximo de N
 * execuções simultâneas. Os restantes pedidos aguardam numa fila,
 * mantendo o estado PENDENTE no Redis.
 */
public class ExecutionServiceImpl
        extends ExecutionServiceGrpc.ExecutionServiceImplBase {

    // Endereço deste RunServer, utilizado para identificar
    // o servidor responsável por cada pedido.
    private final String serverAddress;

    // Ligação ao Redis, onde guardo os estados dos pedidos.
    private final RedisClient redis;

    // Classe responsável pela execução dos programas em Docker.
    private final DockerRunner dockerRunner;

    // Pasta onde guardo os ficheiros dos pedidos deste RunServer.
    private final Path baseWorkDir;

    // Número máximo de execuções simultâneas.
    // Pode ser configurado através da variável MAX_CONTAINERS.
    private final int maxContainers;

    // Pool de threads com um número fixo de trabalhadores.
    // Os pedidos adicionais ficam numa fila à espera de uma vaga.
    private final ExecutorService workerPool;


    /**
     * Inicializo o RunServer e os recursos necessários.
     */
    public ExecutionServiceImpl(
            String host,
            int port,
            String redisHost,
            int redisPort) {

        this.serverAddress = host + ":" + port;

        // Utilizo o porto para separar as pastas dos diferentes RunServers.
        this.baseWorkDir = Path.of("requests", String.valueOf(port));

        // Estabeleço a ligação ao Redis.
        this.redis = RedisClient.create(redisHost, redisPort);

        // Inicializo o responsável pela execução dos contentores.
        this.dockerRunner = new DockerRunner();

        // Leio o número máximo de execuções simultâneas.
        this.maxContainers = readMaxContainers();

        // Crio um pool com exatamente N threads.
        // Se todas estiverem ocupadas, os novos pedidos ficam em fila.
        this.workerPool = Executors.newFixedThreadPool(maxContainers);

        try {
            // Garanto que a pasta de trabalho existe.
            Files.createDirectories(baseWorkDir);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Falha ao criar diretoria base de trabalho", e
            );
        }

        System.out.printf(
                "[RunServer] %s iniciado com limite de %d contentores simultâneos.%n",
                serverAddress,
                maxContainers
        );
    }


    /**
     * Obtém o número máximo de contentores a partir
     * da variável de ambiente MAX_CONTAINERS.
     *
     * Se não estiver definida, utilizo o valor 3.
     */
    private static int readMaxContainers() {

        String value = System.getenv("MAX_CONTAINERS");

        if (value == null || value.isBlank()) {
            return 3;
        }

        try {
            int n = Integer.parseInt(value);

            if (n <= 0) {
                throw new IllegalArgumentException(
                        "MAX_CONTAINERS tem de ser superior a zero."
                );
            }

            return n;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "MAX_CONTAINERS tem de ser um número inteiro.", e
            );
        }
    }


    /**
     * Recebe o programa Python e o ficheiro de entrada por streaming gRPC.
     *
     * Depois de receber os dados, crio um UUID, guardo os ficheiros
     * e registo o pedido no Redis com estado PENDENTE.
     *
     * A execução é colocada numa fila para não bloquear o cliente.
     */
    @Override
    public StreamObserver<ProgramChunk> submitProgram(
            StreamObserver<SubmitResponse> responseObserver) {

        return new StreamObserver<>() {

            // Buffer para juntar os blocos do programa Python.
            private final ByteArrayOutputStream progBuffer =
                    new ByteArrayOutputStream();

            // Buffer para juntar os blocos do ficheiro stdin.
            private final ByteArrayOutputStream inputBuffer =
                    new ByteArrayOutputStream();

            @Override
            public void onNext(ProgramChunk chunk) {

                try {
                    // Identifico se o bloco recebido pertence ao programa.
                    if (chunk.hasProgContent()) {
                        chunk.getProgContent().writeTo(progBuffer);

                        // Caso contrário, verifico se pertence ao stdin.
                    } else if (chunk.hasInputContent()) {
                        chunk.getInputContent().writeTo(inputBuffer);
                    }

                } catch (IOException e) {
                    // Registo o problema ao receber os dados.
                    System.err.println(
                            "[RunServer] Erro ao receber chunks: "
                                    + e.getMessage()
                    );
                }
            }

            @Override
            public void onError(Throwable t) {

                // Registo os erros ocorridos na comunicação com o cliente.
                System.err.println(
                        "[RunServer] Erro no stream do cliente: "
                                + t.getMessage()
                );
            }

            @Override
            public void onCompleted() {

                // Gero um identificador único para o pedido.
                String requestId = UUID.randomUUID().toString();

                // Cada pedido fica guardado numa pasta própria.
                Path reqDir = baseWorkDir.resolve(requestId);

                boolean responseSent = false;

                try {
                    // Crio a pasta do pedido.
                    Files.createDirectories(reqDir);

                    // Guardo o programa Python recebido.
                    Files.write(
                            reqDir.resolve("prog.py"),
                            progBuffer.toByteArray()
                    );

                    // Guardo o stdin, mesmo que esteja vazio.
                    Files.write(
                            reqDir.resolve("input.txt"),
                            inputBuffer.toByteArray()
                    );

                    String key = "pedido:" + requestId;

                    // Registo imediatamente o pedido no Redis.
                    // Enquanto estiver na fila ou em execução,
                    // o estado continua a ser PENDENTE.
                    redis.hset(key, Map.of(
                            "estado", "PENDENTE",
                            "servidor", serverAddress
                    ));

                    // O registo expira após 24 horas.
                    redis.expire(key, 86400);

                    // Coloco a execução no pool.
                    // Se as N threads estiverem ocupadas,
                    // o pedido fica automaticamente em espera.
                    workerPool.submit(
                            () -> processExecution(requestId, reqDir)
                    );

                    // Devolvo o UUID ao cliente sem esperar
                    // pelo fim da execução do programa Python.
                    responseObserver.onNext(
                            SubmitResponse.newBuilder()
                                    .setRequestId(requestId)
                                    .build()
                    );

                    responseSent = true;
                    responseObserver.onCompleted();

                    System.out.printf(
                            "[RunServer] Pedido %s recebido e colocado na fila.%n",
                            requestId
                    );

                } catch (Exception e) {

                    System.err.printf(
                            "[RunServer] Falha ao submeter pedido %s: %s%n",
                            requestId,
                            e.getMessage()
                    );

                    // Só envio um erro gRPC se ainda não tiver
                    // começado a enviar a resposta ao cliente.
                    if (!responseSent) {
                        responseObserver.onError(
                                Status.INTERNAL
                                        .withDescription(
                                                "Erro ao preparar o pedido: "
                                                        + e.getMessage()
                                        )
                                        .asRuntimeException()
                        );
                    }
                }
            }
        };
    }


    /**
     * Executa o programa Python em segundo plano.
     *
     * Este método só começa quando existe uma thread disponível.
     * No final, atualizo o Redis com o estado e o código de saída.
     */
    private void processExecution(String requestId, Path reqDir) {

        System.out.printf(
                "[RunServer] A iniciar execução do pedido %s%n",
                requestId
        );

        // Peço ao DockerRunner para executar o programa.
        DockerRunner.ExecutionOutcome outcome =
                dockerRunner.runCode(requestId, reqDir);

        // Atualizo o estado final do pedido no Redis.
        String key = "pedido:" + requestId;

        redis.hset(key, Map.of(
                "estado", outcome.status(),
                "exitCode", String.valueOf(outcome.exitCode()),
                "servidor", serverAddress
        ));

        System.out.printf(
                "[RunServer] Pedido %s terminou: %s (exit: %d)%n",
                requestId,
                outcome.status(),
                outcome.exitCode()
        );
    }


    /**
     * Permite consultar o estado e os resultados de um pedido.
     *
     * O Redis contém o estado e o endereço do servidor executor.
     * Os ficheiros stdout e stderr ficam guardados localmente
     * no RunServer responsável pela execução.
     */
    @Override
    public void getResult(
            ResultRequest request,
            StreamObserver<ResultChunk> responseObserver) {

        String requestId = request.getRequestId();

        // Consulto o Redis para obter os dados do pedido.
        Map<String, String> data =
                redis.hgetAll("pedido:" + requestId);

        if (data == null || data.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Pedido desconhecido")
                            .asRuntimeException()
            );
            return;
        }

        String statusStr =
                data.getOrDefault("estado", "PENDENTE");

        String execServer =
                data.getOrDefault("servidor", "");

        int exitCode = Integer.parseInt(
                data.getOrDefault("exitCode", "-1")
        );

        // Converto o estado do Redis para o enum utilizado pelo gRPC.
        ExecutionStatus statusEnum = switch (statusStr) {
            case "PENDENTE" -> ExecutionStatus.PENDENTE;
            case "CONCLUIDO" -> ExecutionStatus.CONCLUIDO;
            case "TIMEOUT" -> ExecutionStatus.TIMEOUT;
            default -> ExecutionStatus.ERRO;
        };

        // Envio primeiro o estado geral do pedido.
        responseObserver.onNext(
                ResultChunk.newBuilder()
                        .setStatus(statusEnum)
                        .setExitCode(exitCode)
                        .setExecutionServer(execServer)
                        .build()
        );

        // Se ainda estiver pendente ou se o cliente
        // apenas quiser consultar o estado, termino aqui.
        if (statusEnum == ExecutionStatus.PENDENTE || request.getStatusOnly()) {

            responseObserver.onCompleted();
            return;
        }

        // Verifico se o pedido foi executado neste RunServer.
        boolean executedHere =
                serverAddress.equals(execServer);

        Path reqDir = baseWorkDir.resolve(requestId);

        if (executedHere && Files.exists(reqDir)) {

            // Envio a saída normal do programa.
            sendFileStream(
                    reqDir.resolve("out.txt").toFile(),
                    true,
                    statusEnum,
                    exitCode,
                    execServer,
                    responseObserver
            );

            // Envio as mensagens de erro.
            sendFileStream(
                    reqDir.resolve("err.txt").toFile(),
                    false,
                    statusEnum,
                    exitCode,
                    execServer,
                    responseObserver
            );

            // Removo os ficheiros depois de enviar os resultados.
            deleteDirectory(reqDir);
        }

        responseObserver.onCompleted();
    }


    /**
     * Envia os ficheiros stdout e stderr por streaming gRPC.
     *
     * Utilizo blocos de 4096 bytes para não precisar
     * de carregar o ficheiro completo para memória.
     */
    private void sendFileStream(
            File file,
            boolean isStdout,
            ExecutionStatus status,
            int exitCode,
            String execServer,
            StreamObserver<ResultChunk> observer) {

        if (!file.exists()) {
            return;
        }

        byte[] buffer = new byte[4096];

        try (FileInputStream fis = new FileInputStream(file)) {

            int read;

            while ((read = fis.read(buffer)) > 0) {

                ByteString chunkData =
                        ByteString.copyFrom(buffer, 0, read);

                ResultChunk.Builder builder =
                        ResultChunk.newBuilder()
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

            System.err.println(
                    "[RunServer] Erro ao enviar ficheiro: "
                            + e.getMessage()
            );
        }
    }


    /**
     * Remove a pasta do pedido e os respetivos ficheiros.
     *
     * Faço a eliminação pela ordem inversa,
     * apagando primeiro os ficheiros e depois as pastas.
     */
    private void deleteDirectory(Path path) {

        try (var paths = Files.walk(path)) {

            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (!file.delete()) {
                            System.err.println(
                                    "[RunServer] Não foi possível apagar: "
                                            + file.getAbsolutePath()
                            );
                        }
                    });

        } catch (IOException e) {

            System.err.println(
                    "[RunServer] Erro ao limpar diretório: "
                            + e.getMessage()
            );
        }
    }
}
