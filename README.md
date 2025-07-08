### Run docker container
- docker pull xiaoyifoo/transaction-service:latest
- docker compose up

**Access the API**
- API Base URL: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`

# e-Banking Transaction Service
A microservice for retrieving paginated transaction lists with exchange rate conversion for authenticated e-Banking users.

## Overview

This Spring Boot microservice provides a REST API for retrieving paginated transaction lists from Kafka, with automatic exchange rate conversion. The service is designed to handle high-volume transaction data for e-Banking customers with multi-currency support.

### Key Features

- **REST API**: Paginated transaction retrieval with filtering by month/year
- **Kafka Integration**: Real-time transaction consumption from Kafka topics
- **Multi-Currency Support**: Automatic exchange rate conversion via external API
- **JWT Authentication**: Secure API access with JWT token validation
- **OpenAPI Documentation**: Comprehensive Swagger/OpenAPI documentation
- **Monitoring**: Health checks, metrics, and logging
- **Containerization**: Docker support with Kubernetes deployment manifests

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** (JWT authentication)
- **Spring Kafka** (message consumption)
- **Springdoc OpenAPI** (API documentation)
- **Lombok** (boilerplate reduction)
- **Docker** (containerization)
- **Kubernetes** (orchestration)

## Project Structure

```
src/
├── main/
│   ├── java/com/ebanking/
│   │   ├── config/           # Configuration classes
│   │   ├── controller/       # REST controllers
│   │   ├── domain/          # Domain models
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── exception/       # Custom exceptions
│   │   ├── service/         # Business logic
│   │   ├── security/        # Security configuration
│   │   └── TransactionServiceApplication.java
│   └── resources/
│       ├── application.yml  # Application configuration
│       └── logback-spring.xml
├── test/                    # Test classes
└── docker/                  # Docker configuration
```

## API Documentation

### Authentication

Transaction endpoints require JWT authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

### Endpoints


#### Generate Token

```
GET /api/token/generate
```

**Query Parameters:**
- `customerId` (required): string

**Example Request:**
```bash
curl -X 'POST' \
  'http://localhost:8080/api/token/generate' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "customerId": "sherry"
}'
```

**Example Response:**
```json
{
   "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzaGVycnkiLCJpYXQiOjE3NTE5NjQzNTYsImV4cCI6MTc1MjA1MDc1Nn0.tj0cdpOV80g4HYuiMwiHmBvgb-iLg2JULTENa1YSjV0"
}
```


#### Get Transactions

```
GET /api/v1/transactions
```

**Query Parameters:**
- `month` (required): Calendar month (1-12)
- `year` (required): Calendar year (2014-2030)
- `page` (optional): Page number (0-based, default: 0)
- `size` (optional): Page size (1-100, default: 20)
- `baseCurrency` (optional): Base currency for totals (default: GBP)

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/transactions?month=10&year=2020&page=0&size=20" \
  -H "Authorization: Bearer <jwt-token>"
```

**Example Response:**
```json
{
  "transactions": [
    {
      "id": "89d3o179-abcd-465b-o9ee-e2d5f6ofEld46",
      "amount": 100.00,
      "currency": "GBP",
      "accountIban": "CH93-0000-0000-0000-0000-0",
      "valueDate": "2020-10-01",
      "description": "Online payment CHF"
    }
  ],
  "totalCredit": 0,
  "totalDebit": 100.00,
  "baseCurrency": "GBP",
  "page": 0,
  "size": 20,
  "totalPages": 5,
  "totalElements": 100,
  "first": true,
  "last": false
}
```

## Configuration

### Application Properties

Key configuration properties in `application.yml`:

```yaml
# Kafka Configuration
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: transaction-service-group

# Exchange Rate API
app:
  exchange-rate:
    api:
      base-url: https://api.exchangerate-api.com/v4/latest
      timeout: 5000

# Security
app:
  security:
    jwt:
      secret: ${JWT_SECRET:your-secret-key}
      expiration: 86400000
```

### Environment Variables

- `JWT_SECRET`: Secret key for JWT token validation
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers
- `EXCHANGE_RATE_API_URL`: External exchange rate API URL

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### API Contract Tests
```bash
mvn test -Dtest=ContractTest
```

### Logging

The application uses structured logging with the following levels:
- `DEBUG`: Detailed debugging information
- `INFO`: General application information
- `WARN`: Warning messages
- `ERROR`: Error messages

## Deployment

### Kubernetes

1. **Apply Kubernetes manifests**
   ```bash
   kubectl apply -f k8s/
   ```

2. **Check deployment status**
   ```bash
   kubectl get pods -l app=transaction-service
   ```

### OpenShift

1. **Deploy to OpenShift**
   ```bash
   oc apply -f openshift/
   ```

-cd C:\Users\fxiao\OneDrive\Desktop\e-Banking
- open Docker Desktop
- docker-compose up -d
- run this spring project


docker build -t sherry/transaction-service:latest .
docker compose up
- docker run --rm -e SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 -e JWT_SECRET=your-secret-key-here-make-it-long-and-secure-in-production -p 8080:8080 sherry/transaction-service:latest


e-banking2 is to deploy locally
- docker compose up
- start this application