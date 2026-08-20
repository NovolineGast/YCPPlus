# YCPPlus Admin Backend

Spring Boot REST API for YCPPlus license key management.

## Quick Start

```bash
# Build
mvn clean package

# Run
java -jar target/admin-api-1.0.0.jar

# Or with Maven
mvn spring-boot:run
```

## API Endpoints

### Authentication
- `POST /api/init` - Initialize admin (first time only)
- `POST /api/login` - Login and get JWT token

### Dashboard
- `GET /api/dashboard/stats` - Get statistics

### Key Management
- `GET /api/keys` - List all keys
- `POST /api/keys/generate` - Generate new keys
- `POST /api/keys/{key}/ban` - Ban a key
- `POST /api/keys/{key}/unban` - Unban a key
- `DELETE /api/keys/{key}` - Delete a key
- `GET /api/keys/{key}/fingerprint` - Get key SHA256 fingerprint

## Environment

- Java 17+
- Spring Boot 3.2.5
- SQLite database (auto-created in `data/` directory)
