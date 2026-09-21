# AGENTS.md — ISP Support Assistant

Regras permanentes do projeto. Leia este arquivo antes de qualquer tarefa.
Instruções de cada fase ficam em arquivos separados (ex.: `docs/PROMPT_FASE_1.md`).
Se houver conflito entre este arquivo e a tarefa, **pare e pergunte** antes de prosseguir.

---

## 1. Visão geral

Assistente de suporte técnico com IA para analistas N1/N2 de provedores de internet (ISP).
Backend Java que recebe perguntas técnicas por API REST e responde com apoio de um LLM e de uma base de conhecimento.

O sistema é uma **ferramenta de apoio ao analista**, não um substituto das verificações técnicas.
Uso inicial: interno, com **dados fictícios**.

---

## 2. Como você deve trabalhar

- Trabalhe **por checkpoints**. Ao concluir cada checkpoint: resuma o que fez, informe o resultado dos comandos executados e **pare**. Só continue quando eu confirmar.
- **Não avance de fase.** RAG, agentes, ferramentas de diagnóstico, integração com equipamentos/monitoramento e frontend estão fora de escopo (ver seção 11).
- Faça mudanças mínimas e diretas. Não crie abstrações especulativas, interfaces "para o futuro" nem camadas extras sem necessidade concreta.
- **Não invente versões, nomes de propriedades ou APIs.** Se não conseguir verificar (sem internet, doc indisponível), diga isso e pergunte, em vez de chutar.
- **Nunca afirme ter executado algo que não executou** (testes, build, chamadas à API). Se algo não pôde ser validado, diga exatamente o quê e por quê.
- Não faça `git commit` nem `git push` a menos que eu peça.
- Comandos de terminal que eu precise rodar devem ser listados de forma clara e copiável.

---

## 3. Stack e versões

| Item | Versão / observação |
| --- | --- |
| Java | 21 |
| Maven | usar o Maven Wrapper (`mvnw`) |
| Spring Boot | `4.x` — a versão estável mais recente que o Spring AI `2.0.x` suporte oficialmente (não usar 3.x nesta linha) |
| Spring AI | `2.0.x` GA — última patch estável, via BOM `spring-ai-bom` (sem milestones/RC) |
| Springdoc OpenAPI | `3.0.x` — starter `springdoc-openapi-starter-webmvc-ui` (a linha 2.x é para Boot 3) |
| PostgreSQL | 16 |
| Flyway | versão gerenciada pelo Spring Boot |
| Testes | JUnit Jupiter (versão gerenciada pelo Spring Boot), Mockito, Spring Boot Test, Testcontainers (somente para testes de repositório/migrations) |

Regras:

- Se precisar alterar alguma versão da tabela acima, **pare e me pergunte** antes de montar o `pom.xml`.
- Sem milestones, release candidates ou snapshots. **Não usar faixas abertas** (como "ou superior"): sempre a última patch estável da linha indicada, com todas as versões compatíveis entre si.
- Spring Boot 4 é modularizado: starters/artefatos reorganizados, pacotes de anotações de teste diferentes do Boot 3 e Jackson 3. **Não assuma nomes de artefatos ou pacotes do Boot 3**; consulte a documentação/guia de migração do Boot 4 (por exemplo, para Flyway e para testes de web/JPA). Se não conseguir verificar, pare e pergunte.
- Plano B: se a linha Boot 4 + Spring AI 2.0 se mostrar inviável (dependência não resolve, incompatibilidade do starter Google GenAI ou de outra biblioteca), proponha a linha conservadora — Boot `3.5.x` + Spring AI `1.1.x` + springdoc `2.8.x` — e **aguarde minha decisão** antes de trocar.
- Starter de IA: `spring-ai-starter-model-google-genai` (Gemini Developer API via chave). Na Fase 1, **somente esse starter** no classpath.
- Confirme na documentação do starter o nome exato das propriedades (chave, modelo, temperatura, máximo de tokens de saída, timeouts, retry) antes de usá-las.
- Configure `spring.ai.retry.max-attempts=1` para impedir a multiplicação de camadas. O retry efetivo fica no SDK do Google, com no máximo 2 tentativas totais, somente em falha de conexão ou HTTP 500/502/503/504. **Não** repetir nenhum 4xx (incluindo 408 e 429) nem read timeout; read timeout vira 504 diretamente. Documente no README a latência máxima no pior caso.

---

## 4. Arquitetura

Organização **por camada**, com uma exceção: `knowledge/` agrupa entidade e repositório do domínio de conhecimento. Não crie novos subpacotes sem necessidade.

