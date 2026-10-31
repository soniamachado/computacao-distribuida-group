
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

/**
 * Esta classe é responsável pela execução dos programas Python
 * dentro de contentores Docker.
 *
 * Cada pedido é executado num contentor isolado, com limites
 * de memória, CPU, tempo e acesso à rede.
 *
 * Depois da execução, o contentor é removido para não deixar
 * recursos desnecessários no servidor.
 */
public class DockerRunner {

    // Imagem Docker utilizada para executar os programas Python.
    private static final String IMAGE = "python";
    private static final String TAG = "3.12-slim";
    private static final String IMAGE_NAME = IMAGE + ":" + TAG;

    // Limite de memória: 256 MB.
    private static final long MEMORY_LIMIT =
            256L * 1024L * 1024L;

    // Limite de CPU: metade de um núcleo.
    private static final long CPU_LIMIT =
            500_000_000L;

    private final DockerClient dockerClient;

    // UID:GID utilizado na execução dos contentores.
    private final String containerUser;

    // Guarda o código de saída e o estado final da execução.
    public record ExecutionOutcome(int exitCode, String status) {}


    /**
     * Inicializo a ligação ao Docker Engine.
     */
    public DockerRunner() {

        DefaultDockerClientConfig config =
                DefaultDockerClientConfig
                        .createDefaultConfigBuilder()
                        .build();

        ApacheDockerHttpClient httpClient =
                new ApacheDockerHttpClient.Builder()
                        .dockerHost(config.getDockerHost())
                        .sslConfig(config.getSSLConfig())
                        .maxConnections(100)
                        .connectionTimeout(Duration.ofSeconds(30))
                        .responseTimeout(Duration.ofSeconds(45))
                        .build();

        this.dockerClient =
                DockerClientImpl.getInstance(config, httpClient);

        // Identifico o utilizador que será utilizado no contentor.
        this.containerUser = currentUser();
    }


    /**
     * Executa o programa Python num contentor Docker.
     *
     * O contentor recebe a pasta do pedido em /work,
     * executa o programa e guarda os resultados em ficheiros.
     */
    public ExecutionOutcome runCode(
            String requestId,
            Path requestDir) {

        String containerName = "run-" + requestId;
        String containerId = null;

        try {
            // Configuro os limites e o isolamento do contentor.
            HostConfig hostConfig = HostConfig.newHostConfig()

                    // Monto a pasta do pedido no diretório /work.
                    .withBinds(new Bind(
                            requestDir.toAbsolutePath().toString(),
                            new Volume("/work")
                    ))

                    // Limito a memória RAM a 256 MB.
                    .withMemory(MEMORY_LIMIT)

                    // Não permito swap adicional.
                    .withMemorySwap(MEMORY_LIMIT)

                    // Desativo o acesso à rede.
                    .withNetworkMode("none")

                    // Limito o contentor a 0,5 CPU.
                    .withNanoCPUs(CPU_LIMIT);

            // Garanto que a imagem Python existe no Docker.
            pullImageIfMissing();

            // Executo o programa com um limite de 10 segundos.
            // O stdout, stderr e código de saída ficam em ficheiros.
            String cmd =
                    "timeout 10 python /work/prog.py " +
                            "< /work/input.txt " +
                            "> /work/out.txt " +
                            "2> /work/err.txt; " +
                            "echo $? > /work/exit.txt";

            // Preparo a criação do contentor.
            CreateContainerCmd create =
                    dockerClient.createContainerCmd(IMAGE_NAME)
                            .withName(containerName)
                            .withHostConfig(hostConfig)
                            .withWorkingDir("/work")
                            .withCmd("sh", "-c", cmd);

            // Executo o programa com o UID/GID configurado,
            // evitando a utilização do utilizador root.
            create.withUser(containerUser);

            // Crio o contentor Docker.
            containerId = create.exec().getId();

            // Inicio a execução do contentor.
            dockerClient.startContainerCmd(containerId).exec();

            // Aguardo até o contentor terminar.
            dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitCompletion();

            // Leio o código de saída guardado pelo contentor.
            int exitCode =
                    readExitCode(requestDir.resolve("exit.txt"));

            // Converto o código de saída para o estado do pedido.
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

            // Se ocorrer uma falha na execução Docker,
            // devolvo um estado de erro.
            System.err.printf(
                    "[DockerRunner] Erro no pedido %s: %s%n",
                    requestId,
                    e.getMessage()
            );

            return new ExecutionOutcome(-1, "ERRO");

        } finally {

            // Removo sempre o contentor, mesmo que tenha ocorrido um erro.
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId)
                            .withForce(true)
                            .exec();

                } catch (Exception e) {
                    System.err.println(
                            "[DockerRunner] Erro ao remover contentor: "
                                    + e.getMessage()
                    );
                }
            }
        }
    }


    /**
     * Verifico se a imagem Python já está disponível.
     * Se não existir, descarrego-a do Docker Hub.
     */
    private void pullImageIfMissing()
            throws InterruptedException {

        try {
            dockerClient.inspectImageCmd(IMAGE_NAME).exec();

        } catch (NotFoundException e) {

            System.out.printf(
                    "[DockerRunner] A obter a imagem %s...%n",
                    IMAGE_NAME
            );

            dockerClient.pullImageCmd(IMAGE)
                    .withTag(TAG)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
        }
    }


    /**
     * Lê o código de saída produzido pela execução do Python.
     */
    private static int readExitCode(Path exitFile)
            throws IOException {

        if (!Files.exists(exitFile)) {
            return -1;
        }

        String content = Files.readString(exitFile).strip();

        return content.isEmpty()
                ? -1
                : Integer.parseInt(content);
    }


    /**
     * Identifico o UID e o GID que serão utilizados no contentor.
     *
     * No Windows, utilizo 1000:1000 para testar sem root.
     * No Linux, utilizo o UID/GID real do processo Java.
     *
     * Se não conseguir obter um utilizador não-root,
     * interrompo a inicialização para evitar executar como root.
     */
    private static String currentUser() {

        String os = System.getProperty("os.name", "");

        if (os.toLowerCase().startsWith("win")) {
            return "1000:1000";
        }

        try {
            String uid = readCommand("id", "-u");
            String gid = readCommand("id", "-g");

            if (uid.isBlank() || gid.isBlank()) {
                throw new IllegalStateException(
                        "Não foi possível obter o UID/GID."
                );
            }

            // Não permito executar os programas como root.
            if ("0".equals(uid)) {
                throw new IllegalStateException(
                        "O RunServer não deve executar como root."
                );
            }

            return uid + ":" + gid;

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IllegalStateException(
                    "Falha ao identificar o utilizador do RunServer.", e
            );
        }
    }


    /**
     * Executa um comando do sistema operativo e devolve a sua saída.
     */
    private static String readCommand(String... command)
            throws IOException, InterruptedException {

        Process process = new ProcessBuilder(command).start();

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        ).strip();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException(
                    "O comando terminou com código " + exitCode
            );
        }

        return output;
    }
}
