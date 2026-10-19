package isel.cd.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import isel.cd.coderunner.execution.*;
import isel.cd.coderunner.manager.GetRunServerRequest;
import isel.cd.coderunner.manager.GetRunServerResponse;
import isel.cd.coderunner.manager.ManagerServiceGrpc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ClientApp {

    private static final String DEFAULT_MANAGER_HOST = "localhost";
    private static final int DEFAULT_MANAGER_PORT = 8080;

    public static void main(String[] args) {
        String managerHost = args.length > 0 ? args[0] : DEFAULT_MANAGER_HOST;
        int managerPort = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_MANAGER_PORT;

        ManagedChannel managerChannel = ManagedChannelBuilder.forAddress(managerHost, managerPort)
                .usePlaintext()
                .build();
        ManagerServiceGrpc.ManagerServiceBlockingStub managerStub = ManagerServiceGrpc.newBlockingStub(managerChannel);

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== CodeRunner Client Iniciado ===");
        System.out.printf("Conectado ao Manager em %s:%d%n", managerHost, managerPort);

        while (true) {
            System.out.println("\nComandos disponíveis:");
            System.out.println(" 1 - Submeter programa Python");
            System.out.println(" 2 - Consultar resultado de pedido");
            System.out.println(" 0 - Sair");
            System.out.print("> ");

            String cmd = scanner.nextLine().trim();
            if ("0".equals(cmd)) {
                break;
            }

            try {
                switch (cmd) {
                    case "1" -> {
                        System.out.print("Caminho do script (.py): ");
                        String pyPath = scanner.nextLine().trim();
                        System.out.print("Caminho do ficheiro de input (opcional, ENTER para vazio): ");
                        String inPath = scanner.nextLine().trim();

                        // 1. Obter RunServer junto do Manager (Round-Robin)
                        GetRunServerResponse runServerInfo = managerStub.getRunServer(GetRunServerRequest.getDefaultInstance());
                        System.out.printf("[Client] RunServer atribuído: %s:%d%n",
                                runServerInfo.getServerHost(), runServerInfo.getServerPort());

                        // 2. Submeter via streaming de cliente
                        submitProgram(runServerInfo.getServerHost(), runServerInfo.getServerPort(), pyPath, inPath);
                    }
                    case "2" -> {
                        System.out.print("ID do Pedido (UUID): ");
                        String reqId = scanner.nextLine().trim();

                        // Pede um RunServer qualquer para consultar o estado inicial no Redis
                        GetRunServerResponse runServerInfo = managerStub.getRunServer(GetRunServerRequest.getDefaultInstance());
                        getResult(runServerInfo.getServerHost(), runServerInfo.getServerPort(), reqId);
                    }
                    default -> System.out.println("Comando inválido!");
                }
            } catch (Exception e) {
                System.err.println("Erro na operação: " + e.getMessage());
            }
        }

        managerChannel.shutdown();
        System.out.println("Cliente terminado.");
    }

    private static void submitProgram(String host, int port, String pyPath, String inPath) throws Exception {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        ExecutionServiceGrpc.ExecutionServiceStub asyncStub = ExecutionServiceGrpc.newStub(channel);

        CountDownLatch finishLatch = new CountDownLatch(1);
        AtomicReference<String> requestId = new AtomicReference<>();

        StreamObserver<SubmitResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(SubmitResponse resp) {
                requestId.set(resp.getRequestId());
                System.out.printf("%n[Sucesso] Pedido submetido! ID do Pedido: %s%n", resp.getRequestId());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("[Erro no envio]: " + t.getMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        };

        StreamObserver<ProgramChunk> requestObserver = asyncStub.submitProgram(responseObserver);

        File pyFile = new File(pyPath);
        if (!pyFile.exists()) {
            System.err.println("Ficheiro Python não encontrado: " + pyPath);
            channel.shutdown();
            return;
        }

        // Enviar metadados
        boolean hasInput = inPath != null && !inPath.isBlank() && new File(inPath).exists();
        requestObserver.onNext(ProgramChunk.newBuilder()
                .setMetadata(SubmitMetadata.newBuilder()
                        .setFilename(pyFile.getName())
                        .setHasInput(hasInput)
                        .build())
                .build());

        // Enviar chunks do script Python
        sendFileInChunks(pyFile, requestObserver, true);

        // Enviar chunks do ficheiro de input se existir
        if (hasInput) {
            sendFileInChunks(new File(inPath), requestObserver, false);
        }

        requestObserver.onCompleted();

        if (!finishLatch.await(30, TimeUnit.SECONDS)) {
            System.err.println("Timeout à espera de resposta do servidor.");
        }

        channel.shutdown();
    }

    private static void sendFileInChunks(File file, StreamObserver<ProgramChunk> stream, boolean isProgram) throws IOException {
        byte[] buffer = new byte[4096];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read;
            while ((read = fis.read(buffer)) > 0) {
                ByteString data = ByteString.copyFrom(buffer, 0, read);
                ProgramChunk.Builder chunkBuilder = ProgramChunk.newBuilder();
                if (isProgram) {
                    chunkBuilder.setProgContent(data);
                } else {
                    chunkBuilder.setInputContent(data);
                }
                stream.onNext(chunkBuilder.build());
            }
        }
    }

    private static void getResult(String host, int port, String reqId) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        ExecutionServiceGrpc.ExecutionServiceBlockingStub blockingStub = ExecutionServiceGrpc.newBlockingStub(channel);

        Iterator<ResultChunk> chunks;
        try {
            chunks = blockingStub.getResult(ResultRequest.newBuilder().setRequestId(reqId).build());
        } catch (Exception e) {
            System.err.println("Erro ao obter resultado: " + e.getMessage());
            channel.shutdown();
            return;
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        ExecutionStatus status = ExecutionStatus.UNKNOWN;
        int exitCode = -1;
        String execServer = "";

        while (chunks.hasNext()) {
            ResultChunk chunk = chunks.next();
            status = chunk.getStatus();
            exitCode = chunk.getExitCode();
            execServer = chunk.getExecutionServer();

            if (chunk.hasStdoutChunk()) {
                stdout.append(chunk.getStdoutChunk().toString(StandardCharsets.UTF_8));
            }
            if (chunk.hasStderrChunk()) {
                stderr.append(chunk.getStderrChunk().toString(StandardCharsets.UTF_8));
            }
        }

        channel.shutdown();

        System.out.printf("%n=== Relatório do Pedido [%s] ===%n", reqId);
        System.out.println("Estado           : " + status);
        System.out.println("Servidor Executor: " + execServer);
        System.out.println("Código de Saída  : " + exitCode);

        if (!stdout.isEmpty()) {
            System.out.println("--- STDOUT ---");
            System.out.println(stdout);
        }
        if (!stderr.isEmpty()) {
            System.out.println("--- STDERR ---");
            System.out.println(stderr);
        }
    }
}