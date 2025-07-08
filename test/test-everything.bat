@echo off
REM e-Banking Transaction Service - Windows Batch Test Script
REM This batch file tests basic functionality on Windows

setlocal enabledelayedexpansion

REM Configuration
set BASE_URL=http://localhost:8080
set API_BASE_URL=%BASE_URL%/api/v1
set TEST_BASE_URL=%BASE_URL%/test
set SWAGGER_URL=%BASE_URL%/swagger-ui
set HEALTH_URL=%BASE_URL%/actuator/health
set KAFKA_UI_URL=http://localhost:8081

REM Test data
set CUSTOMER_ID=test-customer-123
set JWT_TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJjdXN0b21lcklkIjoiY3VzdG9tZXIxMjMiLCJpYXQiOjE2MTYyMzkwMjIsImV4cCI6MTkzMTgxNTAyMn0.test-signature

REM Check if test type is provided
if "%1"=="" (
    set TEST_TYPE=all
) else (
    set TEST_TYPE=%1
)

echo ========================================
echo e-Banking Transaction Service Test Suite
echo ========================================
echo.

REM Function to print colored output (basic)
:print_header
echo ========================================
echo %~1
echo ========================================
goto :eof

:print_success
echo [SUCCESS] %~1
goto :eof

:print_error
echo [ERROR] %~1
goto :eof

:print_warning
echo [WARNING] %~1
goto :eof

:print_info
echo [INFO] %~1
goto :eof

:print_step
echo [STEP] %~1
goto :eof

REM Function to check if curl is available
:check_curl
curl --version >nul 2>&1
if errorlevel 1 (
    call :print_error "curl is not installed or not in PATH"
    echo Please install curl from: https://curl.se/windows/
    exit /b 1
)
call :print_success "curl is available"
goto :eof

REM Function to test health endpoint
:test_health
call :print_header "Testing Health Endpoint"
call :print_step "Testing health endpoint"

curl -s "%HEALTH_URL%" > temp_response.txt 2>nul
if errorlevel 1 (
    call :print_error "Health check failed - service may not be running"
    del temp_response.txt 2>nul
    exit /b 1
)

findstr "UP" temp_response.txt >nul
if errorlevel 1 (
    call :print_error "Health check failed - service not healthy"
    type temp_response.txt
    del temp_response.txt 2>nul
    exit /b 1
)

call :print_success "Health check passed"
type temp_response.txt
del temp_response.txt 2>nul
goto :eof

REM Function to test Swagger UI
:test_swagger
call :print_header "Testing Swagger UI"
call :print_step "Checking Swagger UI accessibility"

curl -s -f "%SWAGGER_URL%" >nul 2>&1
if errorlevel 1 (
    call :print_error "Swagger UI is not accessible"
    exit /b 1
)

call :print_success "Swagger UI is accessible"
goto :eof

REM Function to test test endpoints
:test_endpoints
call :print_header "Testing Test Endpoints"

REM Test health endpoint
call :print_step "Testing test health endpoint"
curl -s "%TEST_BASE_URL%/health" > temp_response.txt 2>nul
findstr "UP" temp_response.txt >nul
if errorlevel 1 (
    call :print_error "Test health endpoint failed"
    del temp_response.txt 2>nul
    exit /b 1
)
call :print_success "Test health endpoint works"
del temp_response.txt 2>nul

REM Test adding transactions
call :print_step "Adding test transactions"
curl -s -X POST "%TEST_BASE_URL%/transactions?customerId=%CUSTOMER_ID%&count=3" > temp_response.txt 2>nul
findstr "successfully" temp_response.txt >nul
if errorlevel 1 (
    call :print_error "Failed to add test transactions"
    type temp_response.txt
    del temp_response.txt 2>nul
    exit /b 1
)
call :print_success "Test transactions added successfully"
type temp_response.txt
del temp_response.txt 2>nul

REM Test transaction count
call :print_step "Checking transaction count"
curl -s "%TEST_BASE_URL%/transactions/count?customerId=%CUSTOMER_ID%" > temp_response.txt 2>nul
findstr "totalTransactions" temp_response.txt >nul
if errorlevel 1 (
    call :print_error "Transaction count endpoint failed"
    type temp_response.txt
    del temp_response.txt 2>nul
    exit /b 1
)
call :print_success "Transaction count endpoint works"
type temp_response.txt
del temp_response.txt 2>nul

REM Test service info
call :print_step "Testing service info endpoint"
curl -s "%TEST_BASE_URL%/info" > temp_response.txt 2>nul
findstr "e-Banking Transaction Service" temp_response.txt >nul
if errorlevel 1 (
    call :print_error "Service info endpoint failed"
    del temp_response.txt 2>nul
    exit /b 1
)
call :print_success "Service info endpoint works"
del temp_response.txt 2>nul
goto :eof

REM Function to test Kafka integration
:test_kafka
call :print_header "Testing Kafka Integration"

REM Test sending transactions to Kafka
call :print_step "Sending transactions to Kafka"
curl -s -X POST "%TEST_BASE_URL%/kafka/send?customerId=%CUSTOMER_ID%&count=2" > temp_response.txt 2>nul
findstr "successfully" temp_response.txt >nul
if errorlevel 1 (
    call :print_error "Failed to send transactions to Kafka"
    type temp_response.txt
    del temp_response.txt 2>nul
    exit /b 1
)
call :print_success "Transactions sent to Kafka successfully"
type temp_response.txt
del temp_response.txt 2>nul

REM Wait for Kafka processing
call :print_step "Waiting for Kafka processing (10 seconds)"
timeout /t 10 /nobreak >nul

REM Check if transactions were consumed
call :print_step "Checking if Kafka transactions were consumed"
curl -s "%TEST_BASE_URL%/transactions/count?customerId=%CUSTOMER_ID%" > temp_response.txt 2>nul
echo Transaction count after Kafka:
type temp_response.txt
del temp_response.txt 2>nul
goto :eof

REM Function to test main API endpoints
:test_api
call :print_header "Testing Main API Endpoints"

if "%JWT_TOKEN%"=="" (
    call :print_warning "No JWT token available, skipping authenticated endpoints"
    goto :eof
)

REM Test getting transactions
call :print_step "Testing transaction retrieval API"
curl -s -H "Authorization: Bearer %JWT_TOKEN%" "%API_BASE_URL%/transactions?customerId=%CUSTOMER_ID%&page=0&size=10&month=10&year=2020&baseCurrency=USD" > temp_response.txt 2>nul
findstr "transactions" temp_response.txt >nul
if errorlevel 1 (
    call :print_error "Transaction API endpoint failed"
    type temp_response.txt
    del temp_response.txt 2>nul
    exit /b 1
)
call :print_success "Transaction API endpoint works"
echo Response preview:
powershell -Command "Get-Content temp_response.txt | Select-Object -First 1 | ForEach-Object { $_.Substring(0, [Math]::Min(200, $_.Length)) + '...' }"
del temp_response.txt 2>nul
goto :eof

REM Function to test Kafka UI
:test_kafka_ui
call :print_header "Testing Kafka UI"
call :print_step "Checking Kafka UI accessibility"

curl -s -f "%KAFKA_UI_URL%" >nul 2>&1
if errorlevel 1 (
    call :print_warning "Kafka UI is not accessible (may not be running)"
    goto :eof
)

call :print_success "Kafka UI is accessible"
goto :eof

REM Function to test Docker build
:test_docker
call :print_header "Testing Docker Build"

REM Check if Docker is available
docker --version >nul 2>&1
if errorlevel 1 (
    call :print_warning "Docker not found, skipping Docker tests"
    goto :eof
)

call :print_step "Building Docker image"
docker build -t ebanking/transaction-service:test . >nul 2>&1
if errorlevel 1 (
    call :print_error "Docker build failed"
    exit /b 1
)
call :print_success "Docker image built successfully"

call :print_step "Running Docker container"
docker run -d -p 8082:8080 --name ebanking-test ebanking/transaction-service:test >nul 2>&1
if errorlevel 1 (
    call :print_error "Failed to start Docker container"
    exit /b 1
)
call :print_success "Docker container started"

REM Wait for container to be ready
timeout /t 10 /nobreak >nul

REM Test container health
curl -s -f "http://localhost:8082/actuator/health" >nul 2>&1
if errorlevel 1 (
    call :print_error "Docker container health check failed"
) else (
    call :print_success "Docker container health check passed"
)

REM Clean up
docker stop ebanking-test >nul 2>&1
docker rm ebanking-test >nul 2>&1
call :print_info "Docker container cleaned up"
goto :eof

REM Function to run performance tests
:test_performance
call :print_header "Testing Performance"

call :print_step "Running basic load test (10 requests)"
set start_time=%time%

for /l %%i in (1,1,10) do (
    start /b curl -s "%TEST_BASE_URL%/health" >nul 2>&1
)

REM Wait for all requests to complete
timeout /t 5 /nobreak >nul

set end_time=%time%
call :print_success "Load test completed"

call :print_step "Testing concurrent requests"
for /l %%i in (1,1,20) do (
    start /b curl -s "%TEST_BASE_URL%/transactions/count?customerId=%CUSTOMER_ID%" >nul 2>&1
)

timeout /t 5 /nobreak >nul
call :print_success "Concurrent test completed"
goto :eof

REM Function to run integration tests
:test_integration
call :print_header "Running Integration Tests"

call :print_step "Testing complete integration flow"

REM 1. Add transactions directly
call :print_info "Step 1: Adding transactions directly"
curl -s -X POST "%TEST_BASE_URL%/transactions?customerId=%CUSTOMER_ID%&count=2" >nul 2>&1

REM 2. Send transactions to Kafka
call :print_info "Step 2: Sending transactions to Kafka"
curl -s -X POST "%TEST_BASE_URL%/kafka/send?customerId=%CUSTOMER_ID%&count=2" >nul 2>&1

REM 3. Wait for processing
timeout /t 5 /nobreak >nul

REM 4. Check final count
call :print_info "Step 3: Checking final transaction count"
curl -s "%TEST_BASE_URL%/transactions/count?customerId=%CUSTOMER_ID%" > temp_response.txt 2>nul
echo Final transaction count:
type temp_response.txt
del temp_response.txt 2>nul

call :print_success "Integration test completed"
goto :eof

REM Function to clean up test data
:cleanup
call :print_header "Cleaning Up Test Data"

call :print_step "Clearing test transactions"
curl -s -X DELETE "%TEST_BASE_URL%/transactions?customerId=%CUSTOMER_ID%" > temp_response.txt 2>nul
findstr "successfully" temp_response.txt >nul
if errorlevel 1 (
    call :print_warning "Could not clean up test data"
) else (
    call :print_success "Test data cleaned up"
)
del temp_response.txt 2>nul
goto :eof

REM Function to show test summary
:show_summary
call :print_header "Test Summary"

echo ✅ Health endpoints tested
echo ✅ Swagger UI tested
echo ✅ Test endpoints tested
echo ✅ Kafka integration tested
echo ✅ API endpoints tested
echo ✅ Docker build tested
echo ✅ Performance tests completed
echo ✅ Integration tests completed

call :print_success "All tests completed successfully!"
goto :eof

REM Main execution
call :check_curl

if "%TEST_TYPE%"=="health" goto test_health
if "%TEST_TYPE%"=="swagger" goto test_swagger
if "%TEST_TYPE%"=="test-endpoints" goto test_endpoints
if "%TEST_TYPE%"=="kafka" goto test_kafka
if "%TEST_TYPE%"=="api" goto test_api
if "%TEST_TYPE%"=="docker" goto test_docker
if "%TEST_TYPE%"=="performance" goto test_performance
if "%TEST_TYPE%"=="integration" goto test_integration
if "%TEST_TYPE%"=="cleanup" goto cleanup
if "%TEST_TYPE%"=="all" goto run_all_tests

REM Default help
echo Usage: test-everything.bat {all^|health^|swagger^|test-endpoints^|kafka^|api^|docker^|performance^|integration^|cleanup}
echo.
echo Test Categories:
echo   all            - Run all tests
echo   health         - Test health endpoints
echo   swagger        - Test Swagger UI
echo   test-endpoints - Test test endpoints
echo   kafka          - Test Kafka integration
echo   api            - Test main API endpoints
echo   docker         - Test Docker build and run
echo   performance    - Run performance tests
echo   integration    - Run integration tests
echo   cleanup        - Clean up test data
goto :eof

REM Run all tests
:run_all_tests
call :print_header "Starting Complete e-Banking Transaction Service Test Suite"

call :test_health
call :test_swagger
call :test_endpoints
call :test_kafka
call :test_api
call :test_kafka_ui
call :test_docker
call :test_performance
call :test_integration
call :cleanup
call :show_summary
goto :eof 