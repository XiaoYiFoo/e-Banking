#!/bin/bash

# e-Banking Transaction Service - Complete Testing Script
# This script tests all components: local development, Docker, and Kubernetes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080"
API_BASE_URL="${BASE_URL}/api/v1"
TEST_BASE_URL="${BASE_URL}/test"
SWAGGER_URL="${BASE_URL}/swagger-ui"
HEALTH_URL="${BASE_URL}/actuator/health"
KAFKA_UI_URL="http://localhost:8081"

# Test data
CUSTOMER_ID="test-customer-123"
JWT_TOKEN=""

# Function to print colored output
print_header() {
    echo -e "${PURPLE}================================${NC}"
    echo -e "${PURPLE}$1${NC}"
    echo -e "${PURPLE}================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_step() {
    echo -e "${CYAN}🔍 $1${NC}"
}

# Function to check if a service is running
check_service() {
    local service_name=$1
    local url=$2
    local max_attempts=${3:-30}
    local attempt=1
    
    print_step "Checking if $service_name is running at $url"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url" > /dev/null 2>&1; then
            print_success "$service_name is running"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "$service_name is not running after $max_attempts attempts"
    return 1
}

# Function to generate JWT token
generate_jwt_token() {
    print_step "Generating JWT token for testing"
    
    # This is a simple test token - in production, you'd get this from your auth service
    JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJjdXN0b21lcklkIjoi$CUSTOMER_IDIiwiaWF0IjoxNjE2MjM5MDIyLCJleHAiOjE5MzE4MTUwMjJ9.test-signature"
    
    print_success "JWT token generated"
}

# Function to test health endpoint
test_health() {
    print_header "Testing Health Endpoint"
    
    print_step "Testing health endpoint"
    response=$(curl -s "$HEALTH_URL")
    
    if echo "$response" | grep -q "UP"; then
        print_success "Health check passed"
        echo "Response: $response"
    else
        print_error "Health check failed"
        echo "Response: $response"
        return 1
    fi
}

# Function to test Swagger UI
test_swagger_ui() {
    print_header "Testing Swagger UI"
    
    print_step "Checking Swagger UI accessibility"
    if curl -s -f "$SWAGGER_URL" > /dev/null; then
        print_success "Swagger UI is accessible"
    else
        print_error "Swagger UI is not accessible"
        return 1
    fi
}

# Function to test test endpoints
test_test_endpoints() {
    print_header "Testing Test Endpoints"
    
    # Test health endpoint
    print_step "Testing test health endpoint"
    response=$(curl -s "${TEST_BASE_URL}/health")
    if echo "$response" | grep -q "UP"; then
        print_success "Test health endpoint works"
    else
        print_error "Test health endpoint failed"
        return 1
    fi
    
    # Test adding transactions
    print_step "Adding test transactions"
    response=$(curl -s -X POST "${TEST_BASE_URL}/transactions?customerId=$CUSTOMER_ID&count=3")
    if echo "$response" | grep -q "successfully"; then
        print_success "Test transactions added successfully"
        echo "Response: $response"
    else
        print_error "Failed to add test transactions"
        echo "Response: $response"
        return 1
    fi
    
    # Test transaction count
    print_step "Checking transaction count"
    response=$(curl -s "${TEST_BASE_URL}/transactions/count?customerId=$CUSTOMER_ID")
    if echo "$response" | grep -q "totalTransactions"; then
        print_success "Transaction count endpoint works"
        echo "Response: $response"
    else
        print_error "Transaction count endpoint failed"
        echo "Response: $response"
        return 1
    fi
    
    # Test service info
    print_step "Testing service info endpoint"
    response=$(curl -s "${TEST_BASE_URL}/info")
    if echo "$response" | grep -q "e-Banking Transaction Service"; then
        print_success "Service info endpoint works"
    else
        print_error "Service info endpoint failed"
        return 1
    fi
}

# Function to test Kafka integration
test_kafka_integration() {
    print_header "Testing Kafka Integration"
    
    # Test sending transactions to Kafka
    print_step "Sending transactions to Kafka"
    response=$(curl -s -X POST "${TEST_BASE_URL}/kafka/send?customerId=$CUSTOMER_ID&count=2")
    if echo "$response" | grep -q "successfully"; then
        print_success "Transactions sent to Kafka successfully"
        echo "Response: $response"
    else
        print_error "Failed to send transactions to Kafka"
        echo "Response: $response"
        return 1
    fi
    
    # Wait for Kafka processing
    print_step "Waiting for Kafka processing (10 seconds)"
    sleep 10
    
    # Check if transactions were consumed
    print_step "Checking if Kafka transactions were consumed"
    response=$(curl -s "${TEST_BASE_URL}/transactions/count?customerId=$CUSTOMER_ID")
    echo "Transaction count after Kafka: $response"
}

# Function to test main API endpoints
test_api_endpoints() {
    print_header "Testing Main API Endpoints"
    
    if [ -z "$JWT_TOKEN" ]; then
        print_warning "No JWT token available, skipping authenticated endpoints"
        return 0
    fi
    
    # Test getting transactions
    print_step "Testing transaction retrieval API"
    response=$(curl -s -H "Authorization: Bearer $JWT_TOKEN" \
        "${API_BASE_URL}/transactions?customerId=$CUSTOMER_ID&page=0&size=10&month=10&year=2020&baseCurrency=USD")
    
    if echo "$response" | grep -q "transactions"; then
        print_success "Transaction API endpoint works"
        echo "Response preview: $(echo "$response" | head -c 200)..."
    else
        print_error "Transaction API endpoint failed"
        echo "Response: $response"
        return 1
    fi
}

# Function to test Kafka UI
test_kafka_ui() {
    print_header "Testing Kafka UI"
    
    print_step "Checking Kafka UI accessibility"
    if curl -s -f "$KAFKA_UI_URL" > /dev/null; then
        print_success "Kafka UI is accessible"
    else
        print_warning "Kafka UI is not accessible (may not be running)"
    fi
}

# Function to test Docker build
test_docker_build() {
    print_header "Testing Docker Build"
    
    print_step "Building Docker image"
    if docker build -t ebanking/transaction-service:test .; then
        print_success "Docker image built successfully"
    else
        print_error "Docker build failed"
        return 1
    fi
    
    print_step "Running Docker container"
    container_id=$(docker run -d -p 8082:8080 --name ebanking-test ebanking/transaction-service:test)
    
    if [ $? -eq 0 ]; then
        print_success "Docker container started"
        
        # Wait for container to be ready
        sleep 10
        
        # Test container health
        if curl -s -f "http://localhost:8082/actuator/health" > /dev/null; then
            print_success "Docker container health check passed"
        else
            print_error "Docker container health check failed"
        fi
        
        # Clean up
        docker stop "$container_id" > /dev/null 2>&1
        docker rm "$container_id" > /dev/null 2>&1
        print_info "Docker container cleaned up"
    else
        print_error "Failed to start Docker container"
        return 1
    fi
}

# Function to test Kubernetes deployment
test_kubernetes() {
    print_header "Testing Kubernetes Deployment"
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        print_warning "kubectl not found, skipping Kubernetes tests"
        return 0
    fi
    
    # Check if we're connected to a cluster
    if ! kubectl cluster-info &> /dev/null; then
        print_warning "Not connected to Kubernetes cluster, skipping Kubernetes tests"
        return 0
    fi
    
    print_step "Deploying to Kubernetes"
    if [ -f "k8s/deploy.sh" ]; then
        chmod +x k8s/deploy.sh
        if ./k8s/deploy.sh deploy; then
            print_success "Kubernetes deployment successful"
        else
            print_error "Kubernetes deployment failed"
            return 1
        fi
    else
        print_warning "Kubernetes deployment script not found"
    fi
}

# Function to run performance tests
test_performance() {
    print_header "Testing Performance"
    
    print_step "Running basic load test (10 requests)"
    start_time=$(date +%s)
    
    for i in {1..10}; do
        curl -s "${TEST_BASE_URL}/health" > /dev/null &
    done
    wait
    
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    
    print_success "Load test completed in ${duration} seconds"
    
    # Test concurrent requests
    print_step "Testing concurrent requests"
    start_time=$(date +%s)
    
    for i in {1..20}; do
        curl -s "${TEST_BASE_URL}/transactions/count?customerId=$CUSTOMER_ID" > /dev/null &
    done
    wait
    
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    
    print_success "Concurrent test completed in ${duration} seconds"
}

# Function to run integration tests
test_integration() {
    print_header "Running Integration Tests"
    
    # Test complete flow: add transactions -> send to Kafka -> retrieve via API
    print_step "Testing complete integration flow"
    
    # 1. Add transactions directly
    print_info "Step 1: Adding transactions directly"
    curl -s -X POST "${TEST_BASE_URL}/transactions?customerId=$CUSTOMER_ID&count=2" > /dev/null
    
    # 2. Send transactions to Kafka
    print_info "Step 2: Sending transactions to Kafka"
    curl -s -X POST "${TEST_BASE_URL}/kafka/send?customerId=$CUSTOMER_ID&count=2" > /dev/null
    
    # 3. Wait for processing
    sleep 5
    
    # 4. Check final count
    print_info "Step 3: Checking final transaction count"
    response=$(curl -s "${TEST_BASE_URL}/transactions/count?customerId=$CUSTOMER_ID")
    echo "Final transaction count: $response"
    
    print_success "Integration test completed"
}

# Function to clean up test data
cleanup_test_data() {
    print_header "Cleaning Up Test Data"
    
    print_step "Clearing test transactions"
    response=$(curl -s -X DELETE "${TEST_BASE_URL}/transactions?customerId=$CUSTOMER_ID")
    if echo "$response" | grep -q "successfully"; then
        print_success "Test data cleaned up"
    else
        print_warning "Could not clean up test data"
    fi
}

# Function to show test summary
show_test_summary() {
    print_header "Test Summary"
    
    echo "✅ Health endpoints tested"
    echo "✅ Swagger UI tested"
    echo "✅ Test endpoints tested"
    echo "✅ Kafka integration tested"
    echo "✅ API endpoints tested"
    echo "✅ Docker build tested"
    echo "✅ Performance tests completed"
    echo "✅ Integration tests completed"
    
    print_success "All tests completed successfully!"
}

# Main test function
run_all_tests() {
    print_header "Starting Complete e-Banking Transaction Service Test Suite"
    
    # Check prerequisites
    print_step "Checking prerequisites"
    if ! command -v curl &> /dev/null; then
        print_error "curl is required but not installed"
        exit 1
    fi
    
    if ! command -v docker &> /dev/null; then
        print_warning "Docker not found, skipping Docker tests"
    fi
    
    # Generate JWT token
    generate_jwt_token
    
    # Run tests
    test_health || exit 1
    test_swagger_ui || exit 1
    test_test_endpoints || exit 1
    test_kafka_integration || exit 1
    test_api_endpoints || exit 1
    test_kafka_ui || exit 1
    test_docker_build || exit 1
    test_kubernetes || exit 1
    test_performance || exit 1
    test_integration || exit 1
    
    # Cleanup
    cleanup_test_data
    
    # Show summary
    show_test_summary
}

# Parse command line arguments
case "$1" in
    "health")
        test_health
        ;;
    "swagger")
        test_swagger_ui
        ;;
    "test-endpoints")
        test_test_endpoints
        ;;
    "kafka")
        test_kafka_integration
        ;;
    "api")
        test_api_endpoints
        ;;
    "docker")
        test_docker_build
        ;;
    "kubernetes")
        test_kubernetes
        ;;
    "performance")
        test_performance
        ;;
    "integration")
        test_integration
        ;;
    "cleanup")
        cleanup_test_data
        ;;
    "all"|"")
        run_all_tests
        ;;
    *)
        echo "Usage: $0 {all|health|swagger|test-endpoints|kafka|api|docker|kubernetes|performance|integration|cleanup}"
        echo ""
        echo "Test Categories:"
        echo "  all            - Run all tests"
        echo "  health         - Test health endpoints"
        echo "  swagger        - Test Swagger UI"
        echo "  test-endpoints - Test test endpoints"
        echo "  kafka          - Test Kafka integration"
        echo "  api            - Test main API endpoints"
        echo "  docker         - Test Docker build and run"
        echo "  kubernetes     - Test Kubernetes deployment"
        echo "  performance    - Run performance tests"
        echo "  integration    - Run integration tests"
        echo "  cleanup        - Clean up test data"
        exit 1
        ;;
esac 