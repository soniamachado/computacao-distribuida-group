package isel.cd.manager;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class ManagerServer {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_REDIS_HOST = "localhost";
    private static final int DEFAULT_REDIS_PORT = 6000;

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        String redisHost = args.length > 1 ? args[1] : DEFAULT_REDIS_HOST;
        int redisPort = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_REDIS_PORT;

        Server server = ServerBuilder.forPort(port)
                .addService(new ManagerServiceImpl(redisHost, redisPort))
                .build();

        server.start();
        System.out.printf("[ManagerServer] Em execução no porto %d... (Redis configurado para %s:%d)%n",
                port, redisHost, redisPort);

        // Terminação graciosa com CTRL+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[ManagerServer] A encerrar...");
            server.shutdown();
        }));

        server.awaitTermination();
    }
}