package isel.cd.runserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import isel.cd.coderunner.manager.ManagerServiceGrpc;
import isel.cd.coderunner.manager.RegisterRequest;
import isel.cd.coderunner.manager.RegisterResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RunServer {

    private static final String DEFAULT_MY_HOST = "localhost";
    //mudei o porto para 9002 porque o 9001 ja estava a ser usado em outro processe
    private static final int DEFAULT_MY_PORT = 9002;
    private static final String DEFAULT_MANAGER_HOST = "localhost";
    private static final int DEFAULT_MANAGER_PORT =  8080;

    public static void main(String[] args) throws IOException, InterruptedException {
        int myPort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_MY_PORT;
        String managerHost = args.length > 1 ? args[1] : DEFAULT_MANAGER_HOST;
        int managerPort = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_MANAGER_PORT;
        String myHost = args.length > 3 ? args[3] : advertisedHost(managerHost, managerPort);
        System.out.printf("[RunServer] A contactar Manager em %s:%d...%n", managerHost, managerPort);
        System.out.printf("[RunServer] A registar-se como %s:%d%n", myHost, myPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(managerHost, managerPort)
                .usePlaintext()
                .build();

        ManagerServiceGrpc.ManagerServiceBlockingStub managerStub = ManagerServiceGrpc.newBlockingStub(channel);

        // Registo perante o ManagerServer
        RegisterResponse regResponse = managerStub.registerRunServer(RegisterRequest.newBuilder()
                .setServerHost(myHost)
                .setServerPort(myPort)
                .build());

        String redisHost = regResponse.getRedisHost();
        int redisPort = regResponse.getRedisPort();
        if (isLoopback(redisHost) && !isLoopback(managerHost)) {
            redisHost = managerHost;
        }
        System.out.printf("[RunServer] Registado com sucesso. Redis em %s:%d%n", redisHost, redisPort);

        channel.shutdown();

        // Inicia o serviço gRPC de execução
        Server server = ServerBuilder.forPort(myPort)
                .addService(new ExecutionServiceImpl(myHost, myPort, redisHost, redisPort))
                .build();

        server.start();
        System.out.printf("[RunServer] Pronto a receber pedidos no porto %d.%n", myPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[RunServer] A encerrar...");
            server.shutdown();
        }));

        server.awaitTermination();
    }

    private static String advertisedHost(String managerHost, int managerPort) {
        if (isLoopback(managerHost)) {
            return DEFAULT_MY_HOST;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(managerHost, managerPort), 3000);
            String address = socket.getLocalAddress().getHostAddress();
            if (address == null || address.isBlank() || isLoopback(address)) {
                return DEFAULT_MY_HOST;
            }
            return address;
        } catch (IOException e) {
            System.err.println("[RunServer] Não foi possível descobrir o IP desta VM; a usar localhost.");
            return DEFAULT_MY_HOST;
        }
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }
}