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
│   └── src/main/java/com/poc/orderservice/
│       ├── OrderServiceApplication.java
│       ├── application/              # "core" hexagonal (domínio + casos de uso)
│       │   ├── domain/                   # Order, OrderItem, OrderCreatedEvent
│       │   ├── port/
│       │   │   ├── in/                   # CreateOrderCommand, CreateOrderUseCase
│       │   │   └── out/                  # OrderEventPublisher
│       │   └── service/                  # CreateOrderService (@UseCase)
│       ├── adapters/
│       │   ├── in/web/                   # OrderController, DTOs (REST)
│       │   └── out/messaging/            # KafkaOrderEventPublisher
│       ├── config/                       # KafkaTopicConfig, KafkaProducerConfig
│       └── util/                         # UseCase (meta-anotação @UseCase = @Component)
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

## Problemas conhecidos / Troubleshooting

### Containers do Kafka saem sozinhos (`Exited (143)` / `Exited (137)`)

Em ambiente Docker Desktop no Windows, os containers `kafka` e `kafka-ui` já
saíram sozinhos algumas vezes durante o desenvolvimento desta POC (motivo
provável: reinício/instabilidade da VM do Docker Desktop, não algo da
aplicação). Sintoma: `docker compose ps` retorna vazio, e o order-service
não consegue abrir conexão com `localhost:29092` (`AdminClient ... Connection
to node -1 ... could not be established`).

Como os dados ficam no volume nomeado `kafka-data`, basta religar:
```powershell
cd C:\workspace_eclipse\kafka
docker compose up -d
docker compose ps   # confirmar kafka = healthy antes de iniciar os apps
```

### "Port 8080 was already in use" ao reiniciar o order-service

Se uma tentativa anterior de `mvn spring-boot:run` falhou (ex.: por causa do
problema acima) mas o processo Java não foi encerrado, ele continua
ocupando a porta 8080 — a próxima tentativa falha com "Web server failed to
start. Port 8080 was already in use", mesmo que o motivo original (Kafka
fora do ar) já tenha sido resolvido.

Identificar e encerrar o processo:
```powershell
netstat -ano | findstr ":8080"
Stop-Process -Id <PID> -Force
```

> Lição desta etapa: os dois erros podem aparecer juntos no mesmo log e
> parecer um único problema de conectividade com o Kafka, mas são duas
> causas independentes — sempre confirmar `docker compose ps` (Kafka
> `healthy`) **e** que a porta 8080 está livre antes de subir o
> order-service novamente.

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

### Etapa 2 - order-service: producer Kafka real ✅

**Arquivos novos/alterados** (substitui o `package-info.java` vazio que
existia em `config/`):

```
order-service/src/main/java/com/poc/orderservice/
├── config/
│   ├── KafkaTopicConfig.java       # bean NewTopic: orders.created, 3 partições, RF 1
│   └── KafkaProducerConfig.java    # ProducerFactory/KafkaTemplate (ObjectMapper + JavaTimeModule)
└── adapters/out/messaging/
    └── KafkaOrderEventPublisher.java   # implementação real (antes: stub "[KAFKA-STUB]")
```

**O que foi implementado**:

1. **`pom.xml`**: adicionado `spring-kafka` (dependência principal) e
   `spring-kafka-test` (scope `test`, será usado na Etapa 4 com
   Testcontainers).

2. **`application.properties`**:
   ```properties
   spring.application.name=order-service
   server.port=8080

   # Kafka - usado pelo KafkaAdmin (criacao do topico) e pelo producer
   # (ProducerFactory/KafkaTemplate configurados explicitamente em KafkaProducerConfig,
   # para usar um ObjectMapper com JavaTimeModule - ver javadoc da classe)
   spring.kafka.bootstrap-servers=localhost:29092

   # Topico publicado pelo order-service (criado via bean NewTopic - broker tem auto.create.topics.enable=false)
   app.kafka.topic.order-created=orders.created
   ```

3. **`config/KafkaTopicConfig.java`**: bean `NewTopic` (via
   `TopicBuilder.name("orders.created").partitions(3).replicas(1)`). É
   detectado automaticamente pelo `KafkaAdmin` (autoconfigurado pelo
   Spring Boot a partir de `spring.kafka.bootstrap-servers`) e usado para
   criar o tópico na inicialização da aplicação, já que o broker está com
   `auto.create.topics.enable=false`.

4. **`config/KafkaProducerConfig.java`**: define os beans
   `ProducerFactory<String, Object>` e `KafkaTemplate<String, Object>`
   explicitamente — motivo detalhado no "Achado importante" abaixo.

