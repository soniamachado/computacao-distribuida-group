package isel.cd.runserver;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class DockerRunner {

    private static final String IMAGE = "python";
    private static final String TAG = "3.12-slim";
    private static final String IMAGE_NAME = IMAGE + ":" + TAG;
    private final DockerClient dockerClient;
    private final String containerUser;

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
        this.containerUser = currentUser();
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

            pullImageIfMissing();

            // Comando idêntico ao Anexo 2 do enunciado
            String cmd = "timeout 10 python /work/prog.py < /work/input.txt > /work/out.txt 2> /work/err.txt; echo $? > /work/exit.txt";

            CreateContainerCmd create = dockerClient.createContainerCmd(IMAGE_NAME)
                    .withName(containerName)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/work")
                    .withCmd("sh", "-c", cmd);
            // uid:gid do processo Java, para os ficheiros em /work ficarem legíveis no host
            if (containerUser != null) {
                create.withUser(containerUser);
            }

            containerId = create.exec().getId();
            dockerClient.startContainerCmd(containerId).exec();

            // Aguarda o contentor terminar
            dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitCompletion();

            int exitCode = readExitCode(requestDir.resolve("exit.txt"));

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

    private void pullImageIfMissing() throws InterruptedException {
        try {
            dockerClient.inspectImageCmd(IMAGE_NAME).exec();
        } catch (NotFoundException e) {
            System.out.printf("[DockerRunner] A obter a imagem %s...%n", IMAGE_NAME);
            dockerClient.pullImageCmd(IMAGE)
                    .withTag(TAG)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
        }
    }

    private static int readExitCode(Path exitFile) throws IOException {
        if (!Files.exists(exitFile)) {
            return -1;
        }
        String content = Files.readString(exitFile).strip();
        return content.isEmpty() ? -1 : Integer.parseInt(content);
    }

    /** uid:gid do utilizador atual em Linux/macOS. Em Windows o contentor corre com o utilizador por omissão. */
    private static String currentUser() {
        String os = System.getProperty("os.name", "");
        if (os.toLowerCase().startsWith("win")) {
            return null;
        }
        try {
            String uid = readCommand("id", "-u");
            String gid = readCommand("id", "-g");
            if (uid.isEmpty() || gid.isEmpty()) {
                return null;
            }
            return uid + ":" + gid;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readCommand(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        process.waitFor();
        return output;
    }
}