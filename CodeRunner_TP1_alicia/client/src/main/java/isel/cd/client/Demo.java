
package isel.cd.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.NameResolverRegistry;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;

import isel.cd.coderunner.execution.*;
import isel.cd.coderunner.manager.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Demo {

    private record Server(String host, int port) {}

    private record Job(
            String name,
            String code,
            ExecutionStatus expected) {}

    private record Result(
            ExecutionStatus status,
            String owner,
            String stdout,
            String stderr) {}

    private static final List<Job> JOBS = List.of(

            new Job(
                    "normal-1",
                    "print('Ola do programa 1')\n",
                    ExecutionStatus.CONCLUIDO
            ),

            new Job(
                    "normal-2",
                    "print(sum(range(100)))\n",
                    ExecutionStatus.CONCLUIDO
            ),

            new Job(
                    "normal-3",
                    "print('CodeRunner OK')\n",
                    ExecutionStatus.CONCLUIDO
            ),

            new Job(
                    "erro-1",
                    "raise ValueError('erro de teste 1')\n",
                    ExecutionStatus.ERRO
            ),

            new Job(
                    "erro-2",
                    "print(10 / 0)\n",
                    ExecutionStatus.ERRO
            ),

            new Job(
                    "erro-3",
                    "import modulo_inexistente\n",
                    ExecutionStatus.ERRO
            ),

            new Job(
                    "timeout-1",
                    "while True:\n    pass\n",
                    ExecutionStatus.TIMEOUT
            ),

            new Job(
                    "timeout-2",
                    "import time\nwhile True:\n    time.sleep(1)\n",
                    ExecutionStatus.TIMEOUT
            ),

            new Job(
                    "timeout-3",
                    "import time\ntime.sleep(60)\n",
                    ExecutionStatus.TIMEOUT
            )
    );

    public static void main(String[] args) throws Exception {

        // Corrige a descoberta dos componentes gRPC
        // quando a aplicação é executada num JAR único.
        NameResolverRegistry.getDefaultRegistry()
                .register(new DnsNameResolverProvider());

        LoadBalancerRegistry.getDefaultRegistry()
                .register(new PickFirstLoadBalancerProvider());

        String managerHost =
                args.length > 0 ? args[0] : "localhost";

        int managerPort =
                args.length > 1
                        ? Integer.parseInt(args[1])
                        : 8080;

        System.out.println("=== DEMO CODERUNNER ===");

        System.out.printf(
                "Manager: %s:%d%n",
                managerHost,
                managerPort
        );

        System.out.println(
                "9 pedidos concorrentes: " +
                        "3 normais, 3 erros, 3 timeouts"
        );

        ExecutorService pool =
                Executors.newFixedThreadPool(9);

        List<Future<Boolean>> futures = new ArrayList<>();

        try {

            for (Job job : JOBS) {

                futures.add(
                        pool.submit(() ->
                                executeJob(
                                        managerHost,
                                        managerPort,
                                        job
                                )
                        )
                );
            }

            int passed = 0;

            for (Future<Boolean> future : futures) {

                try {

                    if (future.get(60, TimeUnit.SECONDS)) {
                        passed++;
                    }

                } catch (Exception e) {

                    System.err.println(
                            "Falha na tarefa: " + e.getMessage()
                    );
                }
            }

            System.out.printf(
                    "%n=== RESULTADO: %d/%d CORRETOS ===%n",
                    passed,
                    JOBS.size()
            );

        } finally {
            pool.shutdownNow();
        }
    }

    private static boolean executeJob(
            String managerHost,
            int managerPort,
            Job job) {

        try {

            Server server =
                    chooseServer(managerHost, managerPort);

            String id = submit(server, job);

            System.out.printf(
                    "[%s] UUID=%s | RunServer=%s:%d%n",
                    job.name(),
                    id,
                    server.host(),
                    server.port()
            );

            Result status;

            long deadline =
                    System.nanoTime()
                            + TimeUnit.SECONDS.toNanos(45);

            do {

                Thread.sleep(500);

                status = getResult(server, id, true);

                if (status.status()
                        != ExecutionStatus.PENDENTE) {
                    break;
                }

            } while (System.nanoTime() < deadline);

            if (status.status() == ExecutionStatus.PENDENTE) {

                System.err.printf(
                        "[%s] Continua PENDENTE%n",
                        job.name()
                );

                return false;
            }

            // Os ficheiros de saída estão no RunServer executor.
            Server owner =
                    parseServer(status.owner(), server);

            Result full =
                    getResult(owner, id, false);

            boolean ok =
                    full.status() == job.expected();

            synchronized (System.out) {

                System.out.printf(
                        "%n--- %s ---%n",
                        job.name()
                );

                System.out.println("UUID: " + id);

                System.out.println(
                        "Executor: " + status.owner()
                );

                System.out.println(
                        "Estado: " + full.status()
                                + " | Esperado: " + job.expected()
                );

                if (!full.stdout().isBlank()) {

                    System.out.println(
                            "STDOUT: " + full.stdout().trim()
                    );
                }

                if (!full.stderr().isBlank()) {

                    System.out.println(
                            "STDERR: " + full.stderr().trim()
                    );
                }

                System.out.println(
                        ok ? "PASSOU" : "FALHOU"
                );
            }

            return ok;

        } catch (Exception e) {

            System.err.printf(
                    "[%s] ERRO: %s%n",
                    job.name(),
                    e.getMessage()
            );

            return false;
        }
    }

    private static Server chooseServer(
            String host,
            int port) {

        ManagedChannel channel =
                channel(host, port);

        try {

            GetRunServerResponse response =
                    ManagerServiceGrpc
                            .newBlockingStub(channel)
                            .withDeadlineAfter(
                                    10,
                                    TimeUnit.SECONDS
                            )
                            .getRunServer(
                                    GetRunServerRequest
                                            .getDefaultInstance()
                            );

            return new Server(
                    response.getServerHost(),
                    response.getServerPort()
            );

        } finally {
            channel.shutdownNow();
        }
    }

    private static String submit(
            Server server,
            Job job) throws Exception {

        ManagedChannel channel =
                channel(server.host(), server.port());

        CountDownLatch latch =
                new CountDownLatch(1);

        AtomicReference<String> id =
                new AtomicReference<>();

        AtomicReference<Throwable> error =
                new AtomicReference<>();

        try {

            ExecutionServiceGrpc.ExecutionServiceStub stub =
                    ExecutionServiceGrpc
                            .newStub(channel)
                            .withDeadlineAfter(
                                    20,
                                    TimeUnit.SECONDS
                            );

            StreamObserver<SubmitResponse> responseObserver =
                    new StreamObserver<>() {

                        @Override
                        public void onNext(
                                SubmitResponse response) {

                            id.set(response.getRequestId());
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

            StreamObserver<ProgramChunk> stream =
                    stub.submitProgram(responseObserver);

            stream.onNext(
                    ProgramChunk.newBuilder()
                            .setMetadata(
                                    SubmitMetadata.newBuilder()
                                            .setFilename(
                                                    job.name() + ".py"
                                            )
                                            .setHasInput(false)
                                            .build()
                            )
                            .build()
            );

            byte[] bytes =
                    job.code().getBytes(
                            StandardCharsets.UTF_8
                    );

            // Envio do programa Python por streaming.
            for (int offset = 0;
                 offset < bytes.length;
                 offset += 4096) {

                int size =
                        Math.min(
                                4096,
                                bytes.length - offset
                        );

                stream.onNext(
                        ProgramChunk.newBuilder()
                                .setProgContent(
                                        ByteString.copyFrom(
                                                bytes,
                                                offset,
                                                size
                                        )
                                )
                                .build()
                );
            }

            stream.onCompleted();

            if (!latch.await(
                    20,
                    TimeUnit.SECONDS)) {

                throw new TimeoutException(
                        "Sem resposta à submissão"
                );
            }

            if (error.get() != null) {
                throw new RuntimeException(error.get());
            }

            if (id.get() == null || id.get().isBlank()) {

                throw new IllegalStateException(
                        "UUID não recebido"
                );
            }

            return id.get();

        } finally {
            channel.shutdownNow();
        }
    }

    private static Result getResult(
            Server server,
            String id,
            boolean statusOnly) {

        ManagedChannel channel =
                channel(server.host(), server.port());

        try {

            var stub =
                    ExecutionServiceGrpc
                            .newBlockingStub(channel)
                            .withDeadlineAfter(
                                    15,
                                    TimeUnit.SECONDS
                            );

            var iterator =
                    stub.getResult(
                            ResultRequest.newBuilder()
                                    .setRequestId(id)
                                    .setStatusOnly(statusOnly)
                                    .build()
                    );

            ExecutionStatus status =
                    ExecutionStatus.UNKNOWN;

            String owner = "";

            StringBuilder stdout =
                    new StringBuilder();

            StringBuilder stderr =
                    new StringBuilder();

            while (iterator.hasNext()) {

                ResultChunk chunk =
                        iterator.next();

                status = chunk.getStatus();

                owner = chunk.getExecutionServer();

                if (chunk.hasStdoutChunk()) {

                    stdout.append(
                            chunk.getStdoutChunk()
                                    .toString(
                                            StandardCharsets.UTF_8
                                    )
                    );
                }

                if (chunk.hasStderrChunk()) {

                    stderr.append(
                            chunk.getStderrChunk()
                                    .toString(
                                            StandardCharsets.UTF_8
                                    )
                    );
                }
            }

            return new Result(
                    status,
                    owner,
                    stdout.toString(),
                    stderr.toString()
            );

        } finally {
            channel.shutdownNow();
        }
    }

    private static Server parseServer(
            String owner,
            Server fallback) {

        if (owner == null || owner.isBlank()) {
            return fallback;
        }

        int separator =
                owner.lastIndexOf(':');

        if (separator < 0) {
            return fallback;
        }

        try {

            return new Server(
                    owner.substring(0, separator),
                    Integer.parseInt(
                            owner.substring(separator + 1)
                    )
            );

        } catch (NumberFormatException e) {

            return fallback;
        }
    }

    // Corrige o NameResolver no JAR executável.
    private static ManagedChannel channel(
            String host,
            int port) {

        return NettyChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .nameResolverFactory(
                        new DnsNameResolverProvider()
                )
                .defaultLoadBalancingPolicy("pick_first")
                .build();
    }
}
