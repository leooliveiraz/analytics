# Analytics

Plataforma de analytics (estilo Google Analytics / Plausible): coleta eventos de sites via snippet JS
ou REST API, enriquece e armazena em PostgreSQL, e exibe métricas em dashboards.

## Stack

- **Backend**: Kotlin + Spring Boot 3.3 (Java 21), Spring Security + JWT
- **Banco**: PostgreSQL 16, com `events` particionada por mês
- **Migrações**: Liquibase (master + SQL puro em `changes/`)
- **Frontend**: React 18 + Vite + TypeScript, TanStack Query, Recharts
- **Infra**: Docker Compose (postgres + api + web/nginx)

## Estrutura

```
analytics/
  backend/     # API Spring Boot (Gradle Kotlin DSL)
  frontend/    # React + Vite
  deploy/      # docker-compose.yml, .env.example
```

## Pré-requisitos

- **JDK 21** (`JAVA_HOME` apontando para ele)
- **Docker Desktop** (para Postgres, compose e testes com Testcontainers)
- Node.js 20+ (frontend)

Gradle não precisa ser instalado: use o wrapper (`gradlew.bat`).

## Configuração

| Variável | Default | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/analytics` | Conexão JDBC |
| `DB_USER` / `DB_PASSWORD` | `analytics` / `analytics` | Credenciais do banco |
| `JWT_SECRET` | segredo de dev | **Trocar em produção** (mín. 32 bytes) |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | `15m` / `30d` | Validade dos tokens |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5180` | Origens permitidas |
| `LIQUIBASE_CONTEXTS` | `prod` | Use `dev` para incluir seeds |
| `VISITOR_SALT` | dev | Salt do visitor_id (cookieless) |
| `INGESTION_FILTER_BOTS` | `true` | Descarta eventos de bots |
| `INGESTION_RATE_LIMIT_CAPACITY` / `_WINDOW` | `240` / `1m` | Limite por chave/IP |
| `GEOIP_DB` | vazio | Caminho do `GeoLite2-City.mmdb` (opcional) |
| `RETENTION_EVENTS_MONTHS` | `12` | Meses de eventos retidos |
| `SERVER_PORT` | `8080` | Porta da API |

## Como rodar

### Stack completa (Docker Compose)

```powershell
Copy-Item deploy\.env.example deploy\.env
docker compose -f deploy\docker-compose.yml --env-file deploy\.env up --build
```

- Frontend: `http://localhost:3000`
- API: `http://localhost:8080` · health: `/actuator/health`

### Desenvolvimento local

```powershell
# banco
docker compose -f deploy\docker-compose.yml --env-file deploy\.env up -d postgres

# API (com seeds de dev)
cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"

# frontend (proxy para a API em :8080)
cd frontend
npm install
npm run dev
```

### Build e testes

```powershell
cd backend;  .\gradlew.bat build
cd frontend; npm run build
```

Sem Docker, os testes com Testcontainers são ignorados automaticamente.

### Alternativa sem Docker Desktop (Postgres via WSL)

Se o Docker estiver disponível apenas dentro do WSL, suba o banco por lá e aponte a API para a porta exposta:

```powershell
# dentro do WSL (sem sudo, se o usuário estiver no grupo docker)
wsl -d Ubuntu-24.04 -- docker run -d --name analytics-postgres `
  -e POSTGRES_DB=analytics -e POSTGRES_USER=analytics -e POSTGRES_PASSWORD=analytics `
  -p 5434:5432 postgres:16-alpine

# API no Windows apontando para o banco do WSL
$env:DB_URL='jdbc:postgresql://localhost:5434/analytics'
$env:DB_USER='analytics'; $env:DB_PASSWORD='analytics'
$env:SPRING_PROFILES_ACTIVE='local'
cd backend; .\gradlew.bat bootRun
```

## Ingestão de eventos

### Snippet (web)

```html
<script defer src="https://sua-api/js/analytics.js"
        data-key="pk_xxxxxxxx"></script>
```

- Envia `pageview` no load e em navegação SPA (hook em `pushState`/`popstate`).
- Eventos customizados: `window.analytics.track("signup", { plan: "pro" })`.
- `session_id` e `visitor_id` anônimos em `localStorage` (sessão expira em 30 min).
- Fallback `sendBeacon` → `fetch keepalive`.

### REST

```http
POST /api/v1/events
X-Api-Key: pk_xxxxxxxx        # chave pública (snippet) ou ak_... (server-side)

{
  "events": [
    {
      "name": "pageview",
      "url": "https://site.com/precos?utm_source=news",
      "referrer": "https://google.com",
      "visitorId": "v1",
      "sessionId": "uuid",
      "occurredAt": "2026-09-25T12:00:00Z",
      "properties": { "plan": "pro" }
    }
  ]
}
```

- Aceita no máximo 100 eventos por requisição; responde `202` com `{ accepted, dropped }`.
- Enriquecimento no servidor: User-Agent (browser/OS/dispositivo/bot), referrer, UTM,
  GeoIP (se `GEOIP_DB` configurado) e `visitor_id` cookieless (hash diário IP+UA+salt).
- Filtro de bots e rate limiting por chave/IP.

## API

### Autenticação (`/api/v1/auth`)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/register` · `/login` · `/refresh` | Emite/rotaciona tokens |
| POST | `/logout` | Revoga refresh tokens (auth) |
| GET | `/me` | Usuário atual (auth) |

### Projetos, membros e API keys

| Método | Rota | Papel mínimo |
|---|---|---|
| GET/POST | `/api/v1/projects` | autenticado |
| GET/PATCH/DELETE | `/api/v1/projects/{id}` | VIEWER / ADMIN / OWNER |
| GET/POST/PATCH/DELETE | `/api/v1/projects/{id}/members` | VIEWER / ADMIN / OWNER |
| GET/POST/DELETE | `/api/v1/projects/{id}/api-keys` | VIEWER / ADMIN / ADMIN |

Papéis: `OWNER` > `ADMIN` > `VIEWER`. A chave secreta (`ak_...`) só é exibida na criação;
apenas o hash SHA-256 é persistido.

### Analytics (`/api/v1/projects/{id}`)

| Rota | Params | Retorno |
|---|---|---|
| `/overview` | `from`, `to` (YYYY-MM-DD) | visitantes, pageviews, sessões, bounce, duração |
| `/stats` | `from`, `to`, `interval=day\|hour\|minute` | série temporal |
| `/breakdown` | `from`, `to`, `dimension`, `limit` | top valores por dimensão |
| `/realtime` | `minutes` | visitantes ativos + série por minuto |
| `/events` | `from?`, `to?`, `eventName?`, `path?`, `page`, `size` | eventos paginados |
| `/sessions` | `from?`, `to?`, `page`, `size` | sessões paginadas |

Dimensões: `path`, `referrer`, `country`, `device`, `browser`, `os`, `utm_source`, `event`.
As consultas respeitam o fuso do projeto e usam as partições mensais de `events`.

## Arquitetura de dados

- **`events`** é particionada por RANGE em `occurred_at` (mensal). As partições são criadas
  automaticamente (mês atual + 2 à frente) e removidas pela retenção (`RETENTION_EVENTS_MONTHS`).
- **`sessions`** agregam por sessão via `UPSERT` no momento da ingestão.
- **Rollups diários** (`project_daily_stats`, `dimension_daily_stats`) já existem no schema.
  As consultas atuais leem direto de `events`/`sessions`, o que é exato e suficiente no volume
  previsto (< 1M eventos/mês); a troca para leitura de rollups é uma otimização futura.
- **Cookieless por padrão**: `visitor_id` é hash diário de IP + User-Agent + salt do projeto.

## Testes

- **Backend**: JUnit 5 + Spring Boot Test + Testcontainers (Postgres) —
  `AuthIntegrationTest`, `IngestionAnalyticsIntegrationTest`.
- **Frontend**: build com `tsc --noEmit` + Vite.

## Roadmap

1. **Fase 1 — Fundação** ✅ monorepo, Spring Boot, Liquibase, Docker Compose
2. **Fase 2 — Auth + Tenancy** ✅ usuários, JWT, projetos, membros, API keys
3. **Fase 3 — Ingestão** ✅ eventos particionados, REST, enriquecimento, snippet JS, rate limit
4. **Fase 4 — Analytics** ✅ overview, stats, breakdown, realtime, exploradores
5. **Fase 5 — Dashboards UI** ✅ React + Vite (login, projetos, dashboard, eventos, settings)
6. **Fase 6 — Hardening** ✅ retenção/partições, testes, Docker web, docs

### Próximos passos sugeridos

- Leitura via rollups + job de refresh para escala.
- Metas/funisfeis, alertas e export CSV.
- 2FA, verificação de e-mail e recuperação de senha.
