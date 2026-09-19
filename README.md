# EBank Microservices

A Spring Cloud banking platform built as independent Maven Spring Boot services. The long-term goal is a complete digital banking architecture: customer management, bank accounts and operations, service discovery, an API gateway, resilience, centralized configuration, a Spring AI chatbot, MCP, an Angular frontend, and Telegram integration.

The project currently has working **Customer Service**, **EBank Service**, **Eureka Discovery Server**, a **Gateway** with Eureka Discovery Locator, and a minimal **EBank Bot** chatbot (`GET /chat`) that calls OpenAI through Spring AI `ChatClient`. **Customer Service** is also an MCP Streamable HTTP server at `http://localhost:8056/mcp`. **EBank Service** calls **Customer Service** with OpenFeign (`CUSTOMER-SERVICE` via Eureka) and a Resilience4j Circuit Breaker on account-by-id lookup. EBank Bot is not connected to MCP yet.

## Planned microservices

| Service | Package | Planned role |
| --- | --- | --- |
| `customer-service` | `com.example.ebank.customer` | Customer registration, profiles, and identity data |
| `ebank-service` | `com.example.ebank.bank` | Bank accounts, balances, and banking operations |
| `discovery-service` | `com.example.ebank.discovery` | Eureka service registry |
| `gateway-service` | `com.example.ebank.gateway` | Single entry point (Spring Cloud Gateway) |
| `ebank-bot` | `com.example.ebank.bot` | Spring AI chatbot (`GET /chat` → OpenAI) |

## Planned architecture

Clients will reach the platform through `gateway-service`. The gateway routes traffic to backend services registered in `discovery-service`. `ebank-service` calls `customer-service` with OpenFeign (`CUSTOMER-SERVICE` via Eureka). A Resilience4j Circuit Breaker (`customerService`) protects GET-by-id customer lookup. `customer-service` also exposes the same customer operations as Spring AI MCP tools over Streamable HTTP (`/mcp`). `ebank-bot` is a standalone Spring AI chatbot: REST calls `EBankAgent`, which uses `ChatClient` and in-memory conversation memory (not yet routed through the Gateway, and not connected to MCP yet). Later stages will add an MCP client, Config Server, Angular, and Telegram.

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

**Not started**

- EBank Bot MCP client, EBank MCP server, Gateway bot routing
- Config Server, Angular, Telegram, or Docker

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

The existing `CustomerService` methods (`findAll`, `findById`, `save`) are also MCP tools (`getAllCustomers`, `getCustomerById`, `saveCustomer`) over Streamable HTTP. REST endpoints are unchanged. EBank Bot is not connected to this MCP server yet.

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
| Health | `GET http://localhost:8057/actuator/health` |
| Swagger UI | `http://localhost:8057/swagger-ui.html` |
| H2 console | `http://localhost:8057/h2-console` (JDBC URL `jdbc:h2:mem:ebankdb`) |

`BankAccount.customerId` is a plain Long. The nested `customer` field is `@Transient` (not stored in EBank's H2 database). `GET /accounts/{id}` loads the customer through `CustomerLookupService`, which calls `CustomerRestClient` (`@FeignClient(name = "CUSTOMER-SERVICE")`) behind a Resilience4j Circuit Breaker named `customerService`. If Customer Service is down, the account is still returned with a fallback Customer (same id, name `Not Available`, email `not available`). `POST /accounts` still validates the customer through Feign with no fallback; save fails if that customer cannot be retrieved.

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

Start order: Discovery → Customer → Gateway → EBank → EBank Bot.

## Gateway Service

Run from the repository root after Discovery Service (`8761`) is up:

```bash
mvn -pl gateway-service spring-boot:run
```

| Resource | URL |
| --- | --- |
| Customers via Gateway | `GET http://localhost:9999/CUSTOMER-SERVICE/customers` |
| Accounts via Gateway | `GET http://localhost:9999/EBANK-SERVICE/accounts` |
| Health | `GET http://localhost:9999/actuator/health` |

Routes come from Eureka Discovery Locator. Static `http://localhost:8056` / `8057` routes are no longer used. EBank Bot is not Gateway-routed yet.

## EBank Bot

Requires the `OPENAI_API_KEY` environment variable (not stored in the repository). Run from the repository root after Discovery Service (`8761`) is up:

```bash
mvn -pl ebank-bot spring-boot:run
```

| Resource | URL |
| --- | --- |
| Chat | `GET http://localhost:8058/chat?query=bonjour` |
| Chat (default query `bonjour`) | `GET http://localhost:8058/chat` |
| Chat with conversation | `GET http://localhost:8058/chat?query=My%20name%20is%20Mohammed&conversationId=1` |
| Health | `GET http://localhost:8058/actuator/health` |
| Swagger UI | `http://localhost:8058/swagger-ui.html` |

`GET /chat` calls `EBankAgent`, which uses the shared `ChatClient` with `MessageChatMemoryAdvisor` and in-memory `ChatMemory`. The same `conversationId` keeps prior messages (for example, "My name is Mohammed" then "What is my name?"). Omit `conversationId` to use `default`. Discord, Telegram, and an MCP client are not implemented yet. Customer Service already exposes MCP tools; the bot is not connected to them.

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
- Spring AI 1.1.5 (`ebank-bot` ChatClient; `customer-service` MCP server)
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
