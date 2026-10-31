
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

        ManagerServiceGrpc.ManagerServiceBlockingStub managerStub =
                ManagerServiceGrpc.newBlockingStub(managerChannel);

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== CodeRunner Client Iniciado ===");
        System.out.printf("Conectado ao Manager em %s:%d%n", managerHost, managerPort);

        while (true) {
            System.out.println("\nComandos disponíveis:");
            System.out.println(" 1 - Submeter programa Python");
            System.out.println(" 2 - Obter resultado do pedido");
            System.out.println(" 3 - Mostrar estado");
            System.out.println(" 0 - Sair");
            System.out.print("> ");

            String cmd = scanner.nextLine().trim();

            if ("0".equals(cmd)) {
                break;
            }

            try {
                switch (cmd) {
                    case "1" -> {
                        System.out.println("Escreve o caminho do ficheiro Python que quer correr:");
                        String pyPath = scanner.nextLine().trim();

                        File pyFile = new File(pyPath);

                        if (!pyFile.isFile()
                                || !pyFile.canRead()
                                || !pyFile.getName().toLowerCase().endsWith(".py")) {
                            System.err.println("Ficheiro Python inválido ou inacessível: " + pyPath);
                            break;
                        }

                        GetRunServerResponse runServerInfo =
                                managerStub.getRunServer(GetRunServerRequest.getDefaultInstance());

                        System.out.printf("[Client] RunServer atribuído: %s:%d%n",
                                runServerInfo.getServerHost(),
                                runServerInfo.getServerPort());

                        System.out.printf("[Client] A enviar o programa diretamente para o RunServer %s:%d%n",
                                runServerInfo.getServerHost(),
                                runServerInfo.getServerPort());

                        String requestId = submitProgram(
                                runServerInfo.getServerHost(),
                                runServerInfo.getServerPort(),
                                pyPath,
                                ""
                        );

                        if (requestId != null) {
                            Report report = fetchReport(
                                    runServerInfo.getServerHost(),
                                    runServerInfo.getServerPort(),
                                    requestId,
                                    true
                            );

                            if (report != null) {
                                System.out.printf("%nEstado: %s%n", report.status);
                            }
                        }
                    }

                    case "2" -> {
                        System.out.println("Escreve o UUID do pedido:");
                        String reqId = scanner.nextLine().trim();

                        GetRunServerResponse runServerInfo =
                                managerStub.getRunServer(GetRunServerRequest.getDefaultInstance());

                        obtainResult(
                                runServerInfo.getServerHost(),
                                runServerInfo.getServerPort(),
                                reqId
                        );
                    }

                    case "3" -> {
                        System.out.println("Escreve o UUID do pedido:");
                        String reqId = scanner.nextLine().trim();

                        GetRunServerResponse runServerInfo =
                                managerStub.getRunServer(GetRunServerRequest.getDefaultInstance());

                        showState(
                                runServerInfo.getServerHost(),
                                runServerInfo.getServerPort(),
                                reqId
                        );
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

    private static String submitProgram(
            String host,
            int port,
            String pyPath,
            String inPath) throws Exception {

        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .build();

        ExecutionServiceGrpc.ExecutionServiceStub asyncStub =
                ExecutionServiceGrpc.newStub(channel);

        CountDownLatch finishLatch = new CountDownLatch(1);
        AtomicReference<String> requestId = new AtomicReference<>();

        StreamObserver<SubmitResponse> responseObserver = new StreamObserver<>() {

            @Override
            public void onNext(SubmitResponse resp) {
                requestId.set(resp.getRequestId());

                System.out.printf(
                        "%n[Sucesso] Pedido submetido! ID do Pedido: %s%n",
                        resp.getRequestId()
                );

                System.out.println("Deve guardar a chave do pedido UUID.");
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

        File pyFile = new File(pyPath);

        if (!pyFile.isFile() || !pyFile.canRead()) {
            System.err.println("Ficheiro Python não encontrado ou inacessível: " + pyPath);
            channel.shutdown();
            return null;
        }

        if (inPath != null && !inPath.isBlank()) {
            File inputFile = new File(inPath);

            if (!inputFile.isFile() || !inputFile.canRead()) {
                System.err.println("Ficheiro de entrada não encontrado ou inacessível: " + inPath);
                channel.shutdown();
                return null;
            }
        }

        StreamObserver<ProgramChunk> requestObserver =
                asyncStub.submitProgram(responseObserver);

        boolean hasInput = inPath != null && !inPath.isBlank();

        requestObserver.onNext(
                ProgramChunk.newBuilder()
                        .setMetadata(
                                SubmitMetadata.newBuilder()
                                        .setFilename(pyFile.getName())
                                        .setHasInput(hasInput)
                                        .build()
                        )
                        .build()
        );

        sendFileInChunks(pyFile, requestObserver, true);

        if (hasInput) {
            sendFileInChunks(new File(inPath), requestObserver, false);
        }

        requestObserver.onCompleted();

        if (!finishLatch.await(30, TimeUnit.SECONDS)) {
            System.err.println("Timeout à espera de resposta do servidor.");
            channel.shutdown();
            return null;
        }

        channel.shutdown();
        return requestId.get();
    }

    private static void sendFileInChunks(
            File file,
            StreamObserver<ProgramChunk> stream,
            boolean isProgram) throws IOException {

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

    private record Report(
            ExecutionStatus status,
            int exitCode,
            String execServer,
            String stdout,
            String stderr) {}

    private static void showState(String host, int port, String reqId) {
        Report report = fetchReport(host, port, reqId, true);

        if (report == null) {
            return;
        }

        System.out.printf(
                "%nEstado do pedido [%s]: %s%n",
                reqId,
                report.status
        );
    }

    private static void obtainResult(String host, int port, String reqId) {
        Report report = fetchReport(host, port, reqId, true);

        if (report == null) {
            return;
        }

        if (report.status == ExecutionStatus.PENDENTE
                || report.status == ExecutionStatus.UNKNOWN) {

            System.out.printf(
                    "%nEstado do pedido [%s]: %s%n",
                    reqId,
                    report.status
            );

            return;
        }

        String execHost = host;
        int execPort = port;

        if (!report.execServer.isBlank()
                && !report.execServer.equals(host + ":" + port)) {

            String[] parts = report.execServer.split(":");

            if (parts.length == 2) {
                try {
                    execHost = parts[0];
                    execPort = Integer.parseInt(parts[1]);

                    System.out.printf(
                            "[Client] Output no servidor executor %s%n",
                            report.execServer
                    );

                } catch (NumberFormatException e) {
                    System.err.println(
                            "Endereço do servidor executor inválido: "
                                    + report.execServer
                    );
                }
            }
        }

        Report full = fetchReport(execHost, execPort, reqId, false);

        if (full != null) {
            printReport(reqId, full);
        }
    }

    private static void printReport(String reqId, Report report) {
        System.out.printf(
                "%n=== Relatório do Pedido [%s] ===%n",
                reqId
        );

        System.out.println("Estado           : " + report.status);
        System.out.println("Servidor Executor: " + report.execServer);
        System.out.println("Código de Saída  : " + report.exitCode);

        if (!report.stdout.isEmpty()) {
            System.out.println("--- STDOUT ---");
            System.out.println(report.stdout);
        }

        if (!report.stderr.isEmpty()) {
            System.out.println("--- STDERR ---");
            System.out.println(report.stderr);
        }
    }

    private static Report fetchReport(
            String host,
            int port,
            String reqId,
            boolean statusOnly) {

        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .build();

        ExecutionServiceGrpc.ExecutionServiceBlockingStub blockingStub =
                ExecutionServiceGrpc.newBlockingStub(channel);

        Iterator<ResultChunk> chunks;

        try {
            chunks = blockingStub.getResult(
                    ResultRequest.newBuilder()
                            .setRequestId(reqId)
                            .setStatusOnly(statusOnly)
                            .build()
            );

        } catch (Exception e) {
            System.err.println("Erro ao obter resultado: " + e.getMessage());
            channel.shutdown();
            return null;
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        ExecutionStatus status = ExecutionStatus.UNKNOWN;
        int exitCode = -1;
        String execServer = "";

        try {
            while (chunks.hasNext()) {
                ResultChunk chunk = chunks.next();

                status = chunk.getStatus();
                exitCode = chunk.getExitCode();
                execServer = chunk.getExecutionServer();

                if (chunk.hasStdoutChunk()) {
                    stdout.append(
                            chunk.getStdoutChunk().toString(StandardCharsets.UTF_8)
                    );
                }

                if (chunk.hasStderrChunk()) {
                    stderr.append(
                            chunk.getStderrChunk().toString(StandardCharsets.UTF_8)
                    );
                }
            }

        } catch (Exception e) {
            System.err.println("Erro ao obter resultado: " + e.getMessage());
            channel.shutdown();
            return null;
        }

        channel.shutdown();

        return new Report(
                status,
                exitCode,
                execServer,
                stdout.toString(),
                stderr.toString()
        );
    }
}
