package isel.cd.runserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import isel.cd.coderunner.manager.ManagerServiceGrpc;
import isel.cd.coderunner.manager.RegisterRequest;
import isel.cd.coderunner.manager.RegisterResponse;

import java.io.IOException;

public class RunServer {

    private static final String DEFAULT_MY_HOST = "localhost";
    //mudei o porto para 9002 porque o 9001 ja estava a ser usado em outro processe
    private static final int DEFAULT_MY_PORT = 9002;
    private static final String DEFAULT_MANAGER_HOST = "localhost";
    private static final int DEFAULT_MANAGER_PORT =  8080;

    public static void main(String[] args) throws IOException, InterruptedException {
        String myHost = DEFAULT_MY_HOST;
        int myPort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_MY_PORT;
        String managerHost = args.length > 1 ? args[1] : DEFAULT_MANAGER_HOST;
        int managerPort = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_MANAGER_PORT;
        System.out.printf("[RunServer] A contactar Manager em %s:%d...%n", managerHost, managerPort);

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
        System.out.printf("[RunServer] Registado com sucesso. Endereço Redis recebido: %s:%d%n", redisHost, redisPort);

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
}