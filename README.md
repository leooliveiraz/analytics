# Analytics

Plataforma de analytics (estilo Google Analytics / Plausible), self-hosted e cookieless: coleta eventos
de sites via snippet JS ou REST API, enriquece no servidor e armazena em PostgreSQL, exibindo métricas
em dashboards — incluindo cliques por elemento, tempo por página/seção, imagens mais vistas, mapas de
calor (clique/movimento/scroll) e mapa de acessos por país.

## Sumário

- [Recursos](#recursos)
- [Stack](#stack)
- [Estrutura](#estrutura)
- [Instalação rápida (produção / VPS)](#instalação-rápida-produção--vps)
- [Desenvolvimento local](#desenvolvimento-local)
- [Configuração (variáveis de ambiente)](#configuração-variáveis-de-ambiente)
- [Uso do snippet](#uso-do-snippet)
- [Eventos e schema](#eventos-e-schema)
- [Ingestão via REST](#ingestão-via-rest)
- [API de analytics](#api-de-analytics)
- [GeoIP (mapa por país)](#geoip-mapa-por-país)
- [Privacidade e LGPD](#privacidade-e-lgpd)
- [Arquitetura de dados](#arquitetura-de-dados)
- [Operação (logs, backup, atualização)](#operação-logs-backup-atualização)
- [HTTPS com domínio (Caddy)](#https-com-domínio-caddy)
- [Testes](#testes)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)

## Recursos

- **Coleta**: `pageview`, `pageleave` (tempo/engajamento/scroll), cliques, links externos, downloads,
  visualização de imagens, tempo por seção e `mousemove` — além de eventos customizados.
- **Dashboards**: visão geral, séries temporais, detalhamento por dimensão, tempo real.
- **Exploradores**: páginas (tempo/scroll), elementos clicados, imagens, seções, eventos e sessões.
- **Heatmaps**: clique, movimento e scroll (overlay por porcentagem).
- **Mapa** de acessos por país (com GeoIP opcional).
- **Multi-tenant**: usuários, projetos, membros/papéis e API keys.
- **Cookieless por padrão**: identificação anônima por hash diário (IP + User-Agent + salt).
- **Escala**: `events` particionada por mês, retenção configurável, filtro de bots e rate limit.

## Stack

- **Backend**: Kotlin + Spring Boot 3.3 (Java 21), Spring Security + JWT, JDBC (`NamedParameterJdbcTemplate`)
- **Banco**: PostgreSQL 16, com `events` particionada por mês
- **Migrações**: Liquibase (master XML + SQL puro em `changes/`)
- **Frontend**: React 18 + Vite + TypeScript, TanStack Query, Recharts, react-simple-maps
- **Infra**: Docker Compose (`postgres` + `api` + `web`/nginx)

## Estrutura

```
analytics/
  backend/            # API Spring Boot (Gradle Kotlin DSL)
    src/main/kotlin/com/analytics/
      auth/ user/ project/ apikey/ security/ config/
      ingestion/       # recebimento + enriquecimento
      analytics/       # consultas de métricas
    src/main/resources/
      db/changelog/    # Liquibase (master + changes/*.sql)
      static/js/analytics.js   # snippet de coleta
  frontend/           # React + Vite (nginx serve o SPA + proxy /api,/js)
    nginx.conf
  deploy/             # docker-compose.yml, install.sh, .env.example, geoip/
  README.md
```

## Instalação rápida (produção / VPS)

### Pré-requisitos

- Linux (Ubuntu/Debian recomendado) com **Docker Engine** e o plugin **Docker Compose v2**
  (`docker compose version`).
- Portas livres: **80** (painel) e, se for usar HTTPS, **443**.
- Internet no servidor (build baixa dependências Maven/npm e imagens).
- ~2 GB de RAM mínimo.

### Passos

1. **Copie o projeto** para o servidor (via `git clone` ou `scp`/`rsync` dos diretórios `backend`,
   `frontend`, `deploy` — sem `node_modules`, `dist` e `build`).

2. **Suba a stack** (a partir da pasta `deploy`):

   ```bash
   cd deploy
   WEB_PORT=80 bash install.sh
   ```

   O `install.sh`:
   - valida Docker/Compose;
   - gera `deploy/.env` com **segredos aleatórios** (`JWT_SECRET`, `VISITOR_SALT`, `POSTGRES_PASSWORD`);
   - cria `deploy/geoip/`;
   - faz build e sobe os containers;
   - imprime a URL final.

   Sem a variável, a porta padrão do painel é **80**.

3. **Acesse** `http://SEU_IP` e **crie a primeira conta** (a tela de registro cria o primeiro usuário).

4. **Crie um projeto** e copie a **chave pública `pk_...`** em *Configurações*.

5. **Instale o snippet** no seu site (veja [Uso do snippet](#uso-do-snippet)).

> Portas: só o `web` fica público (`0.0.0.0:${WEB_PORT}`). A API e o Postgres escutam em
> `127.0.0.1` (não ficam expostos à internet).

### Modo manual (sem `install.sh`)

```bash
cd deploy
cp .env.example .env      # edite os segredos!
docker compose up -d --build
```

## Desenvolvimento local

Pré-requisitos: **JDK 21** (`JAVA_HOME` apontando para ele), **Node.js 20+** e **Docker**.
O Gradle é usado via wrapper (`gradlew.bat`/`./gradlew`), não precisa instalar.

### Stack completa (Docker)

```bash
cd deploy
cp .env.example .env
docker compose up -d --build
```

- Painel: `http://localhost:5180`
- API: `http://localhost:8080` · health: `http://localhost:8080/actuator/health`

### Rodando API e frontend fora do Docker

```bash
# 1) banco
cd deploy && docker compose up -d postgres

# 2) API (com seeds de dev)
cd ../backend
./gradlew bootRun --args="--spring.profiles.active=local"   # no Windows: .\gradlew.bat bootRun --args="--spring.profiles.active=local"

# 3) frontend (proxy /api e /js para :8080)
cd ../frontend
npm install
npm run dev
```

### Build e testes

```bash
cd backend  && ./gradlew build      # no Windows: .\gradlew.bat build
cd frontend && npm run build        # tsc --noEmit + vite build
```

## Configuração (variáveis de ambiente)

Definidas em `deploy/.env` (usadas pelo `docker-compose.yml`). Veja `deploy/.env.example`.

| Variável | Default | Descrição |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` | `analytics` | Nome/usuário do banco |
| `POSTGRES_PASSWORD` | `analytics` | Senha do banco — **troque em produção** |
| `POSTGRES_PORT` / `API_PORT` | `5432` / `8080` | Portas no host (bind em `127.0.0.1`) |
| `WEB_PORT` | `5180` | Porta pública do painel (use `80` no servidor) |
| `JWT_SECRET` | segredo de dev | **Trocar em produção** (mín. 32 bytes) |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | `15m` / `30d` | Validade dos tokens |
| `VISITOR_SALT` | segredo de dev | Salt do `visitor_id` (cookieless) |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | `http://localhost:[*],http://127.0.0.1:[*]` | Origens permitidas p/ ingestão cross-domain. Use `*` para liberar |
| `RETENTION_EVENTS_MONTHS` | `12` | Meses de eventos retidos (`144` = 12 anos) |
| `INGESTION_FILTER_BOTS` | `true` | Descarta eventos de bots |
| `INGESTION_ANONYMIZE_IP` | `false` | Zera o último octeto (IPv4)/últimos 80 bits (IPv6) do IP antes de geolocalizar e hashear |
| `INGESTION_RATE_LIMIT_CAPACITY` / `_WINDOW` | `240` / `1m` | Limite por chave/IP |
| `GEOIP_DB` | `/app/geoip/GeoLite2-City.mmdb` | Caminho do `.mmdb` no container |
| `GEOIPUPDATE_ACCOUNT_ID` / `_LICENSE_KEY` | vazio | Credenciais MaxMind p/ atualização automática |
| `GEOIPUPDATE_EDITION_IDS` | `GeoLite2-City` | Base a baixar (`GeoLite2-Country` é mais leve) |
| `GEOIPUPDATE_FREQUENCY` | `24` | Horas entre atualizações (perfil `geoip`) |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | — | Usadas pela API fora do compose |
| `LIQUIBASE_CONTEXTS` | `prod` | Use `dev` para incluir seeds |
| `SERVER_PORT` | `8080` | Porta interna da API |

## Uso do snippet

Cole antes de `</head>`:

```html
<script defer src="http://SEU_HOST/js/analytics.js"
        data-key="pk_xxxxxxxx"
        data-outbound="true"
        data-download="true"
        data-images="true"
        data-sections="true"
        data-scroll="true"></script>
```

### Atributos de configuração

| Atributo | Default | Efeito |
|---|---|---|
| `data-key` | — | **Obrigatório**. Chave pública (`pk_...`) |
| `data-endpoint` | `<origem do script>/api/v1/events` | Endpoint de ingestão |
| `data-auto-click` | `false` | `true` rastreia **todos** os cliques (não só `data-analytics`) |
| `data-outbound` | `false` | Rastreia cliques em links para outros domínios |
| `data-download` | `false` | Rastreia cliques em downloads (extensões + atributo `download`) |
| `data-images` | `false` | Rastreia imagens vistas (`IntersectionObserver`) |
| `data-sections` | `false` | Mede tempo visível por seção |
| `data-scroll` | `true` | Envia profundidade de scroll no `pageleave` |
| `data-heatmap-move` | `false` | Amostra `mousemove` para heatmap de movimento (mais volume) |
| `data-consent` | `false` | Se `true`, só coleta após `window.analytics.consent(true)` |
| `data-sample` | `1` | Fração de sessões amostradas (0–1) |

### Eventos automáticos

- `pageview` — no load e em navegação SPA (`pushState`/`popstate`).
- `pageleave` — ao sair/ocultar a aba; inclui `durationMs`, `engagedMs`, `scrollPct`.
- `click` — clique em `[data-analytics]` (ou em tudo, se `data-auto-click="true"`).
- `outbound_link`, `file_download` — conforme configuração.
- `image_view` — imagem ≥50% visível por ≥1s; inclui `imageKey`, `imageAlt`, `dwellMs`.
- `section_engage` — tempo visível por seção; inclui `sectionKey`, `durationMs`, `scrollPct`.
- `mousemove` — pontos amostrados para heatmap (se habilitado).

### Marcação no HTML

```html
<button data-analytics="signup">Assinar</button>
<a data-analytics href="/precos">Ver preços</a>
<section data-analytics-section="hero">...</section>
```

- `data-analytics="signup"` gera o evento **`signup`**; `data-analytics` vazio gera **`click`**.
- `data-analytics-section="hero"` associa o tempo de permanência à chave `hero`.

### Eventos customizados e consentimento

```js
window.analytics.track("purchase", { plan: "pro", value: 199 });
window.analytics.pageview();
window.analytics.flush();           // envia imediatamente
window.analytics.consent(true);     // inicia a coleta se data-consent="true"
```

## Eventos e schema

Cada evento aceita os campos abaixo (todos opcionais exceto onde indicado). O servidor deriva/enriquece
`path`, `referrer_domain`, `utm_*`, `browser`, `os`, `device_type` e `country/region/city`.

| Campo | Tipo | Descrição |
|---|---|---|
| `name` | string | Nome do evento (default `pageview`) |
| `url`, `referrer` | string | URL atual e referrer |
| `screenWidth`, `screenHeight`, `language` | int/string | Contexto do dispositivo |
| `visitorId`, `sessionId` | string/uuid | Identificadores anônimos |
| `occurredAt` | ISO instant | Quando ocorreu (default: agora) |
| `elementTag`, `elementSelector`, `elementText`, `elementId`, `href` | string | Contexto do elemento clicado |
| `clickX`, `clickY`, `clickXPct`, `clickYPct` | int/number | Coordenadas do clique |
| `viewportWidth`, `viewportHeight`, `pageHeight` | int | Dimensões da página/viewport |
| `durationMs`, `engagedMs`, `scrollPct` | int | Tempo e scroll (`pageleave`/`section_engage`) |
| `imageKey`, `imageAlt`, `dwellMs` | string/int | Contexto de imagem |
| `sectionKey` | string | Seção observada |
| `properties` | objeto | Dados livres do evento |

## Ingestão via REST

```http
POST /api/v1/events
X-Api-Key: pk_xxxxxxxx        # chave pública (snippet) ou ak_... (server-side)
Content-Type: application/json

{
  "events": [
    {
      "name": "purchase",
      "url": "https://site.com/precos?utm_source=news",
      "referrer": "https://google.com",
      "visitorId": "v1",
      "sessionId": "uuid",
      "occurredAt": "2026-09-25T12:00:00Z",
      "properties": { "plan": "pro", "value": 199 }
    }
  ]
}
```

- A chave também pode ir via `?k=pk_...` ou header `X-Project-Key`.
- Máximo **100 eventos** por requisição; responde **`202`** com `{ "accepted": n, "dropped": n }`.
- IP real lido de `X-Forwarded-For`/`X-Real-IP` (usado para GeoIP e rate limit).
- Filtro de bots e rate limiting por chave/IP.

## API de analytics

Base: `/api/v1/projects/{projectId}` (requer `Authorization: Bearer <token>`; papel mínimo `VIEWER`).

| Rota | Params | Retorno |
|---|---|---|
| `/overview` | `from`, `to` (YYYY-MM-DD) | visitantes, pageviews, sessões, bounce, duração |
| `/stats` | `from`, `to`, `interval=day\|hour\|minute` | série temporal |
| `/breakdown` | `from`, `to`, `dimension`, `limit` | top valores por dimensão |
| `/realtime` | `minutes` | visitantes ativos + série por minuto |
| `/pages` | `from`, `to`, `limit` | páginas: pageviews, visitantes, entradas, tempo médio, scroll médio |
| `/elements` | `from`, `to`, `path?`, `limit` | elementos mais clicados |
| `/images` | `from`, `to`, `path?`, `limit` | imagens mais vistas + tempo médio |
| `/sections` | `from`, `to`, `path?`, `limit` | tempo médio por seção |
| `/heatmap` | `from`, `to`, `type=click\|move\|scroll`, `path?`, `device?`, `limit` | pontos `{x, y, weight}` |
| `/geo` | `from`, `to` | `{country, visitors, pageviews}` |
| `/events` | `from?`, `to?`, `eventName?`, `path?`, `page`, `size` | eventos paginados |
| `/sessions` | `from?`, `to?`, `page`, `size` | sessões paginadas |
| `/sessions/{sessionId}` | — | sessão + linha do tempo de eventos |

Dimensões de `/breakdown`: `path`, `referrer`, `country`, `device`, `browser`, `os`, `utm_source`, `event`.

### Autenticação (`/api/v1/auth`)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/register`, `/login`, `/refresh` | Emite/rotaciona tokens |
| POST | `/logout` | Revoga refresh tokens (auth) |
| GET | `/me` | Usuário atual (auth) |

### Projetos, membros e API keys

| Método | Rota | Papel mínimo |
|---|---|---|
| GET/POST | `/api/v1/projects` | autenticado |
| GET/PATCH/DELETE | `/api/v1/projects/{id}` | VIEWER / ADMIN / OWNER |
| GET/POST/PATCH/DELETE | `/api/v1/projects/{id}/members` | VIEWER / ADMIN / OWNER |
| GET/POST/DELETE | `/api/v1/projects/{id}/api-keys` | VIEWER / ADMIN / ADMIN |

Papéis: `OWNER` > `ADMIN` > `VIEWER`. A chave secreta (`ak_...`) só é exibida na criação; apenas o
hash SHA-256 é persistido.

## GeoIP (mapa por país)

A geolocalização é **opcional** e precisa da base `.mmdb` da MaxMind (GeoLite2) — a biblioteca vem no
projeto, mas o arquivo de dados **não**. Sem ele, `country/region/city` ficam vazios e o mapa não popula.
IPs locais/privados são ignorados de propósito.

### Sem conta MaxMind (DB-IP Lite — mais simples)

Base gratuita em formato `.mmdb`, **sem cadastro**:

```bash
bash deploy/geoip/update-dbip.sh        # baixa o mês atual para deploy/geoip/dbip-city-lite.mmdb
```

Depois aponte no `deploy/.env` e reinicie a API:

```env
GEOIP_DB=/app/geoip/dbip-city-lite.mmdb
```

```bash
cd deploy && docker compose up -d api
```

Para manter atualizado, rode o script mensalmente (ex.: um cron).

### Baixar manualmente (MaxMind)

1. Crie uma conta/licença em [maxmind.com](https://www.maxmind.com) e gere uma *license key*.
2. Coloque o arquivo em `deploy/geoip/`:
   - `GeoLite2-City.mmdb` (país + região + cidade) **ou**
   - `GeoLite2-Country.mmdb` (só país). Ajuste `GEOIP_DB=/app/geoip/GeoLite2-Country.mmdb`.
3. `docker compose up -d` (a API recarrega o banco sozinha, inclusive quando o arquivo muda).

### Baixar/atualizar automaticamente (`geoipupdate`)

Preencha `deploy/.env`:

```env
GEOIPUPDATE_ACCOUNT_ID=123456
GEOIPUPDATE_LICENSE_KEY=xxxxxxxx
GEOIPUPDATE_EDITION_IDS=GeoLite2-City
GEOIPUPDATE_FREQUENCY=24
```

Então, escolha uma opção:

```bash
# (A) uma vez, agora
bash deploy/geoip/update.sh          # no Windows: .\deploy\geoip\update.ps1

# (B) manter atualizado sozinho (container)
cd deploy && docker compose --profile geoip up -d
```

## Privacidade e LGPD

Este projeto é **self-hosted**: os dados ficam no seu servidor e não são compartilhados com terceiros.
Ainda assim, você é o **controlador** e o LGPD se aplica porque há tratamento de **dados pessoais**
(o IP é dado pessoal no Brasil), mesmo com `visitor_id` pseudônimo.

**O que é coletado**: IP (usado para geolocalização e para o hash do visitante), User-Agent, idioma,
tela, URL/referrer/UTM e dados de interação (cliques, seções, scroll, imagens). No navegador ficam apenas
`an_visitor`/`an_session` no `localStorage` (identificadores aleatórios, não cookies).

**Responsabilidades mínimas**:

- Ter uma **base legal** (art. 7) — normalmente **legítimo interesse** para analytics próprio, ou
  **consentimento** se houver perfilamento/heatmap detalhado.
- Publicar uma **Política de Privacidade** informando finalidade, dados, base legal, retenção e direitos.
- Definir **retenção** (`RETENTION_EVENTS_MONTHS`) coerente com o que a política declara.
- Garantir **segurança** (use HTTPS em produção).
- Atender aos **direitos do titular** (art. 18).

**Minimização (recomendado)**:

- `INGESTION_ANONYMIZE_IP=true` — não usa o IP exato (zera o último octeto / últimos 80 bits); o país
  continua correto, cidade/região podem degradar.
- Deixe `GEOIP_DB` vazio se não precisar de mapa.
- Não habilite `data-heatmap-move` nem `data-auto-click` sem necessidade.

**Consentimento (opcional)**: o snippet pausa a coleta até o aceite.

```html
<script defer src="https://SEU_HOST/js/analytics.js"
        data-key="pk_..."
        data-consent="true"></script>

<!-- no seu banner de cookies/privacidade -->
<button type="button" onclick="window.analytics.consent(true)">Aceitar</button>
```

> Este texto é orientação técnica e **não constitui aconselhamento jurídico**. Valide com um advogado/DPO.

## Arquitetura de dados

- **`events`** — particionada por RANGE em `occurred_at` (mensal). Partições são criadas
  automaticamente (mês atual + 2 à frente) e removidas pela retenção (`RETENTION_EVENTS_MONTHS`).
  Guarda colunas de contexto (elemento, coordenadas, tempo, scroll, imagem, seção) e `properties` JSONB.
- **`sessions`** — agregadas por sessão via `UPSERT` na ingestão (`duration_seconds`, `pageviews`,
  `is_bounce`, `entry_path`/`exit_path`, UTM, geo, device).
- **Rollups diários** (`project_daily_stats`, `dimension_daily_stats`) existem no schema; as consultas
  atuais leem direto de `events`/`sessions` (exato e suficiente no volume previsto). A troca para leitura
  de rollups é otimização futura.
- **Cookieless**: `visitor_id` = hash diário de `IP + User-Agent + salt`.

## Operação (logs, backup, atualização)

```bash
cd deploy

# status
docker compose ps

# logs ao vivo
docker compose logs -f api

# atualizar código (após reenviar o projeto) e reiniciar
docker compose up -d --build

# parar tudo (mantém os dados)
docker compose down

# backup do banco
docker compose exec -T postgres pg_dump -U analytics analytics | gzip > backup-$(date +%F).sql.gz

# restaurar
gunzip -c backup.sql.gz | docker compose exec -T postgres psql -U analytics -d analytics
```

> Os dados vivem no volume `analytics_postgres_data`. `docker compose down -v` **apaga tudo**.

## HTTPS com domínio (Caddy)

Sem domínio, o painel fica em `http://IP` e o login trafega em HTTP. Com um subdomínio apontando para o
servidor (ex.: `analytics.seudominio.com`), adicione um proxy Caddy com TLS automático:

```caddyfile
analytics.seudominio.com {
    reverse_proxy 127.0.0.1:80
}
```

```bash
docker run -d --name caddy --restart unless-stopped \
  --network host \
  -v $PWD/Caddyfile:/etc/caddy/Caddyfile:ro \
  -v caddy_data:/data -v caddy_config:/config \
  caddy:2
```

Depois, ajuste `CORS_ALLOWED_ORIGIN_PATTERNS` para o domínio (ou `*`) e reimplante o snippet com HTTPS.

## Testes

- **Backend**: JUnit 5 + Spring Boot Test + Testcontainers (Postgres) —
  `AuthIntegrationTest`, `IngestionAnalyticsIntegrationTest`.
- **Frontend**: `tsc --noEmit` + `vite build`.
- Sem Docker disponível, os testes com Testcontainers são ignorados automaticamente.

## Troubleshooting

| Sintoma | Causa provável / solução |
|---|---|
| `password authentication failed for user "analytics"` | Senha mudou depois do volume criado. Recrie o volume (`docker compose down -v`) ou use a senha original |
| Preflight/CORS falha (`Access-Control-Allow-Credentials`) | Origem não está em `CORS_ALLOWED_ORIGIN_PATTERNS`. Ajuste e reinicie a API |
| Evento de clique não aparece | Elemento sem `data-analytics` (padrão) — ou use `data-auto-click="true"` |
| Download/outbound não aparecem | Habilite `data-download` / `data-outbound` |
| Imagens/seções vazias | Habilite `data-images` / `data-sections` na página |
| Mapa vazio | GeoIP desligado (sem `.mmdb`) ou IP privado/loopback |
| Heatmap sem fundo | Site bloqueia iframe (`X-Frame-Options`/CSP); os pontos ainda aparecem |
| `npm ci` falha no build | Instabilidade de rede; rode `docker compose build web` novamente |

## Roadmap

1. **Fase 1 — Fundação** ✅ monorepo, Spring Boot, Liquibase, Docker Compose
2. **Fase 2 — Auth + Tenancy** ✅ usuários, JWT, projetos, membros, API keys
3. **Fase 3 — Ingestão** ✅ eventos particionados, REST, enriquecimento, snippet JS, rate limit
4. **Fase 4 — Analytics** ✅ overview, stats, breakdown, realtime, exploradores
5. **Fase 5 — Dashboards UI** ✅ React + Vite (login, projetos, dashboard, eventos, settings)
6. **Fase 6 — Hardening** ✅ retenção/partições, testes, Docker web, docs
7. **Fase 7 — Coleta detalhada** ✅ cliques, tempo/seções, imagens, scroll, heatmaps, mapa de países
8. **Fase 8 — Deploy** ✅ instalador, secrets, VPS, GeoIP automático

### Próximos passos sugeridos

- HTTPS por padrão (Caddy/Traefik) + domínio.
- Leitura via rollups + job de refresh para escala.
- Funis, retenção de usuários, alertas e export CSV.
- Gravação de sessão (replay) e Web Vitals.
- 2FA, verificação de e-mail e recuperação de senha.
