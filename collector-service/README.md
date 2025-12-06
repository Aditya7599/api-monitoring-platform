# Collector Service

Spring Boot Kotlin service for collecting API logs, managing alerts, and issues.

## Features

- **Log Collection**: Receive logs from client services via POST /collect/log
- **Alerts**: Automatic alert generation for high latency, errors, and rate limits
- **Issues**: Issue tracking with optimistic locking
- **JWT Authentication**: Secure endpoints with JWT tokens
- **Rate Limiting**: Token bucket rate limiting per service

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Secret key for JWT signing (min 32 chars) | dev-only default |
| `SPRING_PROFILES_ACTIVE` | Active profile (dev creates admin user) | none |

### application.yml

```yaml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key}
  issuer: collector-service
  expiresInSec: 3600

mongodb:
  logs:
    uri: mongodb://localhost:27017/logsdb
  metadata:
    uri: mongodb://localhost:27017/metadatadb
```

## JWT Authentication

### Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/auth/register` | POST | No | Register new user |
| `/auth/login` | POST | No | Login and get JWT token |
| `/collect/log` | POST | No | Receive logs (service-to-service) |
| `/logs` | GET | Yes | Get recent logs |
| `/alerts` | GET | Yes | Get recent alerts |
| `/issues` | GET | Yes | Get issues |
| `/issues` | POST | Yes | Create issue |
| `/issues/resolve` | POST | Yes (ADMIN) | Resolve issue |

### Sample curl Commands

#### 1. Register a new user

```powershell
curl -X POST http://localhost:8081/auth/register `
  -H "Content-Type: application/json" `
  -d '{"username":"john","password":"secret123"}'
```

Response:
```json
{
  "id": "abc123",
  "username": "john",
  "message": "User registered successfully"
}
```

#### 2. Login and get JWT token

```powershell
curl -X POST http://localhost:8081/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"john","password":"secret123"}'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

#### 3. Access protected endpoint with token

```powershell
$token = "eyJhbGciOiJIUzI1NiJ9..."

curl -X GET http://localhost:8081/alerts `
  -H "Authorization: Bearer $token"
```

#### 4. Access without token (returns 401)

```powershell
curl -X GET http://localhost:8081/alerts
```

Response: `401 Unauthorized`

## Development

### Run with dev profile (creates admin user)

```powershell
cd collector-service
$env:SPRING_PROFILES_ACTIVE="dev"
.\gradlew bootRun
```

Default admin credentials (dev only):
- Username: `admin`
- Password: `admin123`
- Roles: `ROLE_USER`, `ROLE_ADMIN`

### Production

```powershell
$env:JWT_SECRET="your-very-long-secret-key-at-least-32-characters"
.\gradlew bootRun
```

## Databases

Uses two MongoDB databases on the same instance:

- **logsdb**: API logs (high volume)
- **metadatadb**: Alerts, issues, users (low volume)




