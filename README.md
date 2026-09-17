# EBank Microservices

A Spring Cloud banking platform built as independent Maven Spring Boot services. The long-term goal is a complete digital banking architecture: customer management, bank accounts and operations, service discovery, an API gateway, resilience, centralized configuration, a Spring AI chatbot, MCP, an Angular frontend, and Telegram integration.

The project currently has a completed foundation plus working **Customer Service**, **EBank Service**, and a standalone **Eureka Discovery Server**. Customer and EBank services are not registered with Eureka yet.

## Planned microservices

| Service | Package | Planned role |
| --- | --- | --- |
| `customer-service` | `com.example.ebank.customer` | Customer registration, profiles, and identity data |
| `ebank-service` | `com.example.ebank.bank` | Bank accounts, balances, and banking operations |
| `discovery-service` | `com.example.ebank.discovery` | Eureka service registry |
| `gateway-service` | `com.example.ebank.gateway` | Single entry point (Spring Cloud Gateway) |

## Planned architecture

Clients will reach the platform through `gateway-service`. The gateway will route traffic to backend services registered in `discovery-service`. `customer-service` and `ebank-service` will communicate with OpenFeign and be protected by Resilience4j. Later stages will add a Config Server, Spring AI chatbot, MCP, Angular frontend, and Telegram integration.

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
     OpenFeign + Resilience4j
```

These integrations are **not present yet**. They will be added in later steps.

## Current implementation status

**Completed**

- Root Maven aggregator for the four independent services
- Empty Spring Boot 3.5 application on Java 21 for `gateway-service`
- **Customer Service** REST API on port `8056` (JPA, H2, Actuator, Swagger UI)
- **EBank Service** REST API on port `8057` (JPA, H2, Actuator, Swagger UI)
- **Discovery Service** Eureka Server on port `8761`

**Not started**

- Eureka client registration for Customer Service and EBank Service
- API Gateway routing, OpenFeign, or Resilience4j
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

`BankAccount.customerId` is a plain Long referencing a customer in Customer Service. There is no JPA relationship and no OpenFeign call yet.

## Discovery Service

Run from the repository root:

```bash
mvn -pl discovery-service spring-boot:run
```

| Resource | URL |
| --- | --- |
| Eureka dashboard | `http://localhost:8761` |
| Health | `GET http://localhost:8761/actuator/health` |

This is a standalone Eureka Server (`register-with-eureka=false`, `fetch-registry=false`). Other services are not registered yet.

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
