# POC - Arquitetura Orientada a Eventos com Apache Kafka

Projeto de estudo: dois microsserviços (Java 21 / Spring Boot, Arquitetura
Hexagonal / Ports & Adapters) comunicando-se de forma assíncrona via Apache
Kafka.

## Visão geral da arquitetura

```
Cliente --POST /api/orders--> order-service --publica--> [Kafka: orders.created (3 partições)]
                                                                    |
                                                            consome (consumer group)
                                                                    v
                                                          notification-service --> notificação (log)
```

- **order-service** (Producer): recebe o pedido via REST, valida, cria o
  domínio `Order` e publica um evento `OrderCreatedEvent` no tópico
  `orders.created`. Key da mensagem = `orderId` (garante ordenação por
  pedido dentro da mesma partição).
- **Apache Kafka**: tópico `orders.created`, 3 partições, log particionado e
  replicado (replication factor 1, cluster single-node).
- **notification-service** (Consumer): consome o evento de forma
  assíncrona e dispara uma notificação (stub: log).

## Pré-requisitos

- Java 21
- Maven 3.9+
- Docker Desktop (com Docker Compose v2+)

## Estrutura do projeto

```
kafka/
├── .gitignore               # Ignora target/, metadados de IDE, configs locais (Eclipse/IntelliJ/VSCode/Claude)
├── docker-compose.yml      # Infraestrutura Kafka (Etapa 1)
├── order-service/          # Producer (Etapa 2)
├── notification-service/   # Consumer (Etapa 3 - ainda não criado)
├── PROMPTS-POC.txt          # Roteiro de prompts para evoluir a POC por etapas
└── README.md                # Este arquivo
```

## Como subir a infraestrutura

```powershell
cd C:\workspace_eclipse\kafka
docker compose up -d
docker compose ps
```

Para derrubar tudo (mantendo os dados do Kafka no volume):
```powershell
docker compose down
```

Para derrubar tudo e limpar os dados (reset completo):
```powershell
docker compose down -v
```

---

## Diário de evolução da POC

### Etapa 1 - Infraestrutura Kafka (docker-compose) ✅

**O que foi criado**: `docker-compose.yml` com dois serviços:

- **kafka** (`apache/kafka:3.8.0`): broker em modo **KRaft** (sem
  Zookeeper) — único processo acumula os papéis `broker` e `controller`
  (cluster single-node). Decisões de configuração:
  - **CLUSTER_ID fixo** (`MkU3OEVBNTcwNTJENDM2Qk`): evita erro de
    "cluster ID mismatch" ao reiniciar o container com o volume de dados
    já formatado.
  - **Listeners duplos**: `PLAINTEXT` (interno, `kafka:9092`, usado pela
    rede docker — útil quando os serviços Spring forem dockerizados no
    futuro) e `PLAINTEXT_HOST` (externo, `localhost:29092`, usado pelos
    apps Spring Boot rodando localmente fora do Docker). `CONTROLLER`
    (`9093`) é interno ao KRaft.
  - **`auto.create.topics.enable=false`**: tópicos só são criados
    explicitamente (o tópico `orders.created` com 3 partições será criado
    via bean `NewTopic` no order-service, na Etapa 2) — evita tópicos
    "acidentais" com configuração default.
  - **Volume nomeado `kafka-data`**: persiste os dados do broker entre
    restarts do container.
  - **Healthcheck**: `kafka-broker-api-versions.sh` contra
    `localhost:9092`.

- **kafka-ui** (`provectuslabs/kafka-ui`): interface web em
  `http://localhost:8081` para inspecionar tópicos, partições, mensagens e
  consumer groups durante o desenvolvimento. Aponta para `kafka:9092` (rede
  interna do compose). Só inicia após o `kafka` estar `healthy`.

**Validação realizada**:

| Verificação | Resultado |
|---|---|
| `docker compose up -d` | OK, sem erros |
| `docker compose ps` | `kafka` = `healthy`, `kafka-ui` = `running` |
| `kafka-broker-api-versions.sh --bootstrap-server localhost:9092` (dentro do container) | Respondeu com a lista de APIs suportadas (broker `id: 1` ativo) |
| `http://localhost:8081` | HTTP 200 |
| `http://localhost:8081/api/clusters` | Cluster `poc-kafka` com `status: online`, `brokerCount: 1`, `topicCount: 0` (esperado — nenhum tópico criado ainda) |

Próximo passo: **Etapa 2** — order-service publica eventos reais no
tópico `orders.created` (criado automaticamente via `NewTopic` bean, 3
partições).

### Organização - .gitignore na raiz ✅

Criado `.gitignore` na raiz de `kafka/` (repositório git ainda será
inicializado na Etapa 6, mas o arquivo já fica pronto para cobrir todos os
módulos): ignora `target/` (build Maven), metadados de IDE
(`.classpath`, `.project`, `.settings/` do Eclipse; `.idea/` do
IntelliJ; `.vscode/`), `.claude/settings.local.json` (config local por
máquina/usuário) e arquivos de SO/log.