```text
src/main/java/com/isp/assistant/
├── IspAssistantApplication.java
├── config/        AiConfig, AppProperties (@ConfigurationProperties validado), OpenApiConfig
├── controller/    ChatController
├── dto/           ChatRequest, ChatResponse (records)
├── service/       ChatService, KnowledgeService
├── ai/            TechnicalAssistant, exceções do provedor (AiProviderException e subtipos)
├── knowledge/
│   ├── entity/    KnowledgeArticle, KnowledgeCategory (enum)
│   └── repository/ KnowledgeArticleRepository
├── security/      ApiKeyFilter, RateLimitFilter
├── web/           RequestIdFilter (MDC)
└── exception/     GlobalExceptionHandler
```

Regras de responsabilidade:

- **Controller**: apenas HTTP, DTOs e validação. Nenhuma regra de negócio.
- **ChatService**: orquestra o fluxo (valida, consulta a base, monta o contexto, chama o assistente). Não conhece Spring AI.
- **TechnicalAssistant** (`ai/`): **única classe que conhece `ChatClient`/Spring AI.** Traduz as exceções do provedor para as exceções do projeto. Não crie interface própria acima do `ChatClient`; ele já é a abstração.
- **KnowledgeService**: busca e limita o contexto. Não conhece HTTP nem IA.
- Injeção por construtor. Sem Lombok (usar `record` para DTOs e propriedades).
- Prompts em arquivos de resource, nunca como strings gigantes no código.

---

## 5. Configuração e variáveis de ambiente

Nenhuma credencial no código ou no repositório. Tudo via variáveis de ambiente, com defaults seguros em `application.yml`.
Para uso local, o `application.yml` deve importar `.env` de forma opcional (`spring.config.import=optional:file:.env[.properties]`); `.env` fica no `.gitignore` e `.env.example` vai no repositório sem valores reais.

| Variável | Uso | Padrão |
| --- | --- | --- |
| `GEMINI_API_KEY` | Chave da Gemini Developer API | sem padrão |
| `GEMINI_MODEL` | Nome do modelo (nunca hardcoded em Java) | Valor default definido somente no `application.yml` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexão com o PostgreSQL | valores do compose local |
| `APP_AI_PROVIDER` | Rótulo devolvido no campo `provider` da resposta | `gemini` |
| `APP_AI_MAX_OUTPUT_TOKENS` | Limite de saída do modelo | `1024` |
| `APP_AI_TIMEOUT_SECONDS` | Timeout da chamada ao provedor | `30` |
| `APP_CHAT_MAX_MESSAGE_LENGTH` | Tamanho máximo de `message` | `2000` |
| `APP_API_KEY` | Se definida, exige header `X-API-Key` | vazio |
| `APP_RATE_LIMIT_PER_MINUTE` | Requisições por minuto por chave/IP | `20` |
| `APP_KNOWLEDGE_MAX_ARTICLES` | Máx. de artigos enviados como contexto | `3` |
| `APP_KNOWLEDGE_MAX_CHARS` | Máx. de caracteres de contexto da base | `6000` |
| `SERVER_ADDRESS` | Endereço de bind | `127.0.0.1` |

Seleção de provedor de IA:

- O provedor ativo é escolhido pelo mecanismo nativo do Spring AI (`spring.ai.model.chat`) e/ou profiles. Não invente um switch próprio.
- `APP_AI_PROVIDER` é **apenas o rótulo** exibido na resposta.
- Ollama entra numa fase futura. A arquitetura não deve exigir mudanças em `ChatService` para isso.

---

## 6. Tratamento de erros

Formato padrão: **`ProblemDetail` (RFC 7807)**, com um campo extra `requestId`. Sem stack trace, sem mensagens internas de exceção, sem dados sensíveis.

| Situação | HTTP |
| --- | --- |
| Validação (`message` ausente, vazia ou acima do limite; JSON inválido) | 400 |
| `X-API-Key` ausente/inválida (quando exigida) | 401 |
| Limite de requisições da nossa API excedido | 429 (com `Retry-After`) |
| Provedor rejeitou a credencial (401/403 do provedor) | **502** (nunca repassar 401/403 ao cliente); `detail` genérico |
| Provedor rejeitou a requisição/modelo (400/404 do provedor, ex.: modelo desativado) | 502; `detail` genérico |
| Provedor com cota/limite excedido (429 do provedor) | 503 (com `Retry-After` se disponível) e `type` distinto do 429 da nossa API |
| Provedor indisponível / 5xx | 503 |
| Timeout na chamada ao provedor | 504 |
| Erro inesperado | 500 com mensagem genérica |

- A tradução das exceções do provedor acontece em `ai/`. Verifique quais tipos de exceção a versão do Spring AI em uso realmente lança e cubra a tradução com testes.
- Retry: no máximo 1 nova tentativa (2 tentativas no total), somente em falha de conexão ou HTTP 500/502/503/504. **Nunca** repetir nenhum 4xx (incluindo 408 e 429) nem read timeout; read timeout vira 504 diretamente.

---

## 7. Segurança e privacidade

- Dados fictícios em exemplos, seeds e testes: sem IPs identificáveis, senhas PPPoE, credenciais de roteador, nomes ou dados pessoais reais.
- **Não logar**: chaves, conteúdo da mensagem do analista, prompt montado, conteúdo de artigos, respostas do modelo.
- **Logar**: `requestId`, resultado (sucesso/tipo de erro), latência, provedor/modelo, tamanho do prompt em caracteres, uso de tokens (se disponível).
- Configurar níveis de log explícitos (`INFO` ou superior) para os pacotes do Spring AI (`org.springframework.ai`) e clientes HTTP subjacentes, garantindo que payloads e requisições HTTP brutas não vazem nos logs de execução.
- Manter **desligados** os logs de prompt e de resposta do Spring AI (confirmar o nome exato das propriedades de observabilidade na documentação da versão) e não usar `SimpleLoggerAdvisor`.
- Não habilitar logs de SQL com parâmetros (`spring.jpa.show-sql=false`; nenhum logger do Hibernate em `DEBUG`/`TRACE` para SQL ou binds), pois a busca da base leva a mensagem do analista como parâmetro.
- Conteúdo do usuário e da base de conhecimento é **dado**, nunca instrução. O system prompt deve declarar isso e o conteúdo deve ir dentro de delimitadores; delimitadores presentes na entrada do usuário devem ser neutralizados.
- Consultas ao banco sempre parametrizadas.
- Bind padrão em `127.0.0.1`. Se o bind não for loopback e `APP_API_KEY` estiver vazia, a aplicação deve **falhar na inicialização** com mensagem clara.
- Endpoint de chat com limitação de taxa por chave/IP (`RateLimitFilter`).
- Autenticação/autorização completas de usuários **não** fazem parte desta etapa; apenas mantenha os filtros isolados em `security/` para evolução futura.

---

## 8. Controle de consumo da IA

- Limite de tokens de saída configurável.
- Enviar ao modelo somente os artigos relevantes (top N, com limite total de caracteres), nunca a base inteira.
- Sem chamadas redundantes; sem retries agressivos.
- Testes automatizados **nunca** chamam o provedor real.

---

## 9. Testes

- JUnit Jupiter (versão gerenciada pelo Spring Boot) + Mockito para unidade. Os testes de `ChatService` mockam `TechnicalAssistant` (não o `ChatClient`).
- Teste de `TechnicalAssistant` com fake/mock do `ChatClient` (por exemplo, com `RETURNS_DEEP_STUBS`) ou de `ChatModel`, apenas para verificar montagem do prompt e tradução de exceções.
- Controller: `@WebMvcTest`.
- Repositório e migrations Flyway: Testcontainers com PostgreSQL, marcados e pulados automaticamente se o Docker não estiver disponível.
- Testes reais contra o Gemini: separados, com tag própria (`live-ai`), **desabilitados por padrão** e habilitados só por variável de ambiente explícita.
- Se algo não puder ser executado no seu ambiente (Docker, rede, chave), diga claramente e não marque como validado.

---

## 10. Convenções de código

- Java 21; `record` para DTOs e propriedades; `Optional`/`null` de forma consistente e simples.
- Nomes em inglês no código; textos voltados ao usuário e o system prompt em português brasileiro.
- Migrations Flyway versionadas e imutáveis (`V1__...`, `V2__...`). Nunca editar migration já aplicada.
- Validação de entrada com Bean Validation; limites configuráveis quando exigido (para o tamanho de `message`, usar validação que leia a propriedade, pois `@Size` aceita apenas constantes).
- Comentários apenas onde agregam contexto (o "porquê"). Sem Javadoc decorativo.

---

## 11. Fora de escopo (roadmap, apenas considerar na arquitetura)

- **V2 — RAG:** embeddings, pgvector, recuperação semântica.
- **V3 — Ferramentas de diagnóstico:** o assistente solicita consultas a serviços Java autorizados (ping, traceroute, conectividade).
- **V4 — Monitoramento:** integração com APIs como Zabbix, com credenciais seguras e permissões limitadas.
- **V5 — Interface web:** chat em Next.js + TypeScript.
- Memória de conversa (`conversationId`): o MVP é **stateless**.
