# 🚀 PayFlow — Production-Grade Payment Processing Platform

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.6-black)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-7.2-red)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)](https://docs.docker.com/compose/)

> A Stripe/Razorpay-inspired, **production-grade** distributed payment processing platform
> built with Java 21, Spring Boot 3, Kafka, Redis, React, and Docker.

---

## 📐 System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          CLIENT TIER                                    │
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────┐       │
│   │  React 18 + TypeScript + Tailwind CSS + Recharts (Port 3000)│       │
│   │  • Auth (JWT)  • Dashboard  • Payments  • Analytics         │       │
│   └───────────────────────────┬─────────────────────────────────┘       │
│                               │  HTTP/REST via Nginx Proxy              │
└───────────────────────────────┼─────────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────────┐
│                          API TIER (Port 8080)                           │
│                                                                         │
│   ┌────────────┐  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐   │
│   │AuthController│  │PaymentCtrl │  │TransactionCtrl│  │HealthCtrl   │  │
│   └──────┬─────┘  └──────┬──────┘  └──────┬────────┘  └──────────────┘  │
│          │               │                │                             │
│   ┌──────▼───────────────▼────────────────▼──────────────────────────┐  │
│   │               SPRING SECURITY (JWT Filter + RBAC)                │  │
│   └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│   ┌──────────────────────────────────────────────────────────────────┐  │
│   │                      SERVICE LAYER                               │  │
│   │  AuthService │ PaymentService │ RateLimitService │ CleanupJob   │  │
│   │              │   Resilience4j │                  │              │  │
│   │              │   ┌──────────┐ │                  │              │  │
│   │              │   │Circuit   │ │                  │              │  │
│   │              │   │Breaker   │ │                  │              │  │
│   │              │   │Retry     │ │                  │              │  │
│   │              │   │Timeout   │ │                  │              │  │
│   │              │   └──────────┘ │                  │              │  │
│   └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                    │               │               │
      ┌─────────────▼──┐  ┌────────▼────────┐  ┌──▼──────────────────┐
      │  PostgreSQL 16  │  │   Redis 7.2     │  │  Apache Kafka 7.6   │
      │  (Port 5432)    │  │   (Port 6379)   │  │  (Port 9092)        │
      │  • users        │  │  • Idempotency  │  │  • payment.created  │
      │  • payments     │  │  • Cache        │  │  • payment.processed│
      │  • transactions │  │  • Rate Limit   │  │  • payment.failed   │
      │  • retry_logs   │  │  • Dist. Lock   │  │  • payment.retry    │
      │  • audit_logs   │  │                 │  │  • notification     │
      │  • idempotency  │  │                 │  │  • dlq              │
      └─────────────────┘  └─────────────────┘  └─────────────────────┘
                    │
      ┌─────────────▼──────────────────────────────────────┐
      │              OBSERVABILITY STACK                    │
      │   Prometheus (9090) → Grafana (3001)               │
      │   Spring Actuator → /actuator/prometheus            │
      │   Custom Metrics: payments.created, payments.failed │
      │   JVM Metrics, HTTP Metrics, Kafka Metrics          │
      └─────────────────────────────────────────────────────┘
```

---

## 🔄 Payment Lifecycle & Sequence Flow

```
Client          API         PaymentService     Gateway(sim)     Kafka          DB
  │              │                │                 │              │            │
  │─POST /pay───►│                │                 │              │            │
  │              │─createPayment─►│                 │              │            │
  │              │                │─check idempotency (Redis)      │            │
  │              │                │─save INITIATED──────────────────────────────►│
  │              │                │─publish payment.created────────►│            │
  │              │◄──201 Created──│                 │              │            │
  │◄─response────│                │                 │              │            │
  │              │                │─acquireLock(Redis Redisson)     │            │
  │              │                │─update PROCESSING───────────────────────────►│
  │              │                │─call gateway────►│              │            │
  │              │                │◄─gateway ref─────│              │            │
  │              │                │─update SUCCESS──────────────────────────────►│
  │              │                │─create Transaction──────────────────────────►│
  │              │                │─publish payment.processed──────►│            │
  │              │                │─releaseLock                     │            │
  │              │                │                 │              │            │
  │              │    [Consumer]◄─────────────────────publish notify│            │
  │              │                │                 │  [email/SMS] │            │
```

### Retry Flow
```
FAILED → retry request → RETRYING → processPayment → SUCCESS or FAILED again
                                  └─(retryCount++)─→ if retryCount >= maxRetries → PERMANENTLY_FAILED (DLQ)
```

---

## 🛡️ Resilience Patterns

| Pattern         | Implementation               | Configuration                        |
|-----------------|------------------------------|--------------------------------------|
| Circuit Breaker | Resilience4j CB              | 50% failure rate → OPEN (30s)        |
| Retry           | Resilience4j Retry           | 3 attempts, exponential backoff (2s) |
| Timeout         | Resilience4j TimeLimiter     | 10s per payment processing call      |
| Rate Limiting   | Redis sliding window         | 100 req / 60s per user               |
| Idempotency     | Redis + DB (24h TTL)         | Idempotency-Key header               |
| Distributed Lock| Redisson (Redis)             | Per-payment lock, 30s max hold       |
| DLQ             | Kafka dead.letter.queue      | After 3 consumer retries             |
| Fallback        | CB fallback method           | Sets payment to FAILED gracefully    |

---

## 🗄️ Database ER Diagram

```
users
├── id (PK, UUID)
├── username (UNIQUE)
├── email (UNIQUE)
├── password_hash
├── first_name, last_name
├── role (USER|ADMIN|MERCHANT)
├── enabled, account_locked
├── refresh_token, refresh_token_expiry
└── created_at, updated_at

payments (FK → users)
├── id (PK, UUID)
├── payment_reference (UNIQUE, indexed)
├── user_id (FK → users)
├── amount, currency
├── status (INITIATED|PROCESSING|SUCCESS|FAILED|RETRYING|REVERSED|TIMEOUT)
├── payment_method (CARD|BANK_TRANSFER|UPI|WALLET|NET_BANKING)
├── idempotency_key (UNIQUE, indexed)
├── retry_count, max_retries
├── failure_reason, gateway_reference
├── metadata (JSONB), gateway_response (JSONB)
└── initiated_at, processed_at, reversed_at, created_at, updated_at

transactions (FK → payments, users)
├── id (PK, UUID)
├── transaction_reference (UNIQUE)
├── payment_id (FK → payments)
├── user_id (FK → users)
├── type (DEBIT|CREDIT|REVERSAL|FEE|REFUND)
├── amount, currency
├── status (PENDING|COMPLETED|FAILED|REVERSED)
├── balance_before, balance_after
└── description, metadata, created_at, updated_at

retry_logs (FK → payments)
├── id, payment_id, attempt_number
├── status (ATTEMPTED|SUCCESS|FAILED|SCHEDULED)
├── error_message, error_code
└── retry_at, next_retry_at, created_at

idempotency_records
├── id, idempotency_key (UNIQUE)
├── user_id, request_hash
├── response_status, response_body, endpoint
└── expires_at, created_at

audit_logs
├── id, entity_type, entity_id
├── action, old_value (JSONB), new_value (JSONB)
├── performed_by, ip_address, user_agent
└── correlation_id, created_at
```

---

## 🚀 Quick Start — Run Locally

### Prerequisites
- Docker Desktop ≥ 4.x
- docker-compose ≥ 2.x
- 8GB RAM recommended

### 1-Command Launch

```bash
git clone <repo-url>
cd payment-platform
docker-compose up --build
```

If Docker is unavailable, start only the backend locally with a safe local profile:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Services come up in this order:
1. PostgreSQL (with Flyway migrations)
2. Redis
3. Zookeeper → Kafka
4. Backend (Spring Boot)
5. Frontend (React via Nginx)
6. Prometheus + Grafana

### 🌐 Access URLs

| Service    | URL                              | Credentials        |
|------------|----------------------------------|--------------------|
| Frontend   | http://localhost:3000            | demo / Demo@123456 |
| Backend    | http://localhost:8080/api        | JWT via login      |
| Actuator   | http://localhost:8080/actuator   | —                  |
| Prometheus | http://localhost:9090            | —                  |
| Grafana    | http://localhost:3001            | admin / payflow123 |
| PostgreSQL | localhost:5432/paymentdb         | paymentuser/paymentpass |
| Redis      | localhost:6379                   | —                  |
| Kafka      | localhost:9092                   | —                  |

---

## 📡 REST API Reference

### Authentication

```
POST   /api/auth/register        Register new user
POST   /api/auth/login           Login → JWT tokens
POST   /api/auth/refresh         Refresh access token  (Header: Refresh-Token)
POST   /api/auth/logout          Invalidate session    (Auth required)
GET    /api/auth/me              Current user profile  (Auth required)
```

**Register Request:**
```json
{
  "username":  "john_doe",
  "email":     "john@example.com",
  "password":  "John@1234",
  "firstName": "John",
  "lastName":  "Doe"
}
```

**Login Response:**
```json
{
  "success": true,
  "data": {
    "accessToken":  "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "tokenType":    "Bearer",
    "expiresIn":    900000,
    "user": { "id": "...", "username": "john_doe", "role": "USER" }
  }
}
```

---

### Payments

All payment endpoints require `Authorization: Bearer <token>`.

```
POST   /api/payments             Create payment (optional: Idempotency-Key header)
GET    /api/payments             List payments  (?page=0&size=20&status=SUCCESS)
GET    /api/payments/{id}        Get payment details
POST   /api/payments/{id}/retry  Retry failed payment
POST   /api/payments/{id}/reverse Reverse successful payment
GET    /api/payments/analytics   Dashboard analytics (?days=30)
```

**Create Payment Request:**
```json
{
  "amount":        150.00,
  "currency":      "USD",
  "paymentMethod": "CARD",
  "description":   "Invoice #1234"
}
```

**Payment Response:**
```json
{
  "success": true,
  "data": {
    "id":               "uuid",
    "paymentReference": "PAY-20240101-ABCD1234",
    "amount":           150.00,
    "currency":         "USD",
    "status":           "INITIATED",
    "paymentMethod":    "CARD",
    "retryCount":       0,
    "maxRetries":       3,
    "canRetry":         false,
    "canReverse":       false,
    "initiatedAt":      "2024-01-01T10:00:00Z",
    "createdAt":        "2024-01-01T10:00:00Z"
  }
}
```

---

### Transactions

```
GET    /api/transactions          List transactions (?page=0&size=20)
GET    /api/transactions/{id}     Get transaction detail
```

---

### Health & Monitoring

```
GET    /api/health                       Application health
GET    /actuator/health                  Spring Boot health (detailed)
GET    /actuator/metrics                 Micrometer metrics
GET    /actuator/prometheus              Prometheus scrape endpoint
GET    /actuator/info                    App info
```

---

## 🔑 Idempotency Usage

Prevent duplicate payments by supplying a unique key per request:

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer <token>" \
  -H "Idempotency-Key: invoice-1234-attempt-1" \
  -H "Content-Type: application/json" \
  -d '{"amount":100,"currency":"USD","paymentMethod":"CARD"}'
```

Repeat the identical call — you receive the **same response** without creating a duplicate payment.

---

## 📊 Kafka Topics

| Topic                  | Purpose                                  | Partitions |
|------------------------|------------------------------------------|------------|
| `payment.created`      | Published when payment is initiated      | 3          |
| `payment.processed`    | Published on successful processing       | 3          |
| `payment.failed`       | Published on payment failure             | 3          |
| `payment.retry`        | Triggers retry workflow                  | 3          |
| `payment.reversed`     | Published on reversal                    | 3          |
| `payment.notification` | Email/SMS notification events            | 3          |
| `payment.dlq`          | Dead-letter queue for unprocessable msgs | 1          |

---

## 🏗️ Project Structure

```
payment-platform/
├── backend/
│   ├── src/main/java/com/paymentplatform/
│   │   ├── PaymentPlatformApplication.java
│   │   ├── config/              # Security, Redis, Kafka, Metrics configs
│   │   ├── controller/          # REST API controllers
│   │   ├── dto/
│   │   │   ├── request/         # CreatePaymentRequest, LoginRequest, etc.
│   │   │   └── response/        # PaymentResponse, AuthResponse, etc.
│   │   ├── entity/              # JPA entities (User, Payment, Transaction…)
│   │   ├── exception/           # Custom exceptions + GlobalExceptionHandler
│   │   ├── kafka/
│   │   │   ├── PaymentEvent.java
│   │   │   ├── producer/        # PaymentEventProducer
│   │   │   └── consumer/        # PaymentEventConsumer
│   │   ├── repository/          # Spring Data JPA repositories
│   │   ├── security/
│   │   │   ├── jwt/             # JwtUtils, JwtAuthenticationFilter
│   │   │   └── service/         # UserDetailsServiceImpl
│   │   ├── service/impl/        # AuthService, PaymentService, RateLimitService
│   │   └── util/                # CorrelationIdUtils, PaymentReferenceGenerator
│   ├── src/main/resources/
│   │   ├── application.yml      # Main config with all Resilience4j settings
│   │   └── db/migration/        # Flyway V1 schema, V2 seed data
│   └── src/test/                # Unit + integration tests
│
├── frontend/
│   └── src/
│       ├── App.tsx              # Router setup with lazy loading
│       ├── context/             # AuthContext (JWT session management)
│       ├── hooks/               # usePayments, useAnalytics, etc. (React Query)
│       ├── pages/               # LoginPage, RegisterPage, Dashboard, Payments…
│       ├── components/
│       │   ├── common/          # StatusBadge, EmptyState, ProtectedRoute
│       │   ├── dashboard/       # StatCard
│       │   └── layout/          # Sidebar, AppLayout
│       ├── services/            # apiClient (Axios+interceptors), authService, paymentService
│       ├── types/               # TypeScript interfaces
│       └── utils/               # formatAmount, formatDate, status colour helpers
│
├── infrastructure/
│   ├── prometheus/prometheus.yml
│   └── grafana/
│       ├── provisioning/        # Auto-provisioned datasource + dashboard
│       └── dashboards/          # payment-platform.json (Grafana dashboard)
│
├── docker-compose.yml           # Full stack: PG + Redis + Kafka + BE + FE + Monitoring
└── README.md
```

---

## 🔧 Environment Variables

| Variable                   | Default                  | Description                   |
|----------------------------|--------------------------|-------------------------------|
| `DB_HOST`                  | `localhost`              | PostgreSQL host               |
| `DB_PORT`                  | `5432`                   | PostgreSQL port               |
| `DB_NAME`                  | `paymentdb`              | Database name                 |
| `DB_USER`                  | `paymentuser`            | Database username             |
| `DB_PASS`                  | `paymentpass`            | Database password             |
| `REDIS_HOST`               | `localhost`              | Redis host                    |
| `REDIS_PORT`               | `6379`                   | Redis port                    |
| `KAFKA_BOOTSTRAP_SERVERS`  | `localhost:9092`         | Kafka broker addresses        |
| `JWT_SECRET`               | (long default)           | HS512 signing key (change!)   |
| `CORS_ORIGINS`             | `http://localhost:3000`  | Allowed CORS origins          |

---

## 📈 Observability

### Prometheus Metrics (auto-scraped from `/actuator/prometheus`)

| Metric                              | Type    | Description                       |
|-------------------------------------|---------|-----------------------------------|
| `payments_created_total`            | Counter | Total payments created            |
| `payments_success_total`            | Counter | Total successful payments         |
| `payments_failed_total`             | Counter | Total failed payments             |
| `payments_retried_total`            | Counter | Total retried payments            |
| `payments_reversed_total`           | Counter | Total reversed payments           |
| `payment_creation_time_seconds`     | Timer   | Payment creation latency (p50/95/99)|
| `kafka_events_published_total`      | Counter | Kafka events published            |
| `kafka_dlq_messages_total`          | Counter | Dead-letter queue messages        |
| `jvm_memory_used_bytes`             | Gauge   | JVM heap & non-heap usage         |
| `http_server_requests_seconds`      | Timer   | HTTP request latency              |

### Grafana Dashboard
Navigate to http://localhost:3001 → **PayFlow** folder → **PayFlow — Payment Platform**.

---

## 🧪 Running Tests

```bash
cd backend

# Unit tests only
mvn test -Dtest="*Test" -pl .

# Specific test class
mvn test -Dtest="PaymentServiceTest" -pl .

# With coverage report
mvn test jacoco:report
open target/site/jacoco/index.html
```

---

## 🔐 Security Features

- **JWT Access Tokens** (15-min expiry, HS512-signed)
- **Refresh Tokens** (7-day expiry, bcrypt-hashed in DB)
- **BCrypt password hashing** (strength 12)
- **Role-Based Access Control** (USER, ADMIN, MERCHANT)
- **Correlation IDs** for full request traceability
- **Rate Limiting** via Redis sliding window (100 req/min)
- **Idempotency** prevents double-charge scenarios
- **Distributed Locking** prevents concurrent payment mutation
- **Secure CORS** with explicit allowed-origins
- **Input Validation** on all endpoints (Jakarta Bean Validation)
- **Global Exception Handler** — no stack traces leaked to client

---

## 🎯 Resume-Ready Description

> **PayFlow — Distributed Payment Processing Platform** *(Personal/Portfolio Project)*
>
> Engineered a production-grade fintech payment platform inspired by Stripe/Razorpay using
> **Java 21, Spring Boot 3, Apache Kafka, Redis, and React 18**. Implemented distributed
> reliability through **Resilience4j** (circuit breaker, retry with exponential backoff,
> timeout/fallback), Redis-backed **idempotency** and **distributed locking** (Redisson),
> and a **Kafka event-driven** architecture with dead-letter-queue handling. Database schema
> designed with **PostgreSQL** (normalized, indexed, Flyway-migrated), full **JWT
> authentication** with refresh-token rotation, **RBAC**, and a Prometheus + Grafana
> observability stack with custom payment metrics. Delivered as a fully **Dockerized**
> multi-service application (`docker-compose up --build`) with a professional **React +
> TypeScript** dashboard featuring real-time charts, retry management, and analytics.
>
> **Tech:** Java 21 · Spring Boot 3 · Spring Security · Kafka · Redis · PostgreSQL · Flyway ·
> Resilience4j · JWT · React 18 · TypeScript · Tailwind CSS · Prometheus · Grafana · Docker

---

## 💼 Interview Talking Points

### "Walk me through the payment flow"
> "A payment starts as INITIATED in PostgreSQL and its idempotency key is atomically written
> to Redis. An async task then acquires a Redisson distributed lock on the payment ID,
> transitions the status to PROCESSING, calls the (simulated) payment gateway wrapped in a
> Resilience4j circuit breaker with a 10-second timeout and 3-attempt retry, and upon success
> creates a Transaction ledger record. Every state change emits a Kafka event—consumed by
> notification handlers and the DLQ monitor—and is captured in the audit_log table with
> a correlation ID for full traceability."

### "How does idempotency work?"
> "The client sends an `Idempotency-Key` header. Before processing, we check Redis for that
> key scoped to the user ID. On a cache miss, we proceed and store the payment ID in Redis
> with a 24-hour TTL and also persist it in the `idempotency_records` table. On a cache hit,
> we return the existing payment without re-processing. This guarantees exactly-once semantics
> even under network retries."

### "Why Kafka over direct DB polling?"
> "Kafka decouples the payment processor from downstream concerns like notifications and
> analytics. Producers don't block on consumer availability. The event log enables replay
> for debugging, audit, or rebuilding read models. The DLQ captures poison pills for manual
> review without blocking the main consumer group."

### "How does the circuit breaker protect the system?"
> "Resilience4j monitors the last 10 payment gateway calls. If ≥50% fail, it trips OPEN
> for 30 seconds, routing all calls to the fallback (which marks the payment FAILED and
> publishes a Kafka event), preventing cascade failure. After 30s it enters HALF-OPEN,
> allows 3 probe calls, and resets if they succeed."
