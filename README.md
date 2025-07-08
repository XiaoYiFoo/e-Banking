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

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker (optional)
- Kafka (for production-like testing)

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd e-Banking
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the API**
   - API Base URL: `http://localhost:8080/api/v1`
   - Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
   - Health Check: `http://localhost:8080/api/v1/actuator/health`

### Docker

1. **Build the Docker image**
   ```bash
   docker build -t ebanking-transaction-service .
   ```

2. **Run the container**
   ```bash
   docker run -p 8080:8080 ebanking-transaction-service
   ```

## API Documentation

### Authentication

All API endpoints require JWT authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

### Endpoints

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
  "totalCredit": 5000.00,
  "totalDebit": 3000.00,
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

## Monitoring and Health Checks

### Health Endpoints
- `/actuator/health`: Application health status
- `/actuator/info`: Application information
- `/actuator/metrics`: Application metrics
- `/actuator/prometheus`: Prometheus metrics

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

## Security Considerations

- JWT tokens are validated on every request
- Sensitive configuration is externalized via environment variables
- API endpoints are rate-limited
- Input validation is enforced on all parameters
- HTTPS is required in production

## Performance Considerations

- Kafka consumer concurrency is configurable
- Pagination limits prevent excessive data retrieval
- Exchange rate caching reduces external API calls
- Connection pooling for external services

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please contact:
- Email: dev@ebanking.com
- Documentation: [API Documentation](http://localhost:8080/api/v1/swagger-ui.html) 

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