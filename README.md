# API Monitoring & Observability Platform

A complete platform for tracking API requests across multiple microservices, storing performance metrics, analyzing issues, and displaying them on a dashboard.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           ARCHITECTURE                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────┐         ┌──────────────────────────────────────┐  │
│  │  client-service  │         │         collector-service            │  │
│  │  (Spring Boot)   │ ──────► │         (Spring Boot)                │  │
│  │                  │  REST   │                                      │  │
│  │  • Interceptor   │  logs   │  • Receives logs from services       │  │
│  │  • Rate Limiter  │         │  • Generates alerts                  │  │
│  │  • Log sender    │         │  • JWT Authentication                │  │
│  └──────────────────┘         │  • REST APIs for frontend            │  │
│                               └──────────────────────────────────────┘  │
│                                          │                               │
│                                          ▼                               │
│                               ┌──────────────────────────────────────┐  │
│                               │           MongoDB                     │  │
│                               │  ┌─────────────┐ ┌─────────────────┐ │  │
│                               │  │   logsdb    │ │   metadatadb    │ │  │
│                               │  │  ─────────  │ │  ─────────────  │ │  │
│                               │  │  • logs     │ │  • users        │ │  │
│                               │  │             │ │  • alerts       │ │  │
│                               │  │             │ │  • issues       │ │  │
│                               │  └─────────────┘ └─────────────────┘ │  │
│                               └──────────────────────────────────────┘  │
│                                          │                               │
│                                          ▼                               │
│                               ┌──────────────────────────────────────┐  │
│                               │      Frontend (Next.js)              │  │
│                               │  • Login page                        │  │
│                               │  • Dashboard with widgets            │  │
│                               │  • Logs table with filters           │  │
│                               │  • Alerts viewer                     │  │
│                               │  • Issue management                  │  │
│                               └──────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

## 📁 Project Structure

```
leap-project/
├── client-service/          # API Tracking Client (Spring Boot + Kotlin)
│   ├── src/main/kotlin/com/leap/clientservice/
│   │   ├── interceptor/     # API tracking interceptor
│   │   ├── util/            # Rate limiter
│   │   ├── model/           # ApiLog data class
│   │   └── config/          # WebMvcConfigurer
│   └── build.gradle.kts
│
├── collector-service/       # Central Collector (Spring Boot + Kotlin)
│   ├── src/main/kotlin/com/leap/collectorservice/
│   │   ├── auth/            # JWT authentication
│   │   ├── config/          # MongoDB, Security, CORS config
│   │   ├── controller/      # REST endpoints
│   │   ├── model/           # Entities (ApiLog, Alert, Issue, User)
│   │   ├── repository/      # MongoDB repositories
│   │   └── service/         # Business logic, rate limiting
│   └── build.gradle.kts
│
├── frontend/                # Next.js Dashboard
│   ├── pages/               # Login, Dashboard
│   ├── lib/                 # API client
│   └── styles/              # Tailwind CSS
│
└── README.md
```

## 🗄️ Database Schemas

### Logs Database (logsdb)

**Collection: logs**
```json
{
  "_id": "ObjectId",
  "endpoint": "/api/users",
  "method": "GET",
  "timestamp": "2025-01-01T00:00:00Z",
  "requestSize": 0,
  "responseSize": 1024,
  "statusCode": 200,
  "latencyMs": 150,
  "serviceName": "client-service",
  "rateLimitHit": false
}
```

### Metadata Database (metadatadb)

**Collection: users**
```json
{
  "_id": "ObjectId",
  "username": "admin",
  "passwordHash": "$2a$10$...",
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "createdAt": "2025-01-01T00:00:00Z"
}
```

**Collection: alerts**
```json
{
  "_id": "ObjectId",
  "type": "HIGH_LATENCY | SERVER_ERROR | RATE_LIMIT",
  "message": "Response took 750ms (threshold: 500ms)",
  "serviceName": "client-service",
  "endpoint": "/api/slow",
  "timestamp": "2025-01-01T00:00:00Z",
  "value": "750ms"
}
```

**Collection: issues**
```json
{
  "_id": "ObjectId",
  "title": "Login API slow",
  "description": "Takes more than 5 seconds",
  "serviceName": "client-service",
  "status": "OPEN | RESOLVED",
  "severity": "HIGH",
  "createdAt": "2025-01-01T00:00:00Z",
  "resolved": false,
  "resolvedAt": null,
  "resolvedBy": null,
  "version": 0  // For optimistic locking
}
```

**Collection: rate_limit_configs**
```json
{
  "_id": "ObjectId",
  "serviceName": "orders-service",
  "limit": 120,
  "enabled": true,
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-02T00:00:00Z",
  "updatedBy": "admin"
}
```

## 🔧 How Dual MongoDB Setup Works

The collector-service connects to **two separate MongoDB databases** on the same instance:

1. **logsdb** - High-volume storage for API logs
2. **metadatadb** - Low-volume storage for users, alerts, issues

### Configuration (application.yml)
```yaml
mongodb:
  logs:
    uri: mongodb://localhost:27017/logsdb
    database: logsdb
  metadata:
    uri: mongodb://localhost:27017/metadatadb
    database: metadatadb
```

### Implementation (MongoConfig.kt)
```kotlin
// Two MongoTemplate beans
@Bean("logsMongoTemplate")
@Primary
fun logsMongoTemplate(): MongoTemplate

@Bean("metadataMongoTemplate")
fun metadataMongoTemplate(): MongoTemplate

// Repositories use @Qualifier to select the correct template
@Repository
class LogRepository(
    @Qualifier("logsMongoTemplate")
    private val mongoTemplate: MongoTemplate
)
```

## ⚡ How Rate Limiter Works

### Token Bucket Algorithm

The rate limiter uses a **Token Bucket** algorithm:

1. Each service gets a bucket with `capacity` tokens
2. Tokens refill at a rate of `refillTokens` per `refillIntervalMs`
3. Each request consumes 1 token
4. If no tokens available, request is still processed but `rateLimitHit = true`

### Configuration (application.yml)
```yaml
ratelimits:
  default: 100          # Default: 100 requests/second
  map:
    client-service: 50  # client-service: 50 req/sec
    orders-service: 120 # orders-service: 120 req/sec
```

### Flow
```
Request → Check tokens → tokens > 0?
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
          Consume token              rateLimitHit = true
          rateLimitHit = false       (request continues)
              │                         │
              └────────────┬────────────┘
                           ▼
                   Process & Log Request
```

## 🔐 JWT Authentication

- **Register**: POST /auth/register
- **Login**: POST /auth/login → Returns JWT token
- Protected endpoints require `Authorization: Bearer <token>` header

## 🚨 Alert Rules

Alerts are automatically generated when:

| Condition | Alert Type |
|-----------|------------|
| latencyMs > 500 | HIGH_LATENCY |
| statusCode >= 500 | SERVER_ERROR |
| rateLimitHit = true | RATE_LIMIT |

## 🔒 Concurrency Safety (Optimistic Locking)

The Issue entity uses `@Version` for optimistic locking:

```kotlin
@Document(collection = "issues")
data class Issue(
    // ...
    @Version
    val version: Long? = null
)
```

When two users try to update the same issue:
1. Both read issue with version=1
2. First update succeeds, version becomes 2
3. Second update fails with `OptimisticLockingFailureException`
4. User must refresh and retry

## 🚀 Quick Start

### Prerequisites
- Java 17+
- MongoDB running on localhost:27017
- Node.js 18+

### 1. Start MongoDB
```bash
mongod
```

### 2. Start Collector Service
```powershell
cd collector-service
$env:SPRING_PROFILES_ACTIVE="dev"
.\gradlew.bat bootRun
```

### 3. Start Client Service (optional)
```powershell
cd client-service
.\gradlew.bat bootRun
```

### 4. Start Frontend
```powershell
cd frontend
npm install
npm run dev
```

### 5. Open Dashboard
- URL: http://localhost:3000
- Login: `admin / admin123`

## 📊 API Endpoints

### Authentication
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /auth/register | No | Register user |
| POST | /auth/login | No | Login, get JWT |

### Logs & Monitoring
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /collect/log | No | Receive log from services |
| GET | /logs | JWT | Get logs with filters |
| GET | /alerts | JWT | Get alerts |
| GET | /stats/dashboard | JWT | Get dashboard statistics |

### Issues
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /issues | JWT | Get all issues |
| POST | /issues/create | JWT | Create issue |
| POST | /issues/resolve | JWT+Admin | Resolve issue |

### Rate Limit Config (Runtime Overrides)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /ratelimit/configs | JWT | Get all rate limit configs |
| GET | /ratelimit/configs/{service} | JWT | Get config for service |
| POST | /ratelimit/configs | JWT+Admin | Create/update config |
| DELETE | /ratelimit/configs/{service} | JWT+Admin | Delete config |
| GET | /ratelimit/status/{service} | JWT | Get current rate limit status |

## 🧪 Testing

### Send Test Logs
```powershell
# Normal log
curl -X POST http://localhost:8081/collect/log `
  -H "Content-Type: application/json" `
  -d '{"endpoint":"/test","method":"GET","timestamp":"2025-01-01T00:00:00Z","requestSize":0,"responseSize":10,"statusCode":200,"latencyMs":50,"serviceName":"client-service","rateLimitHit":false}'

# Slow log (triggers alert)
curl -X POST http://localhost:8081/collect/log `
  -H "Content-Type: application/json" `
  -d '{"endpoint":"/slow","method":"GET","timestamp":"2025-01-01T00:00:00Z","requestSize":0,"responseSize":10,"statusCode":200,"latencyMs":750,"serviceName":"client-service","rateLimitHit":false}'

# Error log (triggers alert)
curl -X POST http://localhost:8081/collect/log `
  -H "Content-Type: application/json" `
  -d '{"endpoint":"/error","method":"GET","timestamp":"2025-01-01T00:00:00Z","requestSize":0,"responseSize":0,"statusCode":500,"latencyMs":50,"serviceName":"client-service","rateLimitHit":false}'
```

### Test Rate Limiting
Send more than 50 requests in 1 second for client-service to trigger rate limit alerts.

## ⚡ Concurrent Writes (50+ Requests)

The system handles 50+ concurrent log writes through:

1. **Async WebClient**: Client-service sends logs asynchronously (fire-and-forget)
2. **Non-blocking I/O**: WebClient uses Netty for high throughput
3. **Thread-safe Rate Limiter**: Uses AtomicInteger/AtomicLong for lock-free concurrency
4. **MongoDB Connection Pool**: Default 100 connections handle concurrent writes
5. **No Transaction Overhead**: Log writes don't require transactions

```
50 Concurrent Requests → WebClient (async) → Collector Service
                                                    ↓
                         MongoDB Connection Pool (100 connections)
                                                    ↓
                              logsdb.logs (bulk insert capable)
```

## 🔧 Rate Limit Config Overrides

Rate limits can be configured at runtime via database (takes priority over YAML):

```bash
# Create/update config for orders-service
curl -X POST http://localhost:8081/ratelimit/configs \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "orders-service", "limit": 120}'

# Check current status
curl http://localhost:8081/ratelimit/status/orders-service \
  -H "Authorization: Bearer <token>"
```

Priority order:
1. **Database config** (metadatadb.rate_limit_configs) - highest priority
2. **YAML service-specific** (ratelimits.map.service-name)
3. **YAML default** (ratelimits.default)

## 📝 Design Decisions

1. **Dual MongoDB**: Separates high-volume logs from metadata for easier management and different retention policies.

2. **Token Bucket Rate Limiter**: Allows bursts while maintaining average rate, non-blocking design.

3. **Optimistic Locking**: Prevents lost updates without blocking, better for low-contention scenarios.

4. **JWT + BCrypt**: Industry-standard secure authentication.

5. **Spring Interceptor**: Transparent API tracking without modifying business code.

6. **Next.js + Tailwind**: Modern, performant frontend with great DX.

7. **Database Rate Limit Overrides**: Allows runtime configuration without service restart.

## Build by - Aditya kashyap

API Monitoring & Observability Platform 



