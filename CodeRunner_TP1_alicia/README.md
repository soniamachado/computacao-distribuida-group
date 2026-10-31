# CodeRunner — Plataforma Distribuída de Execução Isolada de Código

**Trabalho Prático de Avaliação 1 (TPA1)**  
**Unidade Curricular:** Computação Distribuída (CD)  
**Semestre:** Verão 2025/2026  
**Instituição:** Instituto Superior de Engenharia de Lisboa (ISEL)  
**Mestrado em Engenharia Informática e de Computadores / Multimédia**

---

##  Identificação do Grupo

| Número | Nome do Estudante | Contacto / Email |
| :---: | :--- | :--- |
| **AXXXXX** | Emanuel [Apelido] | aluno@alunos.isel.pt |
| **AXXXXX** | [Nome do Colega] | aluno2@alunos.isel.pt |

---

##  1. Visão Geral e Arquitetura do Sistema

O **CodeRunner** é um sistema distribuído para execução remota e concorrente de programas Python num ambiente seguro, isolado e controlado por recursos, utilizando contentores Docker efémeros.

O sistema assenta numa arquitetura desacoplada em 4 componentes principais:

```text
               +-------------------+
               |   ManagerServer   |
               | (Porto 8080 gRPC) |
               +---------+---------+
                         |
           (Registo /    |   (GetRunServer - Round Robin)
          Localização)   |
                         v
                  +-------------+
       +--------->|  ClientApp  |
       |          +------+------+
       |                 | (Execute / GetOutput)
       |                 v
+------+------+   +-------------+       +-------------------+
|  RunServer  |<--+  RunServer  | <---> |   Redis Storage   |
| (Porto 9002)|   | (Porto 9003)|       | (Porto 6000 Hash) |
+------+------+   +------+------+       +-------------------+
       |                 |
       v                 v
[ Docker Container ] [ Docker Container ]
 (python:3.12-slim)   (python:3.12-slim)
```

### Componentes:
1. **ManagerServer (Coordenador)**:
    - Mantém o catálogo em memória dos nós `RunServer` ativos registados via gRPC (`RegisterRunServer`).
    - Fornece aos nós executores a localização do cluster/instância de Redis partilhado.
    - Aplica balanceamento de carga em anel (**Round-Robin**) atómico a cada pedido de `GetRunServer` efetuado por clientes.
2. **RunServer (Nós Executores)**:
    - Recebem o código Python e o *stdin* opcional por streaming gRPC (`Execute`).
    - Persistem o estado do pedido no Redis central (`PENDENTE`).
    - Criam uma diretoria de trabalho efémera e instanciam um contentor Docker isolado para executar o script.
    - Atualizam o estado para `CONCLUIDO`, `TIMEOUT` (código `124`) ou `ERRO` na base de dados partilhada.
    - Disponibilizam o procedimento gRPC `GetOutput` para devolver os relatórios de execução (STDOUT e STDERR).
3. **ClientApp (Linha de Comandos)**:
    - Interface interativa de submissão e consulta.
    - Permite submeter scripts `.py` e ficheiros opcionais de *input*.
    - Permite consultar resultados em qualquer momento através do UUID do pedido, mesmo que contacte um nó diferente do executor original.
4. **REDIS (Base de Dados Partilhada)**:
    - Armazena metadados de cada pedido sob a chave hash `pedido:<UUID>`:
        - `estado`: `PENDENTE`, `CONCLUIDO`, `TIMEOUT`, `ERRO`
        - `exitCode`: Código de saída do processo (ex.: `0`, `1`, `124`)
        - `serverHost` e `serverPort`: Endereço do nó que executou o pedido
        - `timestamp`: Registo temporal de início/conclusão

---

##  2. Mecanismos de Isolamento, Segurança e Limpeza

Conforme estipulado nos requisitos não funcionais do guião:
* **Isolamento de Rede**: Os contentores são executados com `--network none` / `withNetworkMode("none")`, impedindo qualquer comunicação externa ou acesso à rede local a partir do código do utilizador.
* **Limitação de Recursos e Tempo**: Cada execução é balizada pelo utilitário `timeout 10` (máximo de 10 segundos). Casos de ciclos infinitos são terminados com o código `124` e refletidos com o estado `TIMEOUT`.
* **Ciclo de Vida Efémero (Sem Órfãos)**:
    - O contentor é forçadamente destruído e limpo logo após a terminação (`client.removeContainerCmd(containerId).withForce(true).exec()`).
    - As diretorias temporárias criadas em disco com os ficheiros de script (`prog.py`, `in.txt`, `out.txt`, `err.txt`) são integralmente apagadas após a recolha dos outputs.

---

## ⚙ 3. Pré-requisitos de Instalação e Execução

* **Java Development Kit (JDK)**: Versão 21 ou superior instalada.
* **Docker Engine / Docker Desktop**: Ativo e com suporte a contentores Linux.
* **Imagem Docker do Python**:
  ```bash
  docker pull python:3.12-slim
  ```
* **Mapeamento de Portos por Omissão**:
    - Redis: `localhost:6000` (mapeado para a porta standard `6379`)
    - ManagerServer: `localhost:8080`
    - RunServer Instância 1: `localhost:9002`
    - RunServer Instância 2: `localhost:9003`

---

##  4. Guia de Inicialização do Sistema (Passo a Passo)

### Passo 1: Iniciar o contentor REDIS
Caso já tenha o contentor criado:
```bash
docker start my-redis
```
*(Caso necessite de criar de raiz)*:
```bash
docker run -d --name my-redis -p 6000:6379 redis:alpine
```

### Passo 2: Iniciar o ManagerServer
Na raiz do projeto (`CodeRunner`), execute num terminal dedicado:
```powershell
./gradlew :manager:run
```
*Saída esperada:* `[ManagerServer] Em escuta no porto 8080...`

### Passo 3: Iniciar os RunServers (Múltiplos Nós Paralelos)

* **Nó 1 (Porto 9002 por omissão):**
  Num segundo terminal:
  ```powershell
  ./gradlew :runserver:run
  ```
  *(Ou passando explicitamente: `./gradlew :runserver:run --args="9002"`)*

* **Nó 2 (Porto 9003 dinâmico):**
  Num terceiro terminal:
  ```powershell
  ./gradlew :runserver:run --args="9003"
  ```
  *Saída esperada em ambos:* `[RunServer] Registado com sucesso (...) Pronto a receber pedidos no porto XXXX.`

### Passo 4: Iniciar o Cliente CLI
Num quarto terminal:
```powershell
./gradlew :client:run
```

---

##  5. Validação das Funcionalidades e Demonstração

### Teste A: Balanceamento de Carga Round-Robin
1. No menu do `ClientApp`, selecione a opção `1` (Submeter programa) e submeta `teste.py`.
    - O cliente reporta: `[Client] RunServer atribuído: localhost:9002`
2. Submeta imediatamente a seguir outro pedido (opção `1`).
    - O cliente reporta: `[Client] RunServer atribuído: localhost:9003`
3. A alternância consecutiva comprova o escalonamento equitativo pelo gestor.

### Teste B: Consulta Desacoplada e Cruzada de Resultados
1. Copie o UUID do pedido processado no nó `9003`.
2. No menu do cliente, escolha a opção `2` (Consultar resultado de pedido) e insira o UUID.
3. O cliente consulta o Redis, identifica que o output reside no `localhost:9003`, estabelece a ligação gRPC pontual a esse nó e apresenta o relatório completo (`CONCLUIDO`, código `0`, STDOUT).
4. O mesmo procedimento funciona para pedidos processados no nó `9002`.

### Teste C: Tratamento de Timeout
1. Submeta um script contendo `import time; time.sleep(15)`.
2. Ao fim de 10 segundos, consulte o estado:
    - O estado devolvido é `TIMEOUT`.
    - O código de saída retornado pelo sistema é `124`.

---

##  6. Estrutura do Repositório e Ficheiros

```text
CodeRunner/
├── client/                     # Módulo do Cliente (CLI gRPC)
│   └── src/main/java/isel/cd/client/ClientApp.java
├── manager/                    # Módulo do ManagerServer (Coordenação e Balancer)
│   └── src/main/java/isel/cd/manager/
│       ├── ManagerServer.java
│       └── ManagerServiceImpl.java
├── runserver/                  # Módulo do RunServer (Execução e Docker)
│   └── src/main/java/isel/cd/runserver/
│       ├── RunServer.java
│       ├── ExecutionServiceImpl.java
│       └── DockerRunner.java
├── contract/                   # Definições dos Protocol Buffers
│   └── src/main/proto/
│       ├── manager.proto
│       └── execution.proto
├── build.gradle.kts            # Configuração modular Gradle
├── settings.gradle.kts
└── README.md                   # Documentação do projeto
```

---

##  7. Limpeza e Empacotamento para Submissão

Para gerar o ficheiro ZIP final em conformidade com as regras de submissão do Moodle (excluindo binários e pastas de build):

1. Executar a limpeza das diretorias de compilação:
   ```powershell
   ./gradlew clean
   ```
2. Garantir que as diretorias `.gradle/`, `.idea/` e `build/` não são adicionadas ao arquivo comprimido.
3. Compactar a raiz do projeto para o arquivo de entrega final (`CodeRunner-GrupoXX.zip`).