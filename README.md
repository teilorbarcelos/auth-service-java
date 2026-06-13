# Java Quarkus Backend — Enterprise Edition

Backend de alta performance construído com **Java 21** e **Quarkus 3.x**,
seguindo arquitetura modular, Clean Code e SOLID. Otimizado para alta
escalabilidade e concorrência desde o boilerplate.

---

## Tecnologias e Ferramentas

| Camada | Tecnologia |
|--------|-----------|
| **Core** | Java 21 + Quarkus 3.21.x |
| **ORM** | Hibernate ORM com Panache (Repository Pattern) |
| **Database** | PostgreSQL (prod/dev) / H2 (testes) |
| **Migrações** | Flyway |
| **Cache/Sessão** | Redis |
| **Mensageria** | SmallRye Reactive Messaging RabbitMQ |
| **Auth** | JWT HMAC‑SHA256 + Session Versioning (Redis) |
| **API Docs** | OpenAPI 3.0 / Swagger UI |
| **Métricas** | Micrometer + Prometheus |
| **Logs** | JSON estruturado + MDC |
| **Qualidade** | JaCoCo 100% (instructions & branches) |

---

## Estrutura do Projeto

```
src/main/java/com/app/
├── core/               # BaseEntity, BaseService, BaseResource, UuidGenerator
├── infrastructure/     # JWT, Redis, Email, Logs, Storage, Rate Limit, PDF
└── modules/            # Domínios de negócio (Product, User, Role, Auth…)
    └── Product/
        ├── ProductModel.java
        ├── ProductRepository.java
        ├── ProductService.java
        └── ProductResource.java
```

---

## Otimizações de Escalabilidade e Concorrência

### Session Versioning (Redis)
Token validation deixou de usar `SADD` + `SISMEMBER` (set cresce com cada login).
Agora usa um **contador de versão** por usuário (`session:user:{id}:version`).
O JWT carrega um claim `sv`; a validação é um único `GET` no Redis.
Invalidação: incrementa `Auth.sessionVersion` no banco + `DEL` no Redis.

### Rate Limit Atômico
Uma única chamada `INCR` + `EXPIRE` condicional.
Race condition eliminada; latência do Redis reduzida à metade.

### Índices de Performance
- B‑tree composto `(is_deleted, active, created_at DESC)` em todas as tabelas de domínio.
- GIN trigram funcional em `immutable_unaccent(lower(col))` nos campos `searchableFields` — acelera `LIKE '%palavra%'` com acentos.

### UUID v7 (Time‑Ordered)
Geração de UUIDs monotônicos (RFC 9562) — reduz fragmentação em índices B‑tree
comparado a UUID v4. Implementação pura Java (`UuidGenerator`), sem dependências.

### JVM Container‑Aware
Flags `-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75.0`, `-XX:+UseG1GC`,
`-XX:+ExitOnOutOfMemoryError`. O G1 enxerga os limites de memória do container.

### Virtual Threads
`quarkus.virtual-threads.enabled=true` — I/O bloqueante não consome carrier threads.

### Pool de Conexões
`min-size=10`, `max-size=50`, `acquisition-timeout=5S`, `statement-batch-size=25`.
Com leak detection e validation query.

### Fast‑Jar
Build produz `quarkus-app/` — layers de dependências separadas para melhor cache
de imagem Docker.

### Prometheus Cardinalidade Controlada
Labels de métricas usam path normalizado (regex remove UUIDs e números),
evitando explosão de séries temporais.

### MDC Logging
`requestId` e `userId` no MDC — toda linha de log carrega contexto sem
parametrização explícita.

### Optimistic Locking
`@Version` em todas as entidades — concorrência em escritas lança
`OptimisticLockException` → `409 Conflict` para o cliente retentar.

### Compressão HTTP
Respostas JSON comprimidas com gzip (nível 6).

---

## 🔐 Segurança & RBAC

### Autenticação (Session Versioning)

1. **Login**: lê `Auth.sessionVersion` do banco, embute como claim `sv` no JWT,
   escreve `session:user:{id}:version` no Redis.
2. **Validação** (`AuthFilter`): extrai `sv` do JWT, compara com Redis.
3. **Invalidação**: incrementa versão no banco + deleta chave Redis.
   Todas as sessões do usuário são revogadas instantaneamente.

### Autorização Declarativa

```java
@ResourceFeature("product")
public class ProductResource extends BaseResource<...> { }
```

Permissões aplicadas via `PermissionFilter` com base no papel do usuário.

---

## 📩 Mensageria (RabbitMQ)

Integração via **SmallRye Reactive Messaging RabbitMQ** (`quarkus-messaging-rabbitmq`).
Thread‑safe, lifecycle gerenciado pelo Quarkus, health check automático.

```bash
MESSAGING_ENABLED=true
RABBIT_HOST=rabbitmq
RABBIT_PORT=5672
```

---

## 📦 Storage

Agnóstico e extensível. Troca entre local / S3 / GCS / Azure via config.

```bash
STORAGE_DISK=local
```

---

## 📄 Geração de PDF

Streaming bypass — o backend atua como proxy, sem carregar o PDF inteiro na RAM.
Serviço externo em `http://localhost:8889`.

---

## 📊 Observabilidade

| Recurso | Endpoint |
|---------|----------|
| Health | `/health` (DB, Redis, RabbitMQ, Storage) |
| Métricas | `/metrics` (Prometheus) |
| Logs | JSON estruturado com MDC (`requestId`, `userId`) |
| OpenAPI | `/v1/docs` (Swagger UI) |

Subir stack de métricas:

```bash
make metrics-up
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3001  (admin/admin)
```

---

## Comandos

```bash
make dev          # Hot Reload
make test         # Testes + cobertura
make coverage     # Testes + relatório de cobertura
make generate name=Modulo  # Gerar novo módulo CRUD
make infra-up     # Infraestrutura (DB, Redis, RabbitMQ)
```

---

## Qualidade

- **100% de cobertura** (instruções + branches) — trava no CI/CD.
- **SonarQube** — quality gate em `http://localhost:9000`.
- **Compliance** — suíte E2E em `mage-backend-compliance`.
- **Auditoria** — `audit.tb_audit` + `audit.tb_error_log`.