5. **`adapters/out/messaging/KafkaOrderEventPublisher.java`**: substitui o
   stub que apenas logava `[KAFKA-STUB]`. Implementação real:
   `publish(OrderCreatedEvent event)` chama
   `kafkaTemplate.send(topic, key, event)`, onde
   `key = event.orderId().toString()` — garante que todos os eventos do
   mesmo pedido caiam sempre na mesma partição (ordem preservada por
   pedido). O `CompletableFuture` retornado é tratado com
   `.whenComplete(...)`: em sucesso loga tópico/partição/offset, em falha
   loga o erro. A porta `OrderEventPublisher` e o `CreateOrderService`
   **não foram alterados** — só o adapter de saída.

**Achado importante (lição desta etapa)**: a primeira tentativa configurou
apenas a propriedade
`spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer`.
Resultado: o campo `occurredAt` (`Instant`) foi serializado como
**timestamp numérico** (`1781369447.190601900`) em vez de ISO-8601 —
porque o `JsonSerializer` instanciado a partir do nome da classe usa um
`ObjectMapper` padrão, sem `JavaTimeModule` registrado.

Correção aplicada:
- Removidas as propriedades `spring.kafka.producer.key-serializer` /
  `value-serializer` de `application.properties` — ficariam "mortas", já
  que os beans `ProducerFactory`/`KafkaTemplate` customizados sobrescrevem
  a autoconfiguração do Spring Boot.
- Criado `KafkaProducerConfig` com um `ObjectMapper` próprio
  (`registerModule(new JavaTimeModule())` +
  `disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)`), passado ao
  `JsonSerializer` via `Supplier` dentro do `DefaultKafkaProducerFactory`.
- Esse `ObjectMapper` é uma **instância local**, não exposta como
  `@Bean` — evita conflito com o `ObjectMapper` autoconfigurado pelo
  Spring Boot (`JacksonAutoConfiguration`, que usa
  `@ConditionalOnMissingBean(ObjectMapper.class)`).

**Validação realizada**:

| Verificação | Resultado |
|---|---|
| `mvn -q -DskipTests compile` | OK, sem erros |
| `mvn spring-boot:run` | App sobe na porta 8080; `KafkaAdmin` cria o tópico `orders.created` na inicialização |
| `GET /api/clusters/poc-kafka/topics/orders.created` (Kafka UI) | Tópico criado com **3 partições**, replication factor 1 |
| `POST /api/orders` `{"cpf":"123.456.789-00","salario":5000.00}` | HTTP 201, `orderId=6003592d-4c55-45a8-bdec-359e27f594a2` |
| Mensagem 1 no tópico (Kafka UI) | `partition: 1`, `key = "6003592d-4c55-45a8-bdec-359e27f594a2"` (= orderId) ✅. `occurredAt: 1781369447.190601900` ❌ — timestamp numérico (**antes da correção**) |
| Correção do `KafkaProducerConfig` aplicada + restart do order-service | App reiniciado com `JavaTimeModule` registrado no `ObjectMapper` do producer |
| `POST /api/orders` `{"cpf":"987.654.321-00","salario":3200.50}` | HTTP 201, `orderId=95096799-19a3-429c-9094-28dec013eb03` |
| Mensagem 2 no tópico (Kafka UI) | `partition: 2`, `key = "95096799-19a3-429c-9094-28dec013eb03"` (= orderId) ✅. `occurredAt: "2026-06-13T16:53:29.316769500Z"` ✅ — ISO-8601 (**fix confirmado**) |
| Distribuição de partições | Mensagens 1 e 2 caíram em partições diferentes (1 e 2) — confirma particionamento por `orderId` (hash da key) |

> A mensagem 1 (com `occurredAt` no formato numérico) permanece no tópico
> como artefato do "antes da correção" — fica como evidência real do
> problema encontrado e da correção aplicada, útil para quem revisar esta
> POC de estudo depois.

Próximo passo: **Etapa 3** — criar o `notification-service` (novo projeto
Maven, Consumer) para consumir o tópico `orders.created` e logar a
notificação recebida.

### Organização - alinhamento com projeto de referência (agenda-hexagonal) ✅

**Motivação**: o usuário comparou `order-service` com outro projeto de
estudo, `agenda-hexagonal` (`C:\workspace_eclipse\agenda-hexagonal`), também
sobre Arquitetura Hexagonal. A comparação revelou duas divergências entre os
"cores" dos dois projetos.

**Análise SOLID feita antes de refatorar** (revisão do código existente de
`order-service`):

| Princípio | Situação antes da refatoração |
|---|---|
| DIP | Já correto — `CreateOrderService` depende de `OrderEventPublisher`/`CreateOrderUseCase` (abstrações), nunca de `KafkaOrderEventPublisher` |
| ISP | Já satisfeito — `CreateOrderUseCase` tem um único método; ao crescer, seguir o padrão de interfaces segregadas de `agenda-hexagonal` (`ListarContatosUseCase`, `BuscarContatoUseCase`, etc.) |
| SRP/OCP | Já correto — cada classe com responsabilidade única; novos adapters de saída não exigem alterar `CreateOrderService` |
| Domínio (`Order`, `OrderItem`, `OrderCreatedEvent`) | Já eram records imutáveis, sem dependência de framework |

Conclusão: as **duas únicas divergências reais** eram a localização do
pacote `domain` e o uso de `@Service` em vez de uma anotação que isolasse o
Spring do core. As duas foram corrigidas nesta etapa.

**Mudança 1 — pacote `domain` movido para dentro de `application`**

`com.poc.orderservice.domain` → `com.poc.orderservice.application.domain`
(`Order.java`, `OrderItem.java`, `OrderCreatedEvent.java`). Em
`agenda-hexagonal`, `application/` representa todo o "hexágono" (domínio +
ports + casos de uso) e `domain` é um sub-pacote dele; `order-service`
agora segue a mesma convenção. Pacote antigo `com.poc.orderservice.domain`
removido.

Imports/javadocs `{@link}` atualizados em:
- `application/port/in/CreateOrderUseCase.java`
- `application/port/out/OrderEventPublisher.java`
- `application/service/CreateOrderService.java`
- `adapters/in/web/OrderController.java`
- `adapters/out/messaging/KafkaOrderEventPublisher.java`
- `config/KafkaProducerConfig.java` (referência fully-qualified em javadoc)

**Mudança 2 — meta-anotação `util/UseCase.java` (isola o Spring do core)**

Novo arquivo `com.poc.orderservice.util.UseCase`:
`@Target(TYPE) @Retention(RUNTIME) @Documented @Component public @interface UseCase {}`
— espelha o `util/UseCase.java` de `agenda-hexagonal`.

`application/service/CreateOrderService.java` passou a usar `@UseCase` em
vez de `@Service` (`org.springframework.stereotype.Service`). O Spring
continua reconhecendo a classe como bean (herança de anotação: `@UseCase`
carrega `@Component`), mas o pacote `application/` deixa de ter qualquer
import de `org.springframework.*` — o único import de Spring do core fica
concentrado em `util/UseCase.java`, fora de `application/`. Isso permite, no
futuro, testar `CreateOrderService` instanciando-o diretamente com um mock
de `OrderEventPublisher`, sem subir contexto Spring. Optou-se por
`@Component` (estereótipo genérico) e não `@Service` como anotação embutida,
pois `@Service` carrega semântica de "camada de serviço" do MVC que não
existe na Arquitetura Hexagonal.

**Mudança 3 — clean code**

`adapters/in/web/OrderController.java`: normalizada a indentação do
construtor e do método `create` (havia tabs e linhas em branco
inconsistentes), sem mudança de comportamento.

**Estrutura final do core (`application/`) e novo pacote `util/`**:

```
order-service/src/main/java/com/poc/orderservice/
├── application/
│   ├── domain/                 # NOVO local (antes: com.poc.orderservice.domain, pacote irmão)
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   └── OrderCreatedEvent.java
│   ├── port/
│   │   ├── in/  (CreateOrderCommand, CreateOrderUseCase)
│   │   └── out/ (OrderEventPublisher)
│   └── service/
│       └── CreateOrderService.java   # @UseCase em vez de @Service
└── util/
    └── UseCase.java               # NOVO - meta-anotação @UseCase = @Component
```

**Validação realizada**:

| Verificação | Resultado |
|---|---|
| Busca por referências ao pacote antigo `com.poc.orderservice.domain` | Nenhuma ocorrência restante |
| `mvn -q -DskipTests compile` | OK, sem erros |
| Restart manual do order-service + `POST /api/orders` (feito pelo usuário) | HTTP 201, evento publicado normalmente no Kafka — comportamento idêntico ao pré-refatoração |

Próximo passo: **Etapa 3** — criar o `notification-service` (novo projeto
Maven, Consumer) para consumir o tópico `orders.created` e logar a
notificação recebida.
