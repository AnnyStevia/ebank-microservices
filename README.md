# EBank Microservices

A Spring Cloud banking platform built as independent Maven Spring Boot services. The long-term goal is a complete digital banking architecture: customer management, bank accounts and operations, service discovery, an API gateway, resilience, centralized configuration, a Spring AI chatbot, MCP, an Angular frontend, and Telegram integration.

The project currently has working **Customer Service**, **EBank Service**, **Eureka Discovery Server**, a **Gateway** with Eureka Discovery Locator, and **EBank Bot** (`GET /chat`, Discord, and Telegram) that calls OpenAI through Spring AI `ChatClient`. **Customer Service** is an MCP Streamable HTTP server at `http://localhost:8056/mcp`. **EBank Service** is an MCP Streamable HTTP server at `http://localhost:8057/mcp`. **EBank Bot** is an MCP client of both servers, so the LLM can invoke customer tools and bank-account tools. **EBank Service** also calls **Customer Service** with OpenFeign (`CUSTOMER-SERVICE` via Eureka) and a Resilience4j Circuit Breaker on account-by-id lookup.

## Planned microservices

| Service | Package | Planned role |
| --- | --- | --- |
| `customer-service` | `com.example.ebank.customer` | Customer registration, profiles, and identity data |
| `ebank-service` | `com.example.ebank.bank` | Bank accounts, balances, and banking operations |
| `discovery-service` | `com.example.ebank.discovery` | Eureka service registry |
| `gateway-service` | `com.example.ebank.gateway` | Single entry point (Spring Cloud Gateway) |
| `ebank-bot` | `com.example.ebank.bot` | Spring AI chatbot (`GET /chat`, Discord, Telegram → `EBankAgent`) |

## Planned architecture

Clients will reach the platform through `gateway-service`. The gateway routes traffic to backend services registered in `discovery-service`. `ebank-service` calls `customer-service` with OpenFeign (`CUSTOMER-SERVICE` via Eureka). A Resilience4j Circuit Breaker (`customerService`) protects GET-by-id customer lookup. Both `customer-service` and `ebank-service` also expose their existing operations as Spring AI MCP tools over Streamable HTTP (`/mcp`). `ebank-bot` is a Spring AI chatbot: REST, Discord, and Telegram all call the same `EBankAgent`, which uses `ChatClient`, in-memory conversation memory, and Streamable HTTP MCP clients to Customer Service and EBank Service. The same `/chat` endpoint is also reachable through the Gateway as `/EBANK-BOT/chat`. Discord and Telegram talk to EBank Bot directly (Gateway is not required). Later stages will add Config Server and Angular.

```
[ Angular / Telegram ]
          |
          v
   gateway-service
          |
          v
  discovery-service (Eureka)
          |
   +------+------+
   |             |
   v             v
customer-service  ebank-service
   |             |
   +------+------+
          |
     OpenFeign + Circuit Breaker (EBank -> Customer)
```

OpenFeign and Resilience4j Circuit Breaker are implemented. Retry, RateLimiter, Bulkhead, and TimeLimiter are **not** used.

## Current implementation status

**Completed**

- Root Maven aggregator for the five independent services
- **Customer Service** REST API on port `8056` (JPA, H2, Actuator, Swagger UI)
- **EBank Service** REST API on port `8057` (JPA, H2, Actuator, Swagger UI)
- **Discovery Service** Eureka Server on port `8761`
- **Gateway Service** on port `9999` with Eureka Discovery Locator
- Eureka client registration for Customer Service, EBank Service, and Gateway
- OpenFeign: EBank Service retrieves and validates customers through `CUSTOMER-SERVICE` (Eureka, not `localhost:8056`)
- Resilience4j Circuit Breaker on GET `/accounts/{id}` customer lookup, with fallback Customer (`Not Available` / `not available`)
- **EBank Bot** on port `8058`: `GET /chat` → Spring AI `ChatClient` → OpenAI (registers with Eureka as `EBANK-BOT`)
- Conversation memory via `MessageChatMemoryAdvisor` and `ChatMemory` (`conversationId` on `GET /chat`)
- `EBankAgent` as the reusable chatbot component (`GET /chat` → `EBankAgent` → `ChatClient`)
- **Customer Service MCP server**: Streamable HTTP at `http://localhost:8056/mcp` (`getAllCustomers`, `getCustomerById`, `saveCustomer`)
- **EBank Service MCP server**: Streamable HTTP at `http://localhost:8057/mcp` (`getAllBankAccounts`, `getBankAccountById`, `saveBankAccount`)
- **EBank Bot MCP client**: Streamable HTTP to Customer `:8056/mcp` and EBank `:8057/mcp`; both tool sets are `ToolCallback`s on the existing `ChatClient`
- **Gateway bot route**: Eureka Discovery Locator exposes `GET /EBANK-BOT/chat` on port `9999` (direct `:8058/chat` still works)
- **Discord and Telegram clients**: adapters reuse `EBankAgent` (no extra OpenAI/MCP/ChatClient wiring)

**Not started**

- Config Server, Angular, Keycloak, or Docker

## Customer Service

Run from the repository root:

```bash
mvn -pl customer-service spring-boot:run
```

| Resource | URL |
| --- | --- |
| List customers | `GET http://localhost:8056/customers` |
| Get customer by ID | `GET http://localhost:8056/customers/{id}` |
| Create customer | `POST http://localhost:8056/customers` |
| MCP Streamable HTTP | `http://localhost:8056/mcp` |
| Health | `GET http://localhost:8056/actuator/health` |
| Swagger UI | `http://localhost:8056/swagger-ui.html` |
| H2 console | `http://localhost:8056/h2-console` (JDBC URL `jdbc:h2:mem:customerdb`) |

The existing `CustomerService` methods (`findAll`, `findById`, `save`) are also MCP tools (`getAllCustomers`, `getCustomerById`, `saveCustomer`) over Streamable HTTP. REST endpoints are unchanged. EBank Bot connects to this MCP server as a client.

## EBank Service

Run from the repository root:

```bash
mvn -pl ebank-service spring-boot:run
```

| Resource | URL |
| --- | --- |
| List accounts | `GET http://localhost:8057/accounts` |
| Get account by ID | `GET http://localhost:8057/accounts/{id}` |
| Create account | `POST http://localhost:8057/accounts` |
| MCP Streamable HTTP | `http://localhost:8057/mcp` |
| Health | `GET http://localhost:8057/actuator/health` |
| Swagger UI | `http://localhost:8057/swagger-ui.html` |
| H2 console | `http://localhost:8057/h2-console` (JDBC URL `jdbc:h2:mem:ebankdb`) |

`BankAccount.customerId` is a plain Long. The nested `customer` field is `@Transient` (not stored in EBank's H2 database). `GET /accounts/{id}` loads the customer through `CustomerLookupService`, which calls `CustomerRestClient` (`@FeignClient(name = "CUSTOMER-SERVICE")`) behind a Resilience4j Circuit Breaker named `customerService`. If Customer Service is down, the account is still returned with a fallback Customer (same id, name `Not Available`, email `not available`). `POST /accounts` still validates the customer through Feign with no fallback; save fails if that customer cannot be retrieved.

The existing `BankAccountService` methods (`findAll`, `findById`, `save`) are also MCP tools (`getAllBankAccounts`, `getBankAccountById`, `saveBankAccount`) over Streamable HTTP. REST and OpenFeign behavior are unchanged. Account `type` is `CURRENT` or `SAVING`. There is no by-customer query API; listing accounts and filtering by `customerId` covers that.

## Discovery Service

Run from the repository root:

```bash
mvn -pl discovery-service spring-boot:run
```

| Resource | URL |
| --- | --- |
| Eureka dashboard | `http://localhost:8761` |
| Health | `GET http://localhost:8761/actuator/health` |

This is a standalone Eureka Server (`register-with-eureka=false`, `fetch-registry=false`). Customer, EBank, Gateway, and EBank Bot register as clients.

Start order: Discovery → Customer → EBank → EBank Bot → Gateway.

## Gateway Service

Run from the repository root after Discovery Service (`8761`) is up:

```bash
mvn -pl gateway-service spring-boot:run
```

| Resource | URL |
| --- | --- |
| Customers via Gateway | `GET http://localhost:9999/CUSTOMER-SERVICE/customers` |
| Accounts via Gateway | `GET http://localhost:9999/EBANK-SERVICE/accounts` |
| Chat via Gateway | `GET http://localhost:9999/EBANK-BOT/chat?query=bonjour` |
| Health | `GET http://localhost:9999/actuator/health` |

Routes come from Eureka Discovery Locator. Static `http://localhost:8056` / `8057` / `8058` routes are not used. `EBANK-BOT` is reached the same way as the other Eureka service IDs. The Gateway does not call OpenAI or MCP; it only forwards HTTP to the bot.

## EBank Bot

Requires the `OPENAI_API_KEY` environment variable (not stored in the repository). Discord and Telegram reuse that same key through `EBankAgent`; they do not have their own OpenAI keys. Run from the repository root after Discovery Service (`8761`) is up:

```bash
mvn -pl ebank-bot spring-boot:run
```

| Resource | URL |
| --- | --- |
| Chat | `GET http://localhost:8058/chat?query=bonjour` |
| Chat (default query `bonjour`) | `GET http://localhost:8058/chat` |
| Chat with conversation | `GET http://localhost:8058/chat?query=My%20name%20is%20Mohammed&conversationId=1` |
| Chat via Gateway | `GET http://localhost:9999/EBANK-BOT/chat?query=bonjour` |
| Health | `GET http://localhost:8058/actuator/health` |
| Swagger UI | `http://localhost:8058/swagger-ui.html` |

`GET /chat` returns **text/plain**. It calls `EBankAgent`, which uses the shared `ChatClient` with `MessageChatMemoryAdvisor`, in-memory `ChatMemory`, and one MCP `ToolCallbackProvider` for both Customer Service and EBank Service. The same `conversationId` keeps prior messages. Omit `conversationId` to use `default`. Discord uses `discord-{channelId}` and Telegram uses `telegram-{chatId}` as the conversation id so platform chats stay isolated. The LLM decides when to call customer or bank-account MCP tools. Direct `:8058/chat` and Gateway `/EBANK-BOT/chat` both work.

`telegrambots` 6.9.7.1 brings Jersey onto the classpath. Eureka would otherwise pick an incomplete Jersey transport, so EBank Bot sets `eureka.client.jersey.enabled=false` and still registers as `EBANK-BOT`. Jersey is not excluded from Telegram (the 6.x bot client uses it).

### Discord and Telegram tokens

Do not commit tokens. Supply them only as environment variables or local overrides:

| Variable / property | Purpose |
| --- | --- |
| `OPENAI_API_KEY` / `spring.ai.openai.api-key` | Required for live chatbot answers |
| `DISCORD_TOKEN` / `discord.token` | Discord bot token (optional; REST `/chat` starts without it) |
| `TELEGRAM_BOT_TOKEN` / `telegram.bot.token` | Telegram bot token (optional; omitted means Telegram is not started) |
| `TELEGRAM_BOT_USERNAME` / `telegram.bot.username` | Telegram bot username (defaults to `EBANK-BOT` if omitted) |

### Manual Discord test

1. Create an application and bot in the [Discord Developer Portal](https://discord.com/developers/applications).
2. Enable the **Message Content Intent** on the bot.
3. Copy the bot token and set `DISCORD_TOKEN` in your environment (never commit it).
4. Invite the bot to a server with permission to read and send messages.
5. Start Discovery → Customer Service → EBank Service → EBank Bot. Gateway is not required.
6. Send a message such as `List all customers`.

Expected path: Discord → `EBankAgent` → `ChatClient` → OpenAI → Customer MCP → Customer Service → Discord.

### Manual Telegram test

1. Open Telegram and start a chat with [BotFather](https://t.me/BotFather).
2. Send `/newbot` and choose a unique display name and a username that ends with `bot`.
3. Copy the token BotFather returns and set `TELEGRAM_BOT_TOKEN` (and optionally `TELEGRAM_BOT_USERNAME`) in your environment. Never commit them.
4. Start Discovery → Customer Service → EBank Service → EBank Bot. Gateway is not required.
5. Send a message such as `List all bank accounts`.

Expected path: Telegram → `EBankAgent` → `ChatClient` → OpenAI → EBank MCP → EBank Service → Telegram.

Example questions through the Gateway (Discovery, Customer, EBank, Bot, and Gateway must be running, plus `OPENAI_API_KEY`):

```
GET http://localhost:9999/EBANK-BOT/chat?query=bonjour
GET http://localhost:9999/EBANK-BOT/chat?query=My%20name%20is%20Mohammed&conversationId=gateway-test
GET http://localhost:9999/EBANK-BOT/chat?query=What%20is%20my%20name%3F&conversationId=gateway-test
GET http://localhost:9999/EBANK-BOT/chat?query=List%20all%20customers&conversationId=gateway-test
GET http://localhost:9999/EBANK-BOT/chat?query=List%20all%20bank%20accounts&conversationId=gateway-test
```

## Project structure

```
ebank-microservices/
├── customer-service/
├── ebank-service/
├── discovery-service/
├── gateway-service/
├── ebank-bot/
├── pom.xml
├── README.md
└── .gitignore
```

Each service is an independent Maven Spring Boot application. The root `pom.xml` is an aggregator only; it does not impose a custom parent on the services.

## Tech stack (foundation)

- Java 21
- Spring Boot 3.5.16
- Spring Cloud 2025.0.3
- Spring AI 1.1.5 (`ebank-bot` ChatClient + MCP client; `customer-service` and `ebank-service` MCP servers)
- Discord: `com.zgamelogic:spring-boot-starter-discord` 5.0.4
- Telegram: `org.telegram:telegrambots` 6.9.7.1
- Maven

## Prerequisites

- JDK 21 (`JAVA_HOME` must point to JDK 21)
- Apache Maven 3.9+

## How to verify

From the repository root:

```bash
mvn -q test
```

Or build one service:

```bash
mvn -q -pl customer-service test
```
