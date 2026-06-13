# Auth Service (Java/Quarkus) 🔐

Microsserviço de autenticação dedicado, extraído do `backend-java-quarkus`.
Plug-and-play com o monólito: setar `AUTH_MODE=remote` no monólito e subir este serviço na porta `8001`.

---

## 🚀 Tecnologias

- **Runtime:** JDK 21 (Temurin)
- **Framework:** Quarkus 3.21
- **ORM:** Hibernate ORM + Panache (Repository Pattern)
- **Migrações:** Flyway
- **Cache/Sessão:** Redis (Session Versioning)
- **Autenticação:** JWT HMAC-SHA256 (Jose4j) + BCrypt (jBCrypt)
- **Validação:** Hibernate Validator (Bean Validation)
- **API Docs:** OpenAPI 3.0 / Swagger UI
- **Logs:** JSON estruturado com MDC
- **Qualidade:** JaCoCo (100% instructions & branches)

---

## ✨ Funcionalidades

- **Login / Refresh / Logout** via JWT com refresh token rotation
- **Me** — dados do usuário logado via token
- **Password Reset** — troca de senha autenticada
- **JWKS Endpoint** — `GET /v1/auth/.well-known/jwks.json`
- **RBAC no Redis:** Permissões cacheadas em `session:{user_id}:permissions`
- **Session Epoch:** Invalidação O(1) via `session:ver:%s` (INCR no Redis)
- **Rate Limiting:** Script Lua atômico no Redis
- **Health Checks:** `/health`, `/liveness`, `/ready`
- **Request Logging:** Log estruturado com MDC

---

## 🔌 Plug-and-Play: Monolito → Microsserviço

O `auth-service-java` substitui o módulo de autenticação do `backend-java-quarkus` sem alterar o middleware JWT, o RBAC ou a sessão Redis.

### Como funciona

```
FRONTEND                   AUTH SERVICE (8001)         MONOLITH (8888)
   │                            │                          │
   ├─ POST /login ────────────→│                          │
   │                            ├─ SELECT User+Auth+Role  │
   │                            ├─ BCrypt verify          │
   │                            ├─ Redis: create session  │
   │                            ├─ JWT (HS256)            │
   │←── { token, refresh } ────│                          │
   │                                                      │
   ├─ GET /orders (JWT) ────────────────────────────────→│
   │                                                      │
   │                          ├─ valida JWT local (HS256) │
   │                          ├─ checa session:ver:%s     │
   │                          ├─ RBAC check (bitset)      │
   │←─────────────────────────────────────────────────────│
```

### Modos de operação

#### Modo Monolítico (default)

O `backend-java-quarkus` gerencia tudo — auth incluso. **Nenhuma configuração extra.**

```bash
AUTH_MODE=local    # (default) autenticação no próprio monólito
```

#### Modo Microsserviço (opt-in)

Auth extraído para o `auth-service-java`. O monólito mantém validação JWT + RBAC.

```bash
# backend-java-quarkus/.env
AUTH_MODE=remote   # desliga /v1/auth/* no monólito

# auth-service-java/.env
JWT_SECRET=<mesma do monólito>
DB_HOST=<mesmo do monólito>
REDIS_HOST=<mesmo do monólito>
```

### Passo a passo

```bash
# 1. Configure o auth-service
cd auth-service-java
cp .env.example .env
# Edite .env: mesma DB_HOST, JWT_SECRET e REDIS_HOST do monólito

# 2. Suba a infraestrutura (PostgreSQL + Redis)
make infra-up

# 3. Inicie o servidor (porta 8001)
make dev

# 4. No monólito, ative o modo remoto
# backend-java-quarkus/.env → AUTH_MODE=remote

# 5. Frontend passa a chamar:
#   - POST /v1/auth/login        → auth-service (8001)
#   - POST /v1/auth/refresh      → auth-service (8001)
#   - POST /v1/auth/logout       → auth-service (8001)
#   - Demais endpoints           → monólito (8888)

# 6. Pronto! O JWT emitido pelo auth-service é aceito pelo monólito.
```

### O que muda no monólito

| Componente | Antes (monolito) | Depois (auth-service) |
|---|---|---|
| `POST /v1/auth/login` | Handler local | ❌ Remove |
| `POST /v1/auth/refresh` | Handler local | ❌ Remove |
| `POST /v1/auth/logout` | Handler local | ❌ Remove |
| `POST /v1/auth/me` | Handler local | ❌ Remove |
| Middleware JWT | `ValidateToken(secret)` | ✅ **Igual** |
| Middleware RBAC | Lê bitset de permissões no JWT | ✅ **Igual** |
| Session version | `session:ver:%s` no Redis | ✅ **Igual** |

> **Apenas 4 handlers são removidos.** Todo o resto (middleware, RBAC, Redis) continua inalterado.

---

## 🏁 Começando

### Pré-requisitos

- JDK 21 (Temurin recomendado)
- Docker (PostgreSQL + Redis)
- Maven wrapper (incluso — `./mvnw`)
- `backend-java-quarkus` rodando (para criar as tabelas e dados iniciais)

### Setup

```bash
# 1. Suba infraestrutura
make infra-up
# ou use a mesma infra do monólito (recomendado)

# 2. Configure o ambiente
cp .env.example .env
# Edite DB_HOST, JWT_SECRET e REDIS_HOST (mesmos do monólito)

# 3. Execute os testes
make test

# 4. Inicie o servidor
make dev
```

### Variáveis de ambiente

```bash
# App
APP_ENV=development
APP_URL=http://localhost:8001

# JWT
JWT_SECRET=86941813-8b97-4cad-b0b2-f97734a947d7
JWT_EXPIRATION=3600

# Database (apenas produção — Dev Services cobre dev)
DB_HOST=db
DB_PORT=5432
DB_DATABASE=backend_db
DB_USERNAME=postgres
DB_PASSWORD=postgrespw

# Redis (apenas produção — Dev Services cobre dev)
REDIS_HOST=redis
REDIS_PORT=6379

# Rate Limit
RATE_LIMIT_MAX=60
RATE_LIMIT_WINDOW=60

# CORS
CORS_ORIGINS=http://localhost:3000
```

---

## 📡 Endpoints

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | `/v1/auth/login` | ❌ | Login (email + password) |
| POST | `/v1/auth/refresh` | ❌ | Renova par de tokens |
| POST | `/v1/auth/logout` | ✅ | Revoga sessão |
| GET | `/v1/auth/me` | ✅ | Dados do usuário logado |
| POST | `/v1/auth/password-reset` | ✅ | Troca de senha |
| GET | `/v1/auth/.well-known/jwks.json` | ❌ | JWKS (placeholder RS256) |
| GET | `/health` | ❌ | Health check agregado |
| GET | `/liveness` | ❌ | Liveness probe |
| GET | `/ready` | ❌ | Readiness probe |
| GET | `/v1/docs` | ❌ | Swagger UI |

---

## 🧪 Testes

```bash
# Testes unitários e de integração (requer Redis local)
make test

# Cobertura com verificação (100%)
make coverage

# Linter (warnings como erro)
make lint
```

### Compliance (E2E com monólito)

```bash
cd ../mage-backend-compliance

# Modo monolítico (auth no monólito)
cp .env.java .env
make test-java

# Modo microsserviço (auth no auth-service)
cp .env.auth.java .env
make test-auth-java
```

---

## 📊 Qualidade

- **Cobertura:** 100% em código de produção (instructions + branches)
- **SonarQube:** Quality Gate A (0 bugs, 0 vulnerabilidades, 0 code smells novos)
- **JaCoCo:** trava o CI se abaixo de 80% (instructions + branches)

---

## 🛠️ Comandos do Makefile

| Comando | Descrição |
|---------|-----------|
| `make dev` | Sobe infra + servidor com hot reload (Quarkus Dev Mode) |
| `make test` | Roda todos os testes (requer Redis) |
| `make coverage` | Testes + verificação de cobertura JaCoCo |
| `make build` | Compila uber-jar de produção |
| `make lint` | Compilação com warnings como erro |
| `make infra-up` | Sobe PostgreSQL e Redis |
| `make infra-down` | Para a infraestrutura |
| `make infra-clean` | Remove volumes da infraestrutura |
| `make sonar` | Roda SonarQube scan local |
