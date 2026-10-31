
package isel.cd.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import isel.cd.coderunner.execution.ExecutionServiceGrpc;
import isel.cd.coderunner.execution.ExecutionStatus;
import isel.cd.coderunner.execution.ProgramChunk;
import isel.cd.coderunner.execution.ResultChunk;
import isel.cd.coderunner.execution.ResultRequest;
import isel.cd.coderunner.execution.SubmitResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Demonstração da fila de execução do CodeRunner.
 *
 * Envio cinco programas Python ao mesmo RunServer.
 * O servidor está configurado para executar apenas dois
 * contentores em simultâneo.
 *
 * Assim, os restantes pedidos devem aguardar em fila
 * até existir uma thread disponível.
 */
public class DemoFila {

    private static final String HOST = "localhost";
    private static final int PORT = 9002;

    private static final int TOTAL_PEDIDOS = 5;

    // Os programas demoram cerca de 7 segundos.
    // É tempo suficiente para observar a fila no Redis e no Docker.
    private static final String PYTHON_CODE = """
            import time
            print("Inicio da execucao", flush=True)
            time.sleep(7)
            print("Fim da execucao", flush=True)
            """;

    public static void main(String[] args) throws Exception {

        // Estabeleço a ligação diretamente ao RunServer 9002.
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(HOST, PORT)
                .usePlaintext()
                .build();

        // Stub assíncrono para enviar os programas por streaming.
        ExecutionServiceGrpc.ExecutionServiceStub asyncStub =
                ExecutionServiceGrpc.newStub(channel);

        // Stub bloqueante para consultar o estado dos pedidos.
        ExecutionServiceGrpc.ExecutionServiceBlockingStub blockingStub =
                ExecutionServiceGrpc.newBlockingStub(channel);

        // Utilizo cinco threads para submeter os pedidos em simultâneo.
        ExecutorService pool =
                Executors.newFixedThreadPool(TOTAL_PEDIDOS);

        List<String> requestIds = new ArrayList<>();

        try {
            System.out.println("=== DEMO FILA CODERUNNER ===");
            System.out.println("RunServer: " + HOST + ":" + PORT);
            System.out.println("Pedidos: " + TOTAL_PEDIDOS);
            System.out.println("Limite esperado: 2 contentores");
            System.out.println();

            // Lanço cinco submissões concorrentes.
            List<java.util.concurrent.Future<String>> futures =
                    new ArrayList<>();

            for (int i = 1; i <= TOTAL_PEDIDOS; i++) {

                final int pedido = i;

                futures.add(pool.submit(() ->
                        submitProgram(asyncStub, pedido)
                ));
            }

            // Aguardo apenas pelos UUID, não pela execução dos programas.
            for (var future : futures) {
                requestIds.add(future.get(15, TimeUnit.SECONDS));
            }

            System.out.println();
            System.out.println("=== PEDIDOS SUBMETIDOS ===");

            for (String id : requestIds) {
                System.out.println("UUID: " + id);
            }

            // Aguardo um segundo para permitir o início dos contentores.
            Thread.sleep(1000);

            System.out.println();
            System.out.println("=== ESTADOS APOS 1 SEGUNDO ===");

            for (String id : requestIds) {
                consultarEstado(blockingStub, id);
            }

            System.out.println();
            System.out.println("Consulta os contentores com: docker ps");
            System.out.println("Os pedidos em espera continuam PENDENTE.");
            System.out.println();

            // Aguardo até todos os pedidos terminarem.
            long inicio = System.currentTimeMillis();

            int concluidos = 0;

            while (concluidos < TOTAL_PEDIDOS) {

                concluidos = 0;

                for (String id : requestIds) {

                    ExecutionStatus status =
                            obterEstado(blockingStub, id);

                    if (status != ExecutionStatus.PENDENTE) {
                        concluidos++;
                    }
                }

                System.out.printf(
                        "Pedidos terminados: %d/%d%n",
                        concluidos,
                        TOTAL_PEDIDOS
                );

                if (concluidos < TOTAL_PEDIDOS) {
                    Thread.sleep(2000);
                }
            }

            long duracao =
                    System.currentTimeMillis() - inicio;

            System.out.println();
            System.out.println("=== RESULTADO FINAL ===");

            for (String id : requestIds) {
                consultarEstado(blockingStub, id);
            }

            System.out.printf(
                    "Tempo de espera observado: %.2f segundos%n",
                    duracao / 1000.0
            );

            System.out.println();
            System.out.println("Teste terminado.");

        } finally {

            pool.shutdownNow();
            channel.shutdownNow();
        }
    }


    /**
     * Envia um programa Python por streaming gRPC
     * e devolve o UUID recebido do RunServer.
     */
    private static String submitProgram(
            ExecutionServiceGrpc.ExecutionServiceStub stub,
            int numeroPedido) throws Exception {

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> requestId =
                new AtomicReference<>();

        AtomicReference<Throwable> error =
                new AtomicReference<>();

        StreamObserver<SubmitResponse> responseObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(SubmitResponse response) {
                        requestId.set(response.getRequestId());
                    }

                    @Override
                    public void onError(Throwable t) {
                        error.set(t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                };

        StreamObserver<ProgramChunk> requestObserver =
                stub.submitProgram(responseObserver);

        // Converto o programa Python em bytes.
        byte[] code =
                PYTHON_CODE.getBytes(StandardCharsets.UTF_8);

        // Envio o programa ao RunServer.
        requestObserver.onNext(
                ProgramChunk.newBuilder()
                        .setProgContent(ByteString.copyFrom(code))
                        .build()
        );

        // Termino o envio dos dados.
        requestObserver.onCompleted();

        // Aguardo pela resposta que contém o UUID.
        if (!latch.await(15, TimeUnit.SECONDS)) {
            throw new IllegalStateException(
                    "Timeout ao submeter pedido " + numeroPedido
            );
        }

        if (error.get() != null) {
            throw new RuntimeException(
                    "Erro na submissao do pedido " + numeroPedido,
                    error.get()
            );
        }

        String id = requestId.get();

        if (id == null || id.isBlank()) {
            throw new IllegalStateException(
                    "RunServer nao devolveu UUID."
            );
        }

        System.out.printf(
                "[Pedido %d] UUID recebido: %s%n",
                numeroPedido,
                id
        );

        return id;
    }


    /**
     * Consulta o estado de um pedido sem pedir stdout ou stderr.
     */
    private static ExecutionStatus obterEstado(
            ExecutionServiceGrpc.ExecutionServiceBlockingStub stub,
            String requestId) {

        ResultRequest request =
                ResultRequest.newBuilder()
                        .setRequestId(requestId)
                        .setStatusOnly(true)
                        .build();

        var iterator = stub.getResult(request);

        if (!iterator.hasNext()) {
            throw new IllegalStateException(
                    "Resposta vazia para o pedido " + requestId
            );
        }

        ResultChunk first = iterator.next();

        // Consumo o resto do stream, caso existam mais blocos.
        while (iterator.hasNext()) {
            iterator.next();
        }

        return first.getStatus();
    }


    /**
     * Mostra o UUID e o estado atual do pedido.
     */
    private static void consultarEstado(
            ExecutionServiceGrpc.ExecutionServiceBlockingStub stub,
            String requestId) {

        ExecutionStatus status =
                obterEstado(stub, requestId);

        System.out.printf(
                "%s | Estado: %s%n",
                requestId,
                status
        );
    }
}
