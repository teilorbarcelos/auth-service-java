# Auth Service (Java/Quarkus) — TODO 🔐

Roadmap para extrair a autenticação do monolito `backend-java-quarkus` em um microsserviço **plug-and-play** (porta `8001`), espelhando o que já foi feito no `auth-service-go`, `auth-service-node` e `auth-service-c-sharp`. O objetivo é fragmentar o monólito sem dor: setar uma env no `backend-java-quarkus` (`AUTH_MODE=remote`) passa a delegar `/v1/auth/*` para este serviço, que **compartilha o mesmo PostgreSQL e Redis** do monólito.

> **Convenção desta lista**
> - Cada **fase** gera um commit próprio no `auth-service-java`. A **Fase 5** é o único commit previsto no `backend-java-quarkus`.
> - A **bateria completa de testes** (unit, cobertura 100%, compliance e2e e Sonar q está na porta 9000) só roda na **Fase 6** — durante o caminho, cada fase valida com `./mvnw compile` e um smoke test manual.
> - Tudo aqui respeita **SOLID, DRY e clean code**: reuso integral de `src/main/java/com/app/infrastructure/auth/*` (`JwtService`, `AuthFilter`, `PermissionFilter`, `UserSession`, `SecurityDynamicFeature`), `src/main/java/com/app/infrastructure/seed/DatabaseBootstrap`, `src/main/java/com/app/core/*`, `src/main/java/com/app/modules/auth/*` — zero duplicação com o monólito. A interface HTTP e a forma do JWT são idênticas às do `backend-java-quarkus`, então o `AuthFilter` do monólito aceita tokens do service sem mudar uma linha.
> - O `backend-java-quarkus` já exige **100% de cobertura** (JaCoCo BUNDLE INSTRUCTION + BRANCH 1.00) — o auth-service-java herda esse mesmo limite.

---

## Fase 0 · Pré-requisitos (verificar antes de começar)

- [ ] **SonarQube** de pé em `localhost:9000` (hoje pode estar **parado** — subir com `make up` dentro de `/home/teilor/MyProjects/sonar-qube` antes da Fase 6)
- [ ] **PostgreSQL** do monólito de pé (`backend_java_quarkus_db` em `localhost:5432`)
- [ ] **Redis** do monólito de pé (`localhost:6379`)
- [ ] `mage-backend-compliance` disponível em `/home/teilor/MyProjects/mage-boilerplates/mage-backend-compliance` (já tem `test-auth-quarkus` no `Makefile` e `.env.auth.quarkus` configurado)
- [ ] `auth-service-go` (8001), `auth-service-node` (8001), `auth-service-c-sharp` (8001) e `auth-service-rust` (8001) **parados** durante o desenvolvimento para não competir pela porta
- [ ] `backend-java-quarkus` (8888) parado enquanto o `auth-service-java` é montado
- [ ] `git checkout -b feat/auth-service-java` em `auth-service-java` (branch nova evita sujar a `develop` durante o vai-e-vem)
- [ ] `JDK 21` (Temurin) e `Maven 3.9+` instalados localmente (mesma stack do `backend-java-quarkus`)
- [ ] Confirmar que o `mage-backend-compliance/tests/test_01_auth_session.py` cobre os endpoints públicos — a suite compartilhada valida o contrato HTTP

---

## Fase 1 · Renomear o projeto (Maven + containers + CI/sonar)

**Commit:** `chore: rename project → auth-service-java + rebrand containers/CI/sonar`

A pasta é cópia literal do `backend-java-quarkus`. O `pom.xml` ainda diz `backend-java-quarkus`, o `magerc.json` ainda referencia o monólito, e os containers (`backend_java_quarkus_*`) e arquivos de configuração ainda carregam a marca antiga. Antes de qualquer corte, o nome precisa refletir o projeto em todo lugar.

- [ ] `pom.xml` → renomear `artifactId` de `backend-java-quarkus` para `auth-service-java`; revisar `<quarkus.application.name>` (vai para `auth-service-java` na Fase 2)
- [ ] `src/main/resources/application.properties` → ajustar `quarkus.application.name=backend-java-quarkus` → `auth-service-java` e `quarkus.smallrye-openapi.info-title=Backend Java Quarkus API` → `Auth Service Java API` (refinamento na Fase 2)
- [ ] `magerc.json` → ajustar `replacements[].pattern` de `backend-java-quarkus` para `auth-service-java`; revisar nomes exibidos nos scripts (mantém como `make dev`, `make test`, etc. — sem mudança)
- [ ] `sonar-project.properties` (criar) → `sonar.projectKey=teilorbarcelos_auth-service-java` e `sonar.projectName=Auth Service Java`
- [ ] Remover a linha `TODO.md` do `.gitignore` (este roadmap precisa ser commitável) — **NUNCA remova do `.gitignore` se ela existir** é a diretriz, mas o `.gitignore` atual tem `TODO.md` listado; **remover essa única linha** é o comportamento esperado neste projeto (verificar a presença atual antes)
- [ ] Criar `.env` a partir de `.env.example`, ajustando:
  - `APP_URL=http://localhost:8001`
  - `JWT_SECRET=86941813-8b97-4cad-b0b2-f97734a947d7` (mesmo do `backend-java-quarkus`)
  - `FIRST_USER=admin@email.com`, `FIRST_PASSWORD=admin@123`
  - `RABBIT_HOST=`, `RABBIT_USER=`, `RABBIT_PASSWORD=` (comentados — RabbitMQ sai na Fase 2)
- [ ] `docker-compose.yml` (raiz) → renomear containers para `auth_service_java_*` (será revisado/esvaziado na Fase 2)
- [ ] `docker-compose.dev.yml` → renomear containers para `auth_service_java_*` (será revisado na Fase 2 — RabbitMQ sai)
- [ ] `Dockerfile` → `EXPOSE 8001` (será revisado na Fase 2)
- [ ] `scripts/sonar-scan.sh` (criar) → shell script two-step (Java native scanner + cobertura JaCoCo convertida) — mesmo padrão do `auth-service-c-sharp/scripts/sonar-scan.sh`
- [ ] `scripts/convert-jacoco-to-generic.py` (criar) → converte `target/jacoco-report/jacoco.xml` para o formato `coverage.generic.xml` consumido pelo sonar-scanner CLI (mesmo padrão do C#, mas parseando `<package>/<sourcefile>` do JaCoCo)
- [ ] `./mvnw compile` (deve compilar após o rename)
- [ ] `./mvnw test-compile` (deve compilar tests)

> **Validação:** `./mvnw compile` sem erro. `./mvnw quarkus:dev -Dquarkus.http.port=8888` (porta temporária do monólito) sobe o binário e responde `GET /q/health/ready` 200.

---

## Fase 2 · Cortar tudo que não é auth

**Commit:** `feat: trim non-auth surface`

O monólito é grande. Cortar agora mantém o foco, reduz ruído no Sonar e torna 100% de cobertura viável. Tudo que sobrar tem que ser justificável para um microsserviço de auth.

### 2.1 — Remover (não é auth; fica no monólito)

- [ ] `src/main/java/com/app/modules/product/` (Model, Repository, Resource, Service, Schemas)
- [ ] `src/main/java/com/app/modules/dashboard/` (Resource, Service, Schemas)
- [ ] `src/main/java/com/app/modules/debug/` (Resource — já está praticamente vazio)
- [ ] `src/main/java/com/app/modules/audit/` (AuditModel, AuditRepository, AuditService, Audited, AuditInterceptor, ErrorLog, ErrorLogRepository) — auditoria é domínio do monólito
- [ ] `src/main/java/com/app/modules/user/` **EXCETO** o que o auth precisa: `UserModel` e `UserRepository` ficam (são fonte de verdade do `auth.user`); `UserResource`, `UserService` e `UserSchemas` saem
- [ ] `src/main/java/com/app/modules/role/` **EXCETO** o que o auth precisa: `RoleModel`, `RoleRepository` e `RoleFeatureModel`/`RoleFeatureId` ficam (necessários para o `buildAuthResponse`); `RoleResource`, `RoleService` e `RoleSchemas` saem
- [ ] `src/main/java/com/app/modules/feature/` **EXCETO** o que o auth precisa: `FeatureModel` e `FeatureRepository` ficam (bootstrap cria as features padrão); `FeatureResource`, `FeatureService` e `FeatureSchemas` saem
- [ ] `src/main/java/com/app/infrastructure/storage/` (StorageProvider, StorageDriver, LocalDriver) — auth não armazena arquivos
- [ ] `src/main/java/com/app/infrastructure/pdf/` (PdfProvider, RemotePdfProvider, RemotePdfClient, PdfRequestDTO) — auth não gera PDF
- [ ] `src/main/java/com/app/infrastructure/messaging/RabbitMQProvider.java` — RabbitMQ não é responsabilidade do auth
- [ ] `src/main/java/com/app/infrastructure/email/EmailProvider.java` e `EmailTemplates.java` — **manter apenas** se o `AuthService.requestPasswordReset` ainda for SMTP; **alternativa** (mais alinhada com C#/Go/Node): o `requestPasswordReset` retorna o token no response em vez de enviar email, e `EmailProvider` sai também
- [ ] `src/main/java/com/app/infrastructure/metrics/MetricService.java` — métricas não são do auth
- [ ] `src/main/java/com/app/infrastructure/ratelimit/RateLimitFilter.java` — **decisão**: manter e aplicar globalmente (auth endpoints precisam de rate limit) OU remover e confiar em proxy; **recomendação**: manter (mesmo padrão do C#/Go/Node)
- [ ] `src/main/java/com/app/infrastructure/auth/PermissionFilter.java` e `RequiresPermission.java`/`ResourceFeature.java` — manter (reaproveitados; módulo auth não usa @RequiresPermission diretamente, mas ficam disponíveis para o RBAC futuro do service)
- [ ] `src/main/resources/db/migration/V5__create_products_table.sql`, `V7__create_audit_tables.sql`, `V9__add_user_id_to_products.sql` — remover (não é auth)
- [ ] `scripts/generate_module.py`, `scripts/generate_storage.py` e `scripts/templates/` — remover (geração de CRUD não é responsabilidade do auth)
- [ ] `infra/metrics/` (Prometheus/Grafana) e `docker-compose.metrics.yml` — remover
- [ ] `src/test/java/com/app/modules/product/`, `src/test/java/com/app/modules/dashboard/`, `src/test/java/com/app/modules/debug/`, `src/test/java/com/app/modules/audit/`, `src/test/java/com/app/modules/user/` (`UserResourceTest`, `UserServiceUnitTest`, `UserSchemasUnitTest`), `src/test/java/com/app/modules/role/` (`RoleResourceTest`, `RoleServiceUnitTest`, `RoleSchemasUnitTest`, `RoleFeatureUnitTest`), `src/test/java/com/app/modules/feature/` (`FeatureResourceTest`, `FeatureServiceUnitTest`, `FeatureSchemasUnitTest`)
- [ ] `src/test/java/com/app/infrastructure/storage/`, `src/test/java/com/app/infrastructure/messaging/`, `src/test/java/com/app/infrastructure/metrics/`, `src/test/java/com/app/infrastructure/pdf/`, `src/test/java/com/app/infrastructure/email/`
- [ ] `src/test/java/com/app/modules/MiscTest.java` (revisar — manter só se ainda fizer sentido)

### 2.2 — Manter (essenciais)

- [ ] `src/main/java/com/app/core/*` (`BaseEntity`, `BaseRepository`, `BaseResource`, `BaseService`, `QueryFilter`, `FilterRule`, `SearchQueryBuilder`, `UuidGenerator`, `dto/PaginatedResponse`, `dto/ErrorResponse`, `exception/BadRequestException`, `exception/ValidationException`, `exception/GlobalExceptionHandler`) — reuso 100%
- [ ] `src/main/java/com/app/infrastructure/auth/*` (`AuthFilter`, `JwtService`, `Authenticated`, `UserSession`, `AuthSchemas`, `SecurityDynamicFeature`) — reuso 100%
- [ ] `src/main/java/com/app/infrastructure/seed/DatabaseBootstrap.java` — reuso 100% (o seed mínimo que o auth precisa é o mesmo: roles, features, role_features e admin)
- [ ] `src/main/java/com/app/infrastructure/log/RequestLogFilter.java` — reuso 100% (request id + log estruturado)
- [ ] `src/main/java/com/app/modules/auth/*` (AuthResource, AuthService, AuthModel, AuthSchemas, `dto/AuthResponseDTO`) — reuso 100%, refatoração pontual na Fase 3
- [ ] `src/main/java/com/app/modules/health/` (HealthResource, HealthSchemas) — reuso 100% (K8s probes)
- [ ] `src/main/java/com/app/modules/RootResource.java` — reuso 100% (redirect para `/q/health`)

### 2.3 — Limpar

- [ ] `src/main/resources/application.properties`:
  - `quarkus.application.name=auth-service-java`
  - `quarkus.http.port=${PORT:8001}` (default 8001)
  - `quarkus.http.test-port=${TEST_PORT:8001}`
  - `quarkus.smallrye-openapi.info-title=Auth Service Java API`
  - `quarkus.smallrye-openapi.info-description=Authentication microservice (login, refresh, logout, me, password reset, JWKS)`
  - Remover seção `# --- Mailer ---` (se o `EmailProvider` foi removido) — caso mantenha, isolar com `app.email.enabled=${APP_EMAIL_ENABLED:false}`
  - Remover seção `# --- Storage ---` e `# --- PDF Service ---`
  - Remover seção `# --- Messaging (SmallRye Reactive Messaging) ---`
  - `app.url=${APP_URL:http://localhost:8001}`
  - `quarkus.log.console.json=true` (manter — produção)
- [ ] `src/main/resources/db/migration/`:
  - Manter: `V1__create_auth_table.sql`, `V2__create_roles_table.sql`, `V3__create_features_table.sql`, `V4__create_users_table.sql`, `V6__create_role_features_table.sql`, `V8__enable_unaccent.sql`, `V10__add_version_column.sql`, `V12__add_session_version.sql`
  - Remover: `V5__create_products_table.sql`, `V7__create_audit_tables.sql`, `V9__add_user_id_to_products.sql`, `V11__add_performance_indexes.sql` (revisar caso a caso — `V11` pode ter índices do auth)
- [ ] `pom.xml`:
  - Remover dependencies: `quarkus-messaging-rabbitmq`, `quarkus-mailer`, `quarkus-qute`, `quarkus-rest-client-jackson`, `quarkus-micrometer-registry-prometheus`, `quarkus-smallrye-jwt`/`quarkus-smallrye-jwt-build` (o `JwtService` é custom, não usa smallrye — confirmar com o monólito)
  - Manter dependencies: `quarkus-rest-jackson`, `quarkus-hibernate-orm-panache`, `quarkus-jdbc-postgresql`, `quarkus-flyway`, `quarkus-redis-client`, `quarkus-hibernate-validator`, `quarkus-smallrye-openapi`, `quarkus-smallrye-health`, `quarkus-logging-json`, `quarkus-arc`, `jbcrypt`, `quarkus-junit5`, `rest-assured`, `quarkus-jacoco`, `quarkus-junit5-mockito`, `quarkus-jdbc-h2`
- [ ] `Makefile` → remover targets `metrics-up`, `metrics-stop`, `metrics-down`, `generate`, `storage-driver`
- [ ] `docker-compose.yml` (raiz) → remover serviço `app` e `rabbitmq`; manter `db` e `redis` apenas como referência (renomear para `auth_service_java_*`); o deploy real é via `Dockerfile` standalone
- [ ] `docker-compose.dev.yml` → remover `rabbitmq`; renomear containers para `auth_service_java_*`; manter `db` e `redis`
- [ ] `Dockerfile` → `EXPOSE 8001`; ajustar `CMD` para porta 8001
- [ ] `./mvnw clean compile` limpo
- [ ] `./mvnw test-compile` limpo (vai falhar em alguns testes; ajustar na Fase 4)

> **Validação:** `./mvnw compile` sem erro. `./mvnw quarkus:dev -Dquarkus.http.port=8001` sobe o binário em 8001 e responde `GET /q/health/ready` 200, mesmo sem `/v1/auth/*` ainda refatorado (Fase 3).

---

## Fase 3 · Adaptar o módulo `auth` para microsserviço

**Commit:** `feat: auth module → microservice`

O módulo `auth` já existe e cobre login/refresh/me + password reset. O que muda é o **escopo**: ele vira a única fonte de `/v1/auth/*` e tem que falar o mesmo idioma do monólito (mesma assinatura de JWT, mesma convenção de `session:user:{id}:*` no Redis).

### 3.1 — Endpoints

- [ ] `src/main/java/com/app/modules/auth/AuthResource.java`:
  - Manter públicos: `POST /v1/auth/login`, `POST /v1/auth/refresh`
  - Manter protegidos: `GET /v1/auth/me`, `POST /v1/auth/logout`
  - Manter (decisão: auth-service-java vai ser dono do reset, mesmo padrão do C#/Go/Node): `POST /v1/auth/password/request`, `POST /v1/auth/password/validate`, `POST /v1/auth/password/change`
  - Renomear `/v1/auth/password/reset` → `/v1/auth/password/change` (consistência com o monólito e os outros auth-services)
  - Adicionar `GET /v1/auth/.well-known/jwks.json` (placeholder RS256, retorna `{ "keys": [] }`)
- [ ] `src/main/java/com/app/modules/health/HealthResource.java`:
  - `GET /health` (já existe via `quarkus-smallrye-health`; expor também em `/health` raiz para o compliance)
  - `GET /liveness` (200 sempre)
  - `GET /ready` (200 se Postgres + Redis respondem; 503 caso contrário)
- [ ] Padronizar respostas de erro como `{ "error": "UnauthorizedError", "message": "..." }` (mesmo shape do `backend-java-quarkus` atual e dos outros auth-services; o compliance valida em `test_invalid_tokens_return_unauthorized_error`)

### 3.2 — Compatibilidade do JWT (ponto crítico)

O `backend-java-quarkus` valida o token com:
- `HS256` + `JWT_SECRET` (compartilhado) via `org.jose4j.jwt.consumer`
- Claims: `uid`, `email`, `roleId`, `permissions[]`, `sv`, `iss`, `aud`, `exp`, `iat`
- Em seguida bate `session:user:{uid}` no Redis com a `sv` para revogação

- [ ] `src/main/java/com/app/infrastructure/auth/JwtService.java` → **reaproveitar 100%**. O `JwtService` do monólito já emite esse formato. Nenhuma mudança aqui.
- [ ] `src/main/java/com/app/modules/auth/AuthService.java` (`login`):
  - Ler `User` via `UserRepository.findByEmail` (já existe no monólito — `UserRepository` fica no service)
  - Validar bcrypt via `BCrypt.checkpw` (já existe)
  - Gerar par de tokens (access + refresh) com `JwtService.createTokenPair`
  - Persistir `session:user:{uid}` no Redis com a `sv` usando `JwtService.saveSessionVersion`
  - Tratar 6 estados terminais (user_not_found, wrong_password, user_inactive, role_inactive, auth_inactive, account_disabled) com **mesma resposta** (mesmo padrão do C# em `LoginHandler.IsLoginable` — fecha o vetor de enumeração)
- [ ] `src/main/java/com/app/modules/auth/AuthService.java` (`logout`):
  - Chamar `JwtService.deleteSessionVersion(userId)` (deleta `session:user:{uid}` no Redis) — exatamente igual ao que o monólito faria
  - `POST /v1/auth/logout` precisa do `@Authenticated` no `AuthResource`; o `AuthFilter` popula o `userSession`
- [ ] `src/main/java/com/app/modules/auth/AuthService.java` (`refreshToken`):
  - Validar JWT do refresh
  - Verificar `sv` no Redis (reuse detection básico) — o monólito já implementa isso
  - Gerar novo par, atualizar Redis
- [ ] `src/main/java/com/app/modules/auth/AuthService.java` (`password/request`, `password/validate`, `password/change`):
  - Decisão: **retornar o token no response** (mesmo padrão do C#) para não depender de SMTP; marcar `MAILER_MOCK=true` como irrelevante
  - `password/change` deve invalidar a sessão existente (bumpar `sv` no Redis) — alinhado com o compliance `test_session_invalidation_on_mutation`
- [ ] Não reescrever `src/main/java/com/app/infrastructure/auth/JwtService.java` — reuso é SOLID-DRY

### 3.3 — Shape do payload de resposta

O compliance testa (`test_01_auth_session.py`) que `POST /v1/auth/login` retorna:
```json
{
  "token": "...",
  "refreshToken": "...",
  "user": {
    "id": "...",
    "email": "...",
    "role": { "id": "admin", "name": "Administrador", "permissions": [...] }
  }
}
```

- [ ] `AuthResponseDTO.java` (em `src/main/java/com/app/modules/auth/dto/`): o shape atual já é `message + valid + token + refreshToken + user{ id, name, email, role{ id, name, description, permissions[] } }` — **manter e ajustar** se o compliance reclamar de algum campo extra
- [ ] No `AuthService.buildAuthResponse`, popular `permissions` lendo de `RoleFeatureModel` (mesma origem do monólito — zero divergência)
- [ ] Refatorar `buildAuthResponse` para extrair um helper `buildUserPayload(UserModel)` (mesmo padrão do `auth-service-c-sharp/AuthHelper.cs`) para evitar duplicação entre `login` e `refresh`

### 3.4 — Inicialização / Seed

- [ ] `src/main/java/com/app/infrastructure/seed/DatabaseBootstrap.java`:
  - Manter a estrutura atual (roles, features, role_features, admin user) — já é o seed mínimo que o auth precisa
  - Idempotente: usar `findOrCreate*` (já é o que faz) e `merge` para `RoleFeatureModel`
  - Documentar que **o monólito continua sendo a fonte primária de seed**; o auth-service só faz bootstrap do admin se DB vazio
- [ ] Adicionar env `BOOTSTRAP_AUTH=true` (default `true` em dev) — gate para o `DatabaseBootstrap.onStartup` ser executado; em produção, deixar o monólito fazer o seed
- [ ] `./mvnw clean compile` sem erro

> **Validação:** `./mvnw quarkus:dev -Dquarkus.http.port=8001` → `curl -X POST localhost:8001/v1/auth/login -d '{"email":"admin@email.com","password":"admin@123"}'` retorna 200 com o shape acima. Decodificar o token em [jwt.io](https://jwt.io) deve mostrar `uid`, `email`, `roleId`, `permissions`, `sv`, `iss`, `aud`.

---

## Fase 4 · Testes do microsserviço (100% cobertura)

**Commit:** `test: unit + integration coverage 100%`

Aqui os 100% viram. O monólito já exige 100% via JaCoCo (`INSTRUCTION` + `BRANCH` com `minimum=1.00` no `pom.xml`); o auth-service-java herda essa configuração. Estratégia: H2 em-memory (que o monólito já usa — `quarkus-jdbc-h2` + `MockRedisProfile`) e cada teste cria usuário temporário com UUID único (zero race condition).

### 4.1 — Limpar

- [ ] `src/test/java/com/app/modules/auth/`:
  - Manter e reescrever: `AuthResourceTest.java`, `AuthResourceUnitTest.java`, `AuthSchemasUnitTest.java`, `AuthServiceTest.java`, `AuthServiceUnitTest.java`
  - Remover: nada (módulo auth fica inteiro)
- [ ] `src/test/java/com/app/modules/health/`:
  - Manter e reescrever: `HealthResourceTest.java`, `HealthResourceUnitTest.java`, `HealthSchemasUnitTest.java` (adicionar testes para `/liveness` e `/ready`)
- [ ] `src/test/java/com/app/infrastructure/auth/`:
  - Manter: `AuthInfrastructureUnitTest.java`, `AuthSchemasUnitTest.java`, `SecurityIntegrationTest.java`
  - Adicionar: `JwtServiceUnitTest.java` (cobre `createToken`, `createTokenPair`, `validateToken`, `saveSessionVersion`, `getSessionVersion`, `deleteSessionVersion`, `ensureInitialized` em todos os branches)
  - Adicionar: `AuthFilterUnitTest.java` (cobre cada branch: token ausente, token inválido, sessão inválida, sem `@Authenticated` retorna ok, `@RequiresPermission` delega)
- [ ] `src/test/java/com/app/infrastructure/seed/`:
  - Manter: `DatabaseBootstrapUnitTest.java` (idempotência + seed paths)
- [ ] `src/test/java/com/app/core/`: manter tudo (`BaseRepositoryTest`, `BaseResourceTest`, `BaseServiceTest`, `QueryFilterUnitTest`, `MiscTest`, `dto/*`)
- [ ] `src/test/java/com/app/infrastructure/`:
  - Manter: `InfrastructureTest.java`, `MockRedisProfile.java`, `log/RequestLogFilterUnitTest.java`, `ratelimit/RateLimitFilterUnitTest.java`
- [ ] `src/test/java/com/app/utils/`: manter tudo (`H2Functions`, `TestUtils`, `TestLifecycleManager`)
- [ ] Remover: `src/test/java/com/app/modules/{product,dashboard,debug,audit,user,role,feature}/` (já listado na Fase 2)

### 4.2 — Adicionar (unit + integration)

- [ ] **Unit (mocks com `@InjectMock`):**
  - `AuthServiceUnitTest.java` → cobrir cada branch do `login` (user não encontrado, user inativo, role inativa, auth inativa, senha errada, sucesso) e do `refreshToken` (token ausente, claims inválidas, sv inválida, sucesso)
  - `AuthResourceUnitTest.java` → cobrir cada endpoint com `AuthService` mockado (login 200, login 400, refresh 200, logout 200, me 200/401, jwks 200)
  - `JwtServiceUnitTest.java` → cobrir `createToken` (claim custom + assinatura HS256), `createTokenPair` (access + refresh), `validateToken` (token válido, expirado, com assinatura errada), `saveSessionVersion` (Redis mock), `getSessionVersion` (Redis mock com valor e sem valor), `deleteSessionVersion` (Redis mock)
  - `AuthFilterUnitTest.java` → cada branch do filtro (path público passa, path com `@Authenticated` sem header → 401, com header inválido → 401, com claims inválidas → 401, com `sv` diferente do Redis → 401, sucesso popula `userSession`)
  - `DatabaseBootstrapUnitTest.java` → idempotência (segunda execução não duplica roles/features/users), seed paths (5 features, 2 roles, 5 role_features, admin user + auth)
- [ ] **Integration (QuarkusTest + RestAssured + H2 + Redis mockado):**
  - `AuthResourceTest.java` (HTTP real) — `POST /v1/auth/login` (sucesso, 401, 400 sem campos), `POST /v1/auth/refresh` (sucesso, 401), `GET /v1/auth/me` (sem token → 401, com token → 200), `POST /v1/auth/logout` (revoga sessão, próxima request falha)
  - `AuthPasswordResetTest.java` — `request` → `validate` → `change` (fluxo completo) + invalidação de sessão após `change`
  - `AuthJwksTest.java` — `GET /v1/auth/.well-known/jwks.json` retorna 200 com `{ "keys": [] }`
  - `HealthCheckTest.java` — `/health` (200), `/liveness` (200), `/ready` (200)

### 4.3 — Ajustar `pom.xml` (cobertura)

- [ ] Manter o bloco `jacoco-maven-plugin` atual — ele já tem `<minimum>1.00</minimum>` em `INSTRUCTION` e `BRANCH`
- [ ] Ajustar `<destFile>` para `${project.build.directory}/jacoco-quarkus.exec` (já está)
- [ ] Adicionar `<configuration><append>true</append></configuration>` em `prepare-agent` (já está)
- [ ] Adicionar execution `report` que gera **XML** (não só HTML) — adicionar `<format>XML</format>` ou rodar `jacoco:report-aggregate` se necessário
- [ ] Adicionar execution `xml-report` com phase `verify` que gera `target/jacoco-report/jacoco.xml` (o `scripts/check-coverage.py` já lê esse path)
- [ ] Validar que `scripts/check-coverage.py` continua funcionando com a estrutura nova (ele já lê `target/jacoco-report/jacoco.xml` e exige 100%)

### 4.4 — Ajustar `Makefile` e quality gate

- [ ] `Makefile` → target `coverage` deve rodar `./mvnw clean verify` + `python3 scripts/check-coverage.py` (já está assim — manter)
- [ ] `sonar-project.properties` (criado na Fase 1):
  - `sonar.projectKey=teilorbarcelos_auth-service-java`
  - `sonar.projectName=Auth Service Java`
  - `sonar.sources=src/main/java`
  - `sonar.tests=src/test/java`
  - `sonar.exclusions=**/target/**,**/db/migration/**,**/Migrations/**`
  - `sonar.java.coveragePlugin=jacoco`
  - `sonar.jacoco.reportPaths=target/jacoco-report/jacoco.xml` (verificar suporte do sonar-scanner para esse formato)
  - `sonar.host.url=http://localhost:9000`
- [ ] `scripts/sonar-scan.sh` (criado na Fase 1):
  - Aceitar `PROJECT_KEY` (default `auth-service-java`) e `PROJECT_NAME` (default `Auth Service Java`)
  - Rodar sonar-scanner CLI (Java nativo, não precisa de `dotnet-sonarscanner`)
  - Antes do scan: rodar `./mvnw clean verify` para gerar `target/jacoco-report/jacoco.xml`
  - Se o sonar-scanner CLI não aceitar JaCoCo XML diretamente, usar `scripts/convert-jacoco-to-generic.py` para gerar `target/jacoco-report/jacoco.generic.xml` no formato `coverage/generic.xml` esperado pelo scanner
- [ ] `make coverage` → 100% em todos os arquivos mantidos

> **Validação:** `./mvnw test` 100% verde. `make coverage` mostra 100% em todos os arquivos mantidos.

---

## Fase 5 · Adaptação do monolito `backend-java-quarkus`

**Commit (no repo `backend-java-quarkus`):** `feat: respect AUTH_MODE=remote to disable local auth routes`

Para o compliance rodar `make test-auth-quarkus`, o monólito precisa, ao receber `AUTH_MODE=remote`, **simplesmente não registrar** `/v1/auth/*`. Nenhuma outra mudança. Middleware, RBAC, models, seed, session continuam idênticos.

### 5.1 — Adicionar config `AUTH_MODE`

- [ ] `backend-java-quarkus/src/main/resources/application.properties`:
  - Adicionar `app.auth.mode=${AUTH_MODE:local}` (default `local` = monólito gerencia)
  - Adicionar `app.auth.service-url=${AUTH_SERVICE_URL:http://localhost:8001}` (URL do auth-service-java, usada em mensagens de log e OpenAPI)
- [ ] `backend-java-quarkus/.env.example` → documentar `AUTH_MODE=local` (default) e `AUTH_MODE=remote` (modo microsserviço)

### 5.2 — Skip de rotas no monólito

- [ ] Criar `backend-java-quarkus/src/main/java/com/app/modules/auth/AuthModeFilter.java` (JAX-RS `ContainerRequestFilter` com `@Provider` e prioridade `PRE_AUTH`):
  - Lê `app.auth.mode` via `@ConfigProperty` com default `local`
  - Se `mode.equals("remote")` e `requestContext.getUriInfo().getPath().startsWith("v1/auth")`, abortar com `404` (mesmo padrão do C# em `Program.cs`)
  - Aplicar **antes** do `AuthFilter` (priority `Priorities.AUTHENTICATION - 100`)
- [ ] `backend-java-quarkus/MageBackend.http` (se aplicável) → adicionar exemplos com `AUTH_MODE=remote` documentando
- [ ] Nenhuma outra mudança em rotas. `/v1/user/*`, `/v1/role/*`, `/v1/feature/*`, `/v1/product/*`, `/v1/dashboard/*` continuam no monólito

### 5.3 — Manter compatibilidade

- [ ] **Não tocar** em:
  - `src/main/java/com/app/infrastructure/auth/AuthFilter.java` (continua validando JWT — aceita tokens do service, mesmo `JWTSecret`)
  - `src/main/java/com/app/infrastructure/auth/JwtService.java` (mesma assinatura HS256, mesmos claims — service reaproveita 100%)
  - `src/main/java/com/app/infrastructure/auth/PermissionFilter.java`, `SecurityDynamicFeature.java`, `UserSession.java`
  - `src/main/java/com/app/modules/auth/*` (módulo continua existindo; só deixa de ser roteado quando `remote`)
  - `src/main/java/com/app/infrastructure/seed/DatabaseBootstrap.java` (seed mínimo é o mesmo)
  - `src/main/resources/db/migration/` (Flyway roda no service e no monólito — a tabela `Auth` é a **fonte da verdade** dos hashes; ambos compartilham o mesmo Postgres)
- [ ] `./mvnw clean compile` no monólito
- [ ] `./mvnw test` no monólito continua passando (sem regressão)
- [ ] `./mvnw verify` no monólito continua com 100% de cobertura (o novo `AuthModeFilter` precisa de teste unitário cobrindo 100% dos branches)

> **Decisão arquitetural importante:** O service compartilha o **mesmo banco** do monólito (`backend_java_quarkus_db`). A tabela `Auth` é a fonte da verdade dos hashes. O service lê/escreve nela diretamente via Hibernate ORM + Flyway — **não há HTTP entre auth-service-java e o banco do User/Auth**. Isso preserva a atomicidade `User + Auth` na criação distribuída (no MVP, o monólito continua criando ambos; o service só lê e atualiza o `Auth.password` em fluxos de reset).

> **Validação:** `AUTH_MODE=remote ./mvnw quarkus:dev -Dquarkus.http.port=8888` no monólito + `./mvnw quarkus:dev -Dquarkus.http.port=8001` no `auth-service-java` → `curl localhost:8001/v1/auth/login` retorna 200, e o token funciona em `curl -H "Authorization: Bearer ..." localhost:8888/v1/auth/me`. `curl localhost:8888/v1/auth/login` retorna **404** (filtro intercepta).

---

## Fase 6 · Bateria completa de validação (rodar **só no final**)

Conforme combinado, nada disso roda durante as fases 1–5 — só após o último commit.

### 6.1 — `auth-service-java`

- [ ] `./mvnw clean compile` + `./mvnw test-compile` limpos
- [ ] `make lint` (`./mvnw compile -Dmaven.compiler.failOnWarning=true`) sem warnings
- [ ] `make test` → 100% verde
- [ ] `make coverage` → 100% em INSTRUCTION e BRANCH (JaCoCo BUNDLE rule)
- [ ] `make dev` → `/q/health/ready` 200, `/health` 200, `/liveness` 200, `/ready` 200, `/v1/auth/login` 200 com admin seed
- [ ] `./mvnw package -DskipTests -Dquarkus.package.jar.type=uber-jar` → jar gerado

### 6.2 — `backend-java-quarkus`

- [ ] `./mvnw clean compile` + `./mvnw test` limpos
- [ ] `make test` em modo `AUTH_MODE=local` → 100% como antes, sem regressão
- [ ] `AUTH_MODE=remote ./mvnw test` no monólito — confirma que os caminhos de skip não quebram nada
- [ ] `make coverage` → 100% como antes (o novo `AuthModeFilter` está coberto)

### 6.3 — Compliance E2E (ambos os modos)

- [ ] Em `mage-backend-compliance`:
  - `cp .env.quarkus .env && make test-quarkus` (modo **monolítico**) → 100% passando
  - `cp .env.auth.quarkus .env && make test-auth-quarkus` (modo **microsserviço**) → 100% passando
- [ ] O `.env.auth.quarkus` já existe no compliance (`BACKEND_TARGET=quarkus`, `DATABASE_URL=jdbc:postgresql://...`, `API_BASE_URL=http://localhost:8888`, `AUTH_SERVICE_URL=http://localhost:8001`, `AUTH_MODE=remote`) — só validar
- [ ] Se algum teste falhar, **voltar e ajustar** — não pular teste

### 6.4 — SonarQube

- [ ] Confirmar `auth-service-java/sonar-project.properties`:
  - `sonar.projectKey=teilorbarcelos_auth-service-java`
  - `sonar.projectName=Auth Service Java`
  - `sonar.sources=src/main/java`
  - `sonar.tests=src/test/java`
  - `sonar.exclusions=**/target/**,**/db/migration/**`
  - `sonar.java.coveragePlugin=jacoco`
  - `sonar.jacoco.reportPaths=target/jacoco-report/jacoco.xml`
  - `sonar.host.url=http://localhost:9000`
- [ ] Confirmar `backend-java-quarkus/sonar-project.properties` (sem mudança de key esperada — só adicionar `sonar.exclusions=**/AuthModeFilter*.java` se necessário, mas o ideal é cobri-lo 100%)
- [ ] Rodar scanner nos dois e validar o quality gate:
  - Cobertura ≥ 100% em `INSTRUCTION` e `BRANCH` (gate do monólito, herdado pelo service)
  - 0 bugs, 0 vulnerabilidades, 0 code smells novos
  - Duplicação < 3%
  - O quality gate custom do projeto deve estar alinhado com o do `backend-java-quarkus` (que já exige 100%)

### 6.5 — CI do GitHub Actions

- [ ] Criar `auth-service-java/.github/workflows/ci.yml` no mesmo padrão do `auth-service-c-sharp/.github/workflows/ci.yml`:
  - Job `build-and-test`:
    - `actions/checkout@v4`
    - `actions/setup-java@v4` com `distribution: temurin`, `java-version: '21'` (mesma versão do Dockerfile e pom.xml)
    - Cache Maven via `actions/cache@v4` (path `~/.m2/repository`, key `${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}`)
    - Subir Postgres + Redis como services (mesma config do `docker-compose.dev.yml` da Fase 2)
    - `mvn -B clean verify` (gera `target/jacoco-report/jacoco.xml`)
    - `python3 scripts/check-coverage.py` (falha se cobertura < 100%)
  - Job `sonar` (só em push para `main`/`master`/`develop`):
    - `needs: build-and-test`
    - Setup Java 21 + cache do SonarScanner
    - Instalar sonar-scanner CLI (Java nativo, não precisa do `dotnet-sonarscanner`)
    - Rodar `./scripts/sonar-scan.sh "auth-service-java" "Auth Service Java"`
- [ ] Adicionar secrets `SONAR_HOST_URL` e `SONAR_TOKEN` no repositório do `auth-service-java` (mesmos do `auth-service-c-sharp` se compartilharem a instância SonarQube, ou específicos deste projeto)
- [ ] Atualizar `backend-java-quarkus/.github/workflows/ci.yml` (se não existir, criar) no mesmo padrão, incluindo o job `sonar` apontando para `backend-java-quarkus/sonar-project.properties`

---

## Fase 7 · Finalização

- [ ] `auth-service-java/README.md` reescrito no padrão do `auth-service-go/README.md` e `auth-service-c-sharp/README.md`:
  - Arquitetura (JDK 21, Quarkus 3.21, Hibernate ORM + Panache, Flyway, SmallRye JWT/JOSE4J, Redis Client, BCrypt, JaCoCo)
  - Modo monólito vs. microsserviço (diagrama de fluxo + como subir o stack combinado)
  - Tabela "O que muda no monólito" (1 `AuthModeFilter` adicionado; middleware, RBAC, session inalterados)
  - Comandos do `make` (install, dev, test, coverage, build, infra-up, lint, sonar)
  - Seção de compliance (E2E em ambos os modos)
- [ ] `backend-java-quarkus/README.md` → seção "🔌 Modo microsserviço (auth-service-java)" explicando:
  - Como subir o `auth-service-java` (porta 8001)
  - Setar `AUTH_MODE=remote` no monólito
  - Diagrama do fluxo (mesmo padrão dos outros auth-service)
  - Tabela "O que muda no monólito" (1 filtro + 2 envs; módulo auth, middleware, RBAC, JwtService inalterados)
- [ ] Tag/release do `auth-service-java` (opcional)
- [ ] (Pós-MVP) considerar mover geração de PDF, email de reset, audit pipeline para cá (separação total de domínios) — não é pré-requisito do plug-and-play

---

## Resumo dos commits previstos

| # | Repo | Mensagem |
|---|------|----------|
| 1 | `auth-service-java` | `chore: rename project → auth-service-java + rebrand containers/CI/sonar` |
| 2 | `auth-service-java` | `feat: trim non-auth surface` |
| 3 | `auth-service-java` | `feat: auth module → microservice` |
| 4 | `auth-service-java` | `test: unit + integration coverage 100%` |
| 5 | `auth-service-java` | `ci: github actions pipeline with coverage + sonar` |
| 6 | `auth-service-java` | `docs: README + env example for plug-and-play` |
| 7 | `backend-java-quarkus` | `feat: respect AUTH_MODE=remote to disable local auth routes` |
| 8 | `backend-java-quarkus` | `ci: github actions pipeline with coverage + sonar` |
| 9 | `backend-java-quarkus` | `docs: README seção modo microsserviço (auth-service-java)` |

> A bateria da Fase 6 (test/coverage/compliance/sonar) é apenas verificação — só vira commit se aparecer fix necessário durante ela.

---

## Princípios respeitados em todas as fases

- **S** Single Responsibility: cada módulo do service tem um papel (auth, jwt, session, rate limit, request id, health, seed).
- **O** Open/Closed: a interface `/v1/auth/*` é estável; mudanças internas não a afetam.
- **L** Liskov: tokens emitidos pelo service são 100% compatíveis com o validador do monólito (mesmo `JWT_SECRET`, mesmo `JwtService`, mesmo `AuthFilter`).
- **I** ISP: o monólito só depende da interface HTTP do service; nada além.
- **D** DIP: o service depende de abstrações (`UserRepository`, `AuthModel`, `JwtService`, `AuthFilter`, `UserSession`).
- **DRY**: reuso integral de `src/main/java/com/app/infrastructure/auth/*`, `src/main/java/com/app/infrastructure/seed/*`, `src/main/java/com/app/core/*`, `src/main/java/com/app/modules/auth/*` (apenas as classes que sobraram) — zero duplicação com o monólito. A refatoração de `buildAuthResponse` para extrair `buildUserPayload` segue o mesmo padrão de `AuthHelper.cs` (já existente) — reuso de forma.
- **Clean Code**: nomes pequenos, sem comentários óbvios, funções coesas, side effects isolados no service, branches reduzidos a um único `return` por caminho (Liskov-friendly).
