package isel.cd.manager;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import isel.cd.coderunner.manager.GetRunServerRequest;
import isel.cd.coderunner.manager.GetRunServerResponse;
import isel.cd.coderunner.manager.ManagerServiceGrpc;
import isel.cd.coderunner.manager.RegisterRequest;
import isel.cd.coderunner.manager.RegisterResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ManagerServiceImpl extends ManagerServiceGrpc.ManagerServiceImplBase {

    private final String redisHost;
    private final int redisPort;

    // Registo thread-safe dos servidores ativos
    private final List<RunServerInfo> servers = new ArrayList<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public record RunServerInfo(String host, int port) {}

    public ManagerServiceImpl(String redisHost, int redisPort) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
    }

    @Override
    public synchronized void registerRunServer(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        RunServerInfo serverInfo = new RunServerInfo(request.getServerHost(), request.getServerPort());

        // Evita registos duplicados caso o mesmo RunServer reinicie ou re-registe
        servers.removeIf(s -> s.host().equals(serverInfo.host()) && s.port() == serverInfo.port());
        servers.add(serverInfo);

        System.out.printf("[Manager] RunServer registado: %s:%d (Total ativos: %d)%n",
                serverInfo.host(), serverInfo.port(), servers.size());

        RegisterResponse response = RegisterResponse.newBuilder()
                .setRedisHost(redisHost)
                .setRedisPort(redisPort)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void getRunServer(GetRunServerRequest request, StreamObserver<GetRunServerResponse> responseObserver) {
        if (servers.isEmpty()) {
            responseObserver.onError(Status.UNAVAILABLE
                    .withDescription("Não existem instâncias de RunServer registadas no sistema.")
                    .asRuntimeException());
            return;
        }

        // Distribuição equitativa de carga (Round-Robin)
        int idx = Math.abs(roundRobinIndex.getAndIncrement() % servers.size());
        RunServerInfo selected = servers.get(idx);

        System.out.printf("[Manager] Cliente atribuído ao RunServer: %s:%d%n", selected.host(), selected.port());

        GetRunServerResponse response = GetRunServerResponse.newBuilder()
                .setServerHost(selected.host())
                .setServerPort(selected.port())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}