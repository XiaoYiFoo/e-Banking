# e-Banking Transaction Service - Manual Testing Guide

This guide provides step-by-step instructions for manually testing all components of the e-Banking Transaction Service.

## 🎯 Testing Overview

### What We're Testing:
1. **Local Development** - Spring Boot application
2. **Kafka Integration** - Message production and consumption
3. **Docker Containerization** - Build and run
4. **Kubernetes Deployment** - Full cluster deployment
5. **API Endpoints** - REST API functionality
6. **Security** - JWT authentication
7. **Monitoring** - Health checks and metrics

## 🚀 Prerequisites

### Required Tools:
- **Java 17+**
- **Maven 3.6+**
- **Docker** (for containerization)
- **kubectl** (for Kubernetes)
- **curl** (for API testing)
- **Web browser** (for Swagger UI)

### Optional Tools:
- **Postman** (for API testing)
- **Kafka UI** (for Kafka management)
- **Prometheus** (for metrics)

## 📋 Testing Checklist

- [ ] Local development setup
- [ ] Kafka infrastructure
- [ ] Application startup
- [ ] Health endpoints
- [ ] Swagger UI
- [ ] Test endpoints
- [ ] Kafka integration
- [ ] Main API endpoints
- [ ] Docker build and run
- [ ] Kubernetes deployment
- [ ] Performance testing
- [ ] Security testing
- [ ] Monitoring verification

---

## 🏠 Phase 1: Local Development Testing

### Step 1: Start Kafka Infrastructure

```bash
# Start Kafka using Docker Compose
docker-compose up -d

# Verify Kafka is running
docker ps | grep kafka
docker ps | grep zookeeper

# Check Kafka UI (optional)
# Open http://localhost:8081 in browser
```

**Expected Result:**
- Kafka and Zookeeper containers are running
- Kafka UI accessible at http://localhost:8081

### Step 2: Start the Application

```bash
# Build and start the application
mvn spring-boot:run

# Wait for startup (look for "Started TransactionServiceApplication")
```

**Expected Result:**
- Application starts without errors
- Logs show Kafka connection established
- Application listening on port 8080

### Step 3: Test Health Endpoints

```bash
# Test main health endpoint
curl -X GET http://localhost:8080/actuator/health

# Test test health endpoint
curl -X GET http://localhost:8080/test/health
```

**Expected Result:**
```json
{
  "status": "UP",
  "components": {
    "kafka": {
      "status": "UP"
    }
  }
}
```

### Step 4: Test Swagger UI

1. Open browser and go to: `http://localhost:8080/swagger-ui/`
2. Verify all endpoints are visible
3. Test the `/test/health` endpoint using Swagger UI

**Expected Result:**
- Swagger UI loads successfully
- All endpoints are documented
- Test endpoint returns 200 OK

---

## 🧪 Phase 2: Test Endpoints Testing

### Step 1: Test Service Information

```bash
curl -X GET http://localhost:8080/test/info
```

**Expected Result:**
```json
{
  "service": "e-Banking Transaction Service",
  "version": "1.0.0",
  "description": "REST API microservice for e-Banking portal transactions",
  "features": ["JWT Authentication", "Transaction Management", ...],
  "endpoints": [...]
}
```

### Step 2: Add Test Transactions

```bash
# Add 5 test transactions
curl -X POST "http://localhost:8080/test/transactions?customerId=customer123&count=5"
```

**Expected Result:**
```json
{
  "message": "Test transactions added successfully",
  "customerId": "customer123",
  "count": 5,
  "successCount": 5,
  "failedCount": 0,
  "transaction_1": {...},
  "transaction_2": {...}
}
```

### Step 3: Check Transaction Count

```bash
curl -X GET "http://localhost:8080/test/transactions/count?customerId=customer123"
```

**Expected Result:**
```json
{
  "customerId": "customer123",
  "totalTransactions": 5
}
```

### Step 4: Add Bulk Transactions

```bash
curl -X POST "http://localhost:8080/test/transactions/bulk?customerId=customer123"
```

**Expected Result:**
```json
{
  "message": "Bulk test transactions added successfully",
  "customerId": "customer123",
  "totalTransactions": 10,
  "successCount": 10,
  "failedCount": 0
}
```

---

## 🔄 Phase 3: Kafka Integration Testing

### Step 1: Send Transactions to Kafka

```bash
# Send 3 transactions to Kafka
curl -X POST "http://localhost:8080/test/kafka/send?customerId=customer123&count=3"
```

**Expected Result:**
```json
{
  "message": "Transactions sent to Kafka successfully",
  "customerId": "customer123",
  "count": 3,
  "topic": "transactions",
  "note": "Transactions will be consumed by the Kafka consumer and stored in the service"
}
```

### Step 2: Monitor Application Logs

Watch the application logs for Kafka messages:

```bash
# In another terminal, watch the logs
tail -f logs/application.log
```

**Expected Logs:**
```
INFO  - Sending 3 test transactions for customer: customer123
INFO  - Sending transaction to Kafka: [transaction-id]
INFO  - Transaction sent successfully to Kafka: [transaction-id] at offset: [offset]
INFO  - Received transaction from Kafka - Topic: transactions, Partition: 0, Offset: [offset]
INFO  - Successfully processed transaction: [transaction-id]
```

### Step 3: Verify Kafka Consumption

```bash
# Wait 10 seconds for processing, then check count
sleep 10
curl -X GET "http://localhost:8080/test/transactions/count?customerId=customer123"
```

**Expected Result:**
- Transaction count should increase by the number sent to Kafka

### Step 4: Test Single Transaction to Kafka

```bash
curl -X POST "http://localhost:8080/test/kafka/send-single?customerId=customer123&amount=1000.00&currency=USD&description=Test payment"
```

**Expected Result:**
```json
{
  "message": "Single transaction sent to Kafka successfully",
  "transactionId": "[generated-id]",
  "customerId": "customer123",
  "amount": 1000.00,
  "currency": "USD",
  "description": "Test payment"
}
```

---

## 🔐 Phase 4: API Endpoints Testing

### Step 1: Generate JWT Token

For testing purposes, you can use a simple JWT token:

```bash
# This is a test token - in production, get from auth service
JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJjdXN0b21lcklkIjoiY3VzdG9tZXIxMjMiLCJpYXQiOjE2MTYyMzkwMjIsImV4cCI6MTkzMTgxNTAyMn0.test-signature"
```

### Step 2: Test Transaction Retrieval API

```bash
curl -X GET \
  "http://localhost:8080/api/v1/transactions?customerId=customer123&page=0&size=10&month=10&year=2020&baseCurrency=USD" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result:**
```json
{
  "transactions": [...],
  "totalCredit": 1500.00,
  "totalDebit": 500.00,
  "baseCurrency": "USD",
  "page": 0,
  "size": 10,
  "totalPages": 1,
  "totalElements": 15,
  "first": true,
  "last": true
}
```

### Step 3: Test Pagination

```bash
# Test second page
curl -X GET \
  "http://localhost:8080/api/v1/transactions?customerId=customer123&page=1&size=5&month=10&year=2020&baseCurrency=EUR" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Step 4: Test Different Currencies

```bash
# Test with different base currency
curl -X GET \
  "http://localhost:8080/api/v1/transactions?customerId=customer123&page=0&size=10&month=10&year=2020&baseCurrency=CHF" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

## 🐳 Phase 5: Docker Testing

### Step 1: Build Docker Image

```bash
# Build the Docker image
docker build -t ebanking/transaction-service:test .

# Verify image was created
docker images | grep ebanking
```

**Expected Result:**
- Image builds successfully
- Image appears in Docker images list

### Step 2: Run Docker Container

```bash
# Run container with port mapping
docker run -d -p 8082:8080 --name ebanking-test ebanking/transaction-service:test

# Check container status
docker ps | grep ebanking-test
```

**Expected Result:**
- Container starts successfully
- Container appears in running containers list

### Step 3: Test Container Health

```bash
# Wait for container to be ready
sleep 10

# Test health endpoint
curl -X GET http://localhost:8082/actuator/health
```

**Expected Result:**
- Health endpoint returns UP status

### Step 4: Test Container Functionality

```bash
# Test basic functionality
curl -X GET http://localhost:8082/test/health
curl -X POST "http://localhost:8082/test/transactions?customerId=test&count=2"
```

### Step 5: Clean Up Container

```bash
# Stop and remove container
docker stop ebanking-test
docker rm ebanking-test
```

---

## ☸️ Phase 6: Kubernetes Testing

### Prerequisites Check

```bash
# Check kubectl
kubectl version --client

# Check cluster connection
kubectl cluster-info

# Check if you have a cluster running (minikube, Docker Desktop, etc.)
```

### Step 1: Deploy to Kubernetes

```bash
# Make deployment script executable
chmod +x k8s/deploy.sh

# Deploy everything
./k8s/deploy.sh deploy
```

**Expected Result:**
- All resources created successfully
- Pods are running

### Step 2: Check Deployment Status

```bash
# Check namespaces
kubectl get namespaces | grep ebanking
kubectl get namespaces | grep kafka

# Check pods
kubectl get pods -n ebanking
kubectl get pods -n kafka

# Check services
kubectl get services -n ebanking
kubectl get services -n kafka
```

### Step 3: Test Kubernetes Deployment

```bash
# Port forward to access the service
kubectl port-forward svc/transaction-service 8083:80 -n ebanking

# In another terminal, test the service
curl -X GET http://localhost:8083/test/health
```

### Step 4: Check Logs

```bash
# Check application logs
kubectl logs -l app=ebanking-transaction-service -n ebanking

# Check Kafka logs
kubectl logs -l app=kafka -n kafka
```

### Step 5: Test Scaling

```bash
# Check HPA status
kubectl get hpa -n ebanking

# Scale manually (for testing)
kubectl scale deployment transaction-service --replicas=5 -n ebanking

# Check pods
kubectl get pods -n ebanking
```

---

## 📊 Phase 7: Performance Testing

### Step 1: Basic Load Test

```bash
# Test with 10 concurrent requests
for i in {1..10}; do
  curl -s "http://localhost:8080/test/health" &
done
wait
```

### Step 2: Transaction Load Test

```bash
# Add multiple transactions concurrently
for i in {1..20}; do
  curl -s -X POST "http://localhost:8080/test/transactions?customerId=loadtest&count=1" &
done
wait
```

### Step 3: Kafka Load Test

```bash
# Send multiple transactions to Kafka
for i in {1..10}; do
  curl -s -X POST "http://localhost:8080/test/kafka/send?customerId=loadtest&count=2" &
done
wait
```

### Step 4: Monitor Performance

```bash
# Check metrics endpoint
curl -X GET http://localhost:8080/actuator/metrics

# Check specific metrics
curl -X GET http://localhost:8080/actuator/metrics/http.server.requests
```

---

## 🔒 Phase 8: Security Testing

### Step 1: Test Without Authentication

```bash
# Try to access protected endpoint without token
curl -X GET "http://localhost:8080/api/v1/transactions?customerId=customer123"
```

**Expected Result:**
- Returns 401 Unauthorized

### Step 2: Test With Invalid Token

```bash
# Try with invalid token
curl -X GET \
  "http://localhost:8080/api/v1/transactions?customerId=customer123" \
  -H "Authorization: Bearer invalid-token"
```

**Expected Result:**
- Returns 401 Unauthorized

### Step 3: Test With Valid Token

```bash
# Test with valid token
curl -X GET \
  "http://localhost:8080/api/v1/transactions?customerId=customer123" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result:**
- Returns 200 OK with data

---

## 📈 Phase 9: Monitoring Testing

### Step 1: Check Health Endpoints

```bash
# Check main health
curl -X GET http://localhost:8080/actuator/health

# Check health details
curl -X GET http://localhost:8080/actuator/health -H "Authorization: Bearer $JWT_TOKEN"
```

### Step 2: Check Metrics

```bash
# Check Prometheus metrics
curl -X GET http://localhost:8080/actuator/prometheus

# Check specific metrics
curl -X GET http://localhost:8080/actuator/metrics/jvm.memory.used
curl -X GET http://localhost:8080/actuator/metrics/http.server.requests
```

### Step 3: Check Info Endpoint

```bash
curl -X GET http://localhost:8080/actuator/info
```

---

## 🧹 Phase 10: Cleanup Testing

### Step 1: Clean Test Data

```bash
# Clear test transactions
curl -X DELETE "http://localhost:8080/test/transactions?customerId=customer123"
curl -X DELETE "http://localhost:8080/test/transactions?customerId=loadtest"
```

### Step 2: Verify Cleanup

```bash
# Check transaction counts
curl -X GET "http://localhost:8080/test/transactions/count?customerId=customer123"
curl -X GET "http://localhost:8080/test/transactions/count?customerId=loadtest"
```

**Expected Result:**
- Transaction counts should be 0

---

## 🎯 Test Results Summary

After completing all phases, you should have verified:

✅ **Local Development**
- Application starts successfully
- Health endpoints respond correctly
- Swagger UI is accessible

✅ **Kafka Integration**
- Messages are sent to Kafka successfully
- Messages are consumed and processed
- Transaction counts increase after Kafka processing

✅ **API Functionality**
- Test endpoints work correctly
- Main API endpoints require authentication
- Pagination works as expected
- Currency conversion works

✅ **Docker Containerization**
- Image builds successfully
- Container runs and responds to requests
- Health checks work in container

✅ **Kubernetes Deployment**
- All resources deploy successfully
- Pods are running and healthy
- Services are accessible
- Scaling works correctly

✅ **Security**
- Unauthenticated requests are rejected
- Invalid tokens are rejected
- Valid tokens allow access

✅ **Performance**
- Application handles concurrent requests
- Response times are acceptable
- No memory leaks or crashes

✅ **Monitoring**
- Health endpoints provide status
- Metrics are exposed correctly
- Logs contain useful information

---

## 🚨 Troubleshooting

### Common Issues:

1. **Kafka Connection Failed**
   - Check if Kafka is running: `docker ps | grep kafka`
   - Check application logs for connection errors
   - Verify Kafka bootstrap servers configuration

2. **Application Won't Start**
   - Check Java version: `java -version`
   - Check Maven: `mvn -version`
   - Check port 8080 is not in use: `netstat -an | grep 8080`

3. **Docker Build Fails**
   - Check Docker is running
   - Check disk space: `df -h`
   - Check Dockerfile syntax

4. **Kubernetes Deployment Fails**
   - Check cluster is running: `kubectl cluster-info`
   - Check resource limits
   - Check namespace exists: `kubectl get namespaces`

5. **API Returns 403/401**
   - Check JWT token is valid
   - Check token format: `Authorization: Bearer <token>`
   - Check customer ID in token matches request

### Debug Commands:

```bash
# Check application logs
tail -f logs/application.log

# Check Docker logs
docker logs <container-name>

# Check Kubernetes logs
kubectl logs <pod-name> -n <namespace>

# Check Kubernetes events
kubectl get events -n <namespace>

# Check resource usage
kubectl top pods -n <namespace>
```

---

## 📝 Test Report Template

After completing all tests, document your results:

### Test Environment:
- Date: _______________
- Tester: _______________
- Environment: Local/Docker/Kubernetes
- Version: _______________

### Test Results:
- [ ] Local Development: PASS/FAIL
- [ ] Kafka Integration: PASS/FAIL
- [ ] API Endpoints: PASS/FAIL
- [ ] Docker: PASS/FAIL
- [ ] Kubernetes: PASS/FAIL
- [ ] Security: PASS/FAIL
- [ ] Performance: PASS/FAIL
- [ ] Monitoring: PASS/FAIL

### Issues Found:
1. _______________
2. _______________
3. _______________

### Recommendations:
1. _______________
2. _______________
3. _______________

---

**Congratulations!** 🎉 You've successfully tested all components of the e-Banking Transaction Service. The application is ready for production deployment. 