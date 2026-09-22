# ISP Support Assistant

Backend Java para apoiar analistas N1/N2 de provedores de internet. A API recebe uma pergunta técnica, busca artigos relevantes no PostgreSQL e envia esse contexto ao Gemini para produzir uma orientação em português brasileiro.

O assistente é uma ferramenta de apoio. Ele não executa testes, não acessa equipamentos e não substitui a validação do analista.

## Pré-requisitos

- Java 21
- Docker Desktop com Docker Compose
- Uma chave da Gemini Developer API
- PowerShell, terminal Bash ou equivalente

O projeto inclui o Maven Wrapper; não é necessário instalar Maven globalmente.

## Execução local

1. Crie o arquivo local de configuração:

```powershell
Copy-Item .env.example .env
```

2. Edite `.env` e preencha, no mínimo:

```dotenv
GEMINI_API_KEY=sua-chave-aqui
```

Não versione o arquivo `.env`. Para usar um modelo diferente do padrão definido em `application.yml`, descomente e preencha `GEMINI_MODEL`.

3. Suba o PostgreSQL:

```powershell
docker compose up -d
docker compose ps
```

4. Inicie a aplicação:

```powershell
.\mvnw.cmd spring-boot:run
```

Em Bash/Linux/macOS, use `./mvnw` no lugar de `.\mvnw.cmd`.

O bind padrão é `127.0.0.1:8080`. Se `SERVER_ADDRESS` apontar para um endereço não-loopback, a aplicação só inicia com `APP_API_KEY` definida.

## Uso da API

Endpoint: `POST /api/v1/chat`

Sem `APP_API_KEY` configurada:

```bash
curl -X POST http://127.0.0.1:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Cliente apresenta latência alta em jogos online. O que verificar?"}'
```

Com `APP_API_KEY` configurada:

```bash
curl -X POST http://127.0.0.1:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sua-chave-interna" \
  -d '{"message":"Cliente está com o LED LOS aceso na ONU. O que verificar?"}'
```

Exemplo em PowerShell usando a chave do ambiente:

```powershell
$headers = @{ "X-API-Key" = $env:APP_API_KEY }
$body = @{ message = "Como investigar perda de pacotes?" } | ConvertTo-Json
Invoke-RestMethod -Method Post `
  -Uri "http://127.0.0.1:8080/api/v1/chat" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $body
```

Resposta esperada:

```json
{
  "response": "**Resumo**\n...",
  "provider": "gemini"
}
```

Erros usam `ProblemDetail` e incluem `requestId`. A API pode responder com `400`, `401`, `429`, `500`, `502`, `503` ou `504`, conforme a causa.

## Swagger/OpenAPI

Com a aplicação em execução:

- Swagger UI: <http://127.0.0.1:8080/swagger-ui.html>
- Documento OpenAPI: <http://127.0.0.1:8080/v3/api-docs>

Quando `APP_API_KEY` está definida, o esquema `X-API-Key` aparece na documentação.

Em ambientes públicos, desabilite o Swagger UI e o documento OpenAPI com
`SPRINGDOC_SWAGGER_UI_ENABLED=false` e `SPRINGDOC_API_DOCS_ENABLED=false`.

## Testes

Execute toda a verificação sem consumir cota do Gemini:

```powershell
.\mvnw.cmd clean verify
```

Os testes de repositório e migrations usam PostgreSQL 16 via Testcontainers e são pulados automaticamente quando o Docker não está disponível.

O teste real do Gemini possui a tag `live-ai`, fica desabilitado por padrão e só executa com autorização explícita:

```powershell
$env:RUN_LIVE_AI_TESTS = "true"
$env:GEMINI_API_KEY = "sua-chave-aqui"
.\mvnw.cmd -Dtest=GeminiLiveAiTest test
```

Esse comando consome cota real. Remova as variáveis da sessão ao terminar:

```powershell
Remove-Item Env:RUN_LIVE_AI_TESTS
Remove-Item Env:GEMINI_API_KEY
```

## Variáveis de ambiente

| Variável | Padrão | Uso |
| --- | --- | --- |
| `GEMINI_API_KEY` | sem padrão | Credencial da Gemini Developer API |
| `GEMINI_MODEL` | definido em `application.yml` | Modelo Gemini utilizado |
| `DB_URL` | `jdbc:postgresql://localhost:5432/isp_assistant` | URL JDBC |
| `DB_USER` | `isp_assistant` | Usuário do PostgreSQL |
| `DB_PASSWORD` | `isp_assistant_dev` | Senha local do PostgreSQL |
| `DB_PORT` | `5432` | Porta publicada pelo Docker Compose |
| `POSTGRES_DB` | `isp_assistant` | Banco criado pelo container |
| `APP_AI_PROVIDER` | `gemini` | Rótulo devolvido no campo `provider` |
| `APP_AI_MAX_OUTPUT_TOKENS` | `1024` | Limite de tokens de saída |
| `APP_AI_TIMEOUT_SECONDS` | `30` | Timeout por chamada ao provedor |
| `APP_CHAT_MAX_MESSAGE_LENGTH` | `2000` | Tamanho máximo da pergunta |
| `APP_API_KEY` | vazio | Proteção opcional do endpoint |
| `APP_RATE_LIMIT_PER_MINUTE` | `20` | Requisições por minuto por chave/IP |
| `APP_KNOWLEDGE_MAX_ARTICLES` | `3` | Máximo de artigos no contexto |
| `APP_KNOWLEDGE_MAX_CHARS` | `6000` | Máximo de caracteres do contexto |
| `SERVER_ADDRESS` | `127.0.0.1` | Endereço de bind HTTP |
| `PORT` | `8080` | Porta HTTP; preenchida automaticamente pelo Railway |
| `SPRINGDOC_API_DOCS_ENABLED` | `true` | Habilita o documento OpenAPI; use `false` em produção |
| `SPRINGDOC_SWAGGER_UI_ENABLED` | `true` | Habilita o Swagger UI; use `false` em produção |

## Segurança de credenciais

- Nunca coloque chaves, senhas, URLs privadas ou tokens no Git, README, logs, issues ou screenshots.
- O `.env`, arquivos `.env.*`, chaves privadas e configurações locais sensíveis são ignorados pelo Git; apenas `.env.example` deve ser versionado.
- Configure os valores reais diretamente no ambiente local ou no painel de variáveis do provedor.
- Se uma credencial for publicada por engano, revogue e substitua imediatamente; removê-la apenas do commit mais recente não elimina o valor do histórico Git.

## Arquitetura

```text
POST /api/v1/chat
  -> RequestIdFilter / ApiKeyFilter / RateLimitFilter
  -> ChatController
  -> ChatService
       -> KnowledgeService -> PostgreSQL full-text search
       -> TechnicalAssistant -> Spring AI -> Gemini
```

- `controller/`: contrato HTTP e validação dos DTOs.
- `service/`: validação e orquestração do chat; busca e limitação do contexto.
- `knowledge/`: entidade JPA e consulta full-text parametrizada.
- `ai/`: integração exclusiva com Spring AI e tradução de falhas do provedor.
- `security/` e `web/`: API key, rate limit e correlação por `requestId`.
- `exception/`: respostas `ProblemDetail` sem detalhes internos.

A busca converte até 15 termos significativos da pergunta em uma consulta OR para `websearch_to_tsquery('portuguese', ...)`, ordenada por `ts_rank`. São enviados somente os primeiros artigos e caracteres permitidos pela configuração. A pergunta e os artigos têm os delimitadores do prompt neutralizados antes da chamada ao modelo.

O fluxo é stateless. Não há memória de conversa, embeddings, pgvector ativo, ferramentas de diagnóstico, frontend ou integração com equipamentos nesta fase.

## Retry, timeout e consumo

O Spring AI não adiciona retries. A integração permite no máximo uma nova tentativa apenas para falha de conexão ou HTTP `500`, `502`, `503` e `504`. Erros `4xx`, limite `429` e read timeout não são repetidos.

Com o timeout padrão, o pior caso de uma falha elegível para retry é de aproximadamente 60 segundos, acrescido do atraso curto entre tentativas. Um read timeout é encerrado após aproximadamente 30 segundos e devolvido como `504`.

## Encerramento local

```powershell
docker compose down
```

Para também remover o volume local do banco:

```powershell
docker compose down -v
```

## Deploy no Railway

Crie dois serviços no mesmo projeto Railway: um serviço da aplicação conectado a este repositório e um PostgreSQL gerenciado. Não publique o banco; a aplicação deve usar a rede privada.

No serviço da aplicação, configure estas variáveis:

```dotenv
GEMINI_API_KEY=<defina-no-painel-do-Railway>
APP_API_KEY=<gere-uma-chave-aleatoria-forte>
SERVER_ADDRESS=0.0.0.0
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USER=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
SPRINGDOC_API_DOCS_ENABLED=false
SPRINGDOC_SWAGGER_UI_ENABLED=false
```

`Postgres` deve corresponder exatamente ao nome do serviço de banco no Railway. As demais configurações podem usar os padrões seguros do `application.yml`.

O Railway injeta `PORT` automaticamente. Configure `/actuator/health` como **Healthcheck Path** e gere um domínio público somente para o serviço da aplicação. O Flyway aplica as migrations V1 e V2 na inicialização.
