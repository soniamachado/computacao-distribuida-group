package isel.cd.runserver;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class DockerRunner {

    private static final String IMAGE_NAME = "python:3.12-slim";
    private final DockerClient dockerClient;

    public record ExecutionOutcome(int exitCode, String status) {}

    public DockerRunner() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    public ExecutionOutcome runCode(String requestId, Path requestDir) {
        String containerName = "run-" + requestId;
        String containerId = null;

        try {
            // Volume /work mapeado para a pasta isolada do pedido no host
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(new Bind(requestDir.toAbsolutePath().toString(), new Volume("/work")))
                    .withMemory(256L * 1024L * 1024L)      // Limite 256MB RAM
                    .withMemorySwap(256L * 1024L * 1024L)  // Sem swap adicional
                    .withNetworkMode("none");              // Sem acesso à rede

            // Comando idêntico ao Anexo 2 do enunciado
            String cmd = "timeout 10 python /work/prog.py < /work/input.txt > /work/out.txt 2> /work/err.txt; echo $? > /work/exit.txt";

            CreateContainerResponse container = dockerClient.createContainerCmd(IMAGE_NAME)
                    .withName(containerName)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/work")
                    .withCmd("sh", "-c", cmd)
                    .exec();

            containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            // Aguarda o contentor terminar
            dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitCompletion();

            // Leitura do código de saída gravado em exit.txt
            File exitFile = requestDir.resolve("exit.txt").toFile();
            int exitCode = -1;
            if (exitFile.exists()) {
                String content = Files.readString(exitFile.toPath()).trim();
                exitCode = Integer.parseInt(content);
            }

            // Mapeamento oficial de estados segundo o enunciado:
            // 0 -> CONCLUIDO, 124 -> TIMEOUT, outros -> ERRO
            String status;
            if (exitCode == 0) {
                status = "CONCLUIDO";
            } else if (exitCode == 124) {
                status = "TIMEOUT";
            } else {
                status = "ERRO";
            }

            return new ExecutionOutcome(exitCode, status);

        } catch (Exception e) {
            System.err.printf("[DockerRunner] Erro na execução do contentor para o pedido %s: %s%n", requestId, e.getMessage());
            return new ExecutionOutcome(-1, "ERRO");
        } finally {
            // Destruição forçada do contentor para nunca deixar recursos órfãos
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) {
                }
            }
        }
    }
}