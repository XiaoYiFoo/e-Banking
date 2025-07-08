# e-Banking Transaction Service - Testing Guide

This directory contains comprehensive testing tools for the e-Banking Transaction Service, including automated scripts, manual testing guides, and Postman collections.

## 📁 Test Files

- **`test-everything.sh`** - Automated test script for all components
- **`manual-test-guide.md`** - Step-by-step manual testing instructions
- **`postman-collection.json`** - Postman collection for API testing
- **`README.md`** - This file

## Testing Options for Different Operating Systems

### Windows Users
Since you're on Windows, you have several testing options:

1. **PowerShell Script** (Recommended)
   - File: `test-everything.ps1`
   - Run with: `powershell -ExecutionPolicy Bypass -File test-everything.ps1`
   - Features: Full functionality, colored output, detailed logging

2. **Batch File** (Simple)
   - File: `test-everything.bat`
   - Run with: `test-everything.bat` or `test-everything.bat all`
   - Features: Basic functionality, works with any Windows version

3. **WSL/Git Bash** (Advanced)
   - Install WSL or Git Bash to run the original `.sh` script
   - File: `test-everything.sh`
   - Run with: `bash test-everything.sh`

### Linux/Mac Users
- Use the bash script: `./test-everything.sh`

## 🚀 Quick Start Testing

### Option 1: Automated Testing (Recommended)

```bash
# Make the test script executable
chmod +x test/test-everything.sh

# Run all tests
./test/test-everything.sh all

# Run specific test categories
./test/test-everything.sh health
./test/test-everything.sh kafka
./test/test-everything.sh api
./test/test-everything.sh docker
./test/test-everything.sh kubernetes
```

### Option 2: Manual Testing

Follow the detailed instructions in `manual-test-guide.md` for step-by-step testing.

### Option 3: Postman Collection

1. Import `postman-collection.json` into Postman
2. Set up environment variables
3. Run the collection

## 🎯 What Gets Tested

### ✅ Application Components
- **Health Endpoints** - Service status and readiness
- **Swagger UI** - API documentation accessibility
- **Test Endpoints** - Data creation and management
- **Main API** - Transaction retrieval with authentication
- **Kafka Integration** - Message production and consumption

### ✅ Infrastructure
- **Docker** - Container build and runtime
- **Kubernetes** - Deployment and scaling
- **Kafka** - Message queue functionality
- **Monitoring** - Metrics and health checks

### ✅ Security
- **JWT Authentication** - Token validation
- **Authorization** - Access control
- **Input Validation** - Request sanitization

### ✅ Performance
- **Load Testing** - Concurrent request handling
- **Response Times** - API performance
- **Resource Usage** - Memory and CPU consumption

## 📋 Test Categories

### 1. Health & Status Tests
```bash
./test/test-everything.sh health
```
**Tests:**
- Application health endpoint
- Test health endpoint
- Service information endpoint

### 2. Swagger UI Tests
```bash
./test/test-everything.sh swagger
```
**Tests:**
- Swagger UI accessibility
- API documentation loading
- Interactive API testing

### 3. Test Endpoints
```bash
./test/test-everything.sh test-endpoints
```
**Tests:**
- Adding test transactions
- Bulk transaction creation
- Transaction counting
- Service information

### 4. Kafka Integration
```bash
./test/test-everything.sh kafka
```
**Tests:**
- Sending messages to Kafka
- Message consumption
- Transaction processing
- Kafka UI accessibility

### 5. Main API Endpoints
```bash
./test/test-everything.sh api
```
**Tests:**
- Authenticated API access
- Transaction retrieval
- Pagination
- Currency conversion
- Error handling

### 6. Docker Testing
```bash
./test/test-everything.sh docker
```
**Tests:**
- Docker image build
- Container startup
- Health checks in container
- Port mapping

### 7. Kubernetes Testing
```bash
./test/test-everything.sh kubernetes
```
**Tests:**
- Cluster connectivity
- Resource deployment
- Pod health
- Service accessibility
- Scaling functionality

### 8. Performance Testing
```bash
./test/test-everything.sh performance
```
**Tests:**
- Concurrent request handling
- Response time measurement
- Load testing
- Resource usage monitoring

### 9. Integration Testing
```bash
./test/test-everything.sh integration
```
**Tests:**
- End-to-end workflows
- Data flow validation
- System integration
- Error recovery

## 🧪 Test Scenarios

### Scenario 1: Basic Functionality
1. Start application
2. Test health endpoints
3. Add test transactions
4. Verify transaction count
5. Test API retrieval

### Scenario 2: Kafka Workflow
1. Send transactions to Kafka
2. Monitor application logs
3. Verify message consumption
4. Check transaction storage

### Scenario 3: Security Validation
1. Test without authentication
2. Test with invalid token
3. Test with valid token
4. Verify access control

### Scenario 4: Performance Validation
1. Run concurrent requests
2. Monitor response times
3. Check resource usage
4. Validate scalability

## 📊 Expected Results

### Health Endpoints
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

### Transaction API Response
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

### Test Endpoint Response
```json
{
  "message": "Test transactions added successfully",
  "customerId": "customer123",
  "count": 5,
  "successCount": 5,
  "failedCount": 0
}
```

## 🚨 Troubleshooting

### Common Issues

#### 1. Application Won't Start
```bash
# Check Java version
java -version

# Check port availability
netstat -an | grep 8080

# Check application logs
tail -f logs/application.log
```

#### 2. Kafka Connection Issues
```bash
# Check Kafka status
docker ps | grep kafka

# Check Kafka logs
docker logs kafka

# Test Kafka connectivity
telnet localhost 9092
```

#### 3. Docker Build Fails
```bash
# Check Docker status
docker info

# Check disk space
df -h

# Clean Docker cache
docker system prune
```

#### 4. Kubernetes Issues
```bash
# Check cluster status
kubectl cluster-info

# Check namespace
kubectl get namespaces

# Check pods
kubectl get pods -n ebanking
```

### Debug Commands

```bash
# Application logs
tail -f logs/application.log

# Docker logs
docker logs <container-name>

# Kubernetes logs
kubectl logs <pod-name> -n <namespace>

# Network connectivity
curl -v http://localhost:8080/actuator/health

# Kafka topic inspection
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

## 📈 Performance Benchmarks

### Expected Performance
- **Response Time**: < 500ms for simple requests
- **Throughput**: > 100 requests/second
- **Memory Usage**: < 1GB for normal operation
- **CPU Usage**: < 50% under normal load

### Load Testing
```bash
# Basic load test
for i in {1..100}; do
  curl -s http://localhost:8080/test/health &
done
wait

# Concurrent API test
for i in {1..50}; do
  curl -s -H "Authorization: Bearer $JWT_TOKEN" \
    "http://localhost:8080/api/v1/transactions?customerId=customer123" &
done
wait
```

## 🔒 Security Testing

### Authentication Tests
```bash
# Test without token
curl -X GET "http://localhost:8080/api/v1/transactions?customerId=customer123"

# Test with invalid token
curl -X GET "http://localhost:8080/api/v1/transactions?customerId=customer123" \
  -H "Authorization: Bearer invalid-token"

# Test with valid token
curl -X GET "http://localhost:8080/api/v1/transactions?customerId=customer123" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Expected Results
- **No Token**: 401 Unauthorized
- **Invalid Token**: 401 Unauthorized
- **Valid Token**: 200 OK with data

## 📝 Test Reporting

### Automated Test Report
The test script generates a summary report showing:
- ✅ Passed tests
- ❌ Failed tests
- ⚠️ Warnings
- 📊 Performance metrics

### Manual Test Checklist
Use the checklist in `manual-test-guide.md` to track:
- Test completion status
- Issues found
- Performance observations
- Recommendations

## 🎯 Success Criteria

A successful test run should demonstrate:

✅ **Functionality**
- All endpoints respond correctly
- Data is stored and retrieved properly
- Kafka messages are processed

✅ **Performance**
- Response times are acceptable
- System handles concurrent requests
- Resource usage is reasonable

✅ **Security**
- Authentication works correctly
- Unauthorized access is blocked
- Sensitive data is protected

✅ **Reliability**
- System recovers from errors
- Data consistency is maintained
- Logs provide useful information

✅ **Scalability**
- Docker containers work properly
- Kubernetes deployment succeeds
- Auto-scaling functions correctly

## 🚀 Next Steps

After successful testing:

1. **Deploy to Staging** - Use the same tests in staging environment
2. **Performance Tuning** - Optimize based on test results
3. **Security Hardening** - Address any security findings
4. **Production Deployment** - Deploy with confidence
5. **Monitoring Setup** - Configure production monitoring

## 📚 Additional Resources

- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [Kafka Testing Best Practices](https://kafka.apache.org/documentation/)
- [Docker Testing Strategies](https://docs.docker.com/develop/dev-best-practices/)
- [Kubernetes Testing](https://kubernetes.io/docs/tasks/debug-application-cluster/)

---

**Happy Testing!** 🎉 

If you encounter any issues during testing, refer to the troubleshooting section or check the application logs for detailed error information. 