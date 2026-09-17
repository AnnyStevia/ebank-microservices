# EBank Microservices

A Spring Cloud banking platform built as independent Maven Spring Boot services. The long-term goal is a complete digital banking architecture: customer management, bank accounts and operations, service discovery, an API gateway, resilience, centralized configuration, a Spring AI chatbot, MCP, an Angular frontend, and Telegram integration.

The project currently has working **Customer Service**, **EBank Service**, **Eureka Discovery Server**, and a **Gateway** that routes dynamically through Eureka Discovery Locator. **EBank Service** calls **Customer Service** with Spring Cloud OpenFeign, resolving `CUSTOMER-SERVICE` through Eureka. A Resilience4j Circuit Breaker protects account-by-id customer lookup and returns a fallback Customer when Customer Service is unavailable.

## Planned microservices

| Service | Package | Planned role |
| --- | --- | --- |
| `customer-service` | `com.example.ebank.customer` | Customer registration, profiles, and identity data |
| `ebank-service` | `com.example.ebank.bank` | Bank accounts, balances, and banking operations |
| `discovery-service` | `com.example.ebank.discovery` | Eureka service registry |
| `gateway-service` | `com.example.ebank.gateway` | Single entry point (Spring Cloud Gateway) |

## Planned architecture

Clients will reach the platform through `gateway-service`. The gateway routes traffic to backend services registered in `discovery-service`. `ebank-service` calls `customer-service` with OpenFeign (`CUSTOMER-SERVICE` via Eureka). A Resilience4j Circuit Breaker (`customerService`) protects GET-by-id customer lookup. Later stages will add a Config Server, Spring AI chatbot, MCP, Angular frontend, and Telegram integration.

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

- Root Maven aggregator for the four independent services
- **Customer Service** REST API on port `8056` (JPA, H2, Actuator, Swagger UI)
- **EBank Service** REST API on port `8057` (JPA, H2, Actuator, Swagger UI)
- **Discovery Service** Eureka Server on port `8761`
- **Gateway Service** on port `9999` with Eureka Discovery Locator
- Eureka client registration for Customer Service, EBank Service, and Gateway
- OpenFeign: EBank Service retrieves and validates customers through `CUSTOMER-SERVICE` (Eureka, not `localhost:8056`)
- Resilience4j Circuit Breaker on GET `/accounts/{id}` customer lookup, with fallback Customer (`Not Available` / `not available`)

**Not started**

- Config Server, Spring AI, MCP, Angular, Telegram, or Docker

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
| Health | `GET http://localhost:8056/actuator/health` |
| Swagger UI | `http://localhost:8056/swagger-ui.html` |
| H2 console | `http://localhost:8056/h2-console` (JDBC URL `jdbc:h2:mem:customerdb`) |

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

This is a standalone Eureka Server (`register-with-eureka=false`, `fetch-registry=false`). Customer, EBank, and Gateway register as clients.

Start order: Discovery → Customer → Gateway → EBank.

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

Routes come from Eureka Discovery Locator. Static `http://localhost:8056` / `8057` routes are no longer used.

## Project structure

```
ebank-microservices/
├── customer-service/
├── ebank-service/
├── discovery-service/
├── gateway-service/
├── pom.xml
├── README.md
└── .gitignore
```

Each service is an independent Maven Spring Boot application. The root `pom.xml` is an aggregator only; it does not impose a custom parent on the services.

## Tech stack (foundation)

- Java 21
- Spring Boot 3.5.16
- Spring Cloud 2025.0.3
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
