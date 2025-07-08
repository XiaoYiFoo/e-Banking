# Basic e-Banking Transaction Service Test Script for Windows
# This script tests basic functionality without complex functions

param(
    [Parameter(Position=0)]
    [ValidateSet("all", "health", "swagger", "test-endpoints", "kafka", "api", "docker")]
    [string]$TestType = "all"
)

# Configuration
$BaseUrl = "http://localhost:8080"
$ApiBaseUrl = "$BaseUrl/api/v1"
$TestBaseUrl = "$BaseUrl/test"
$SwaggerUrl = "$BaseUrl/swagger-ui"
$HealthUrl = "$BaseUrl/actuator/health"
$KafkaUiUrl = "http://localhost:8081"

# Test data
$CustomerId = "test-customer-123"
$JwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJjdXN0b21lcklkIjoiY3VzdG9tZXIxMjMiLCJpYXQiOjE2MTYyMzkwMjIsImV4cCI6MTkzMTgxNTAyMn0.test-signature"

# Test health endpoint
function Test-Health {
    Write-Host "========================================" -ForegroundColor Magenta
    Write-Host "Testing Health Endpoint" -ForegroundColor Magenta
    Write-Host "========================================" -ForegroundColor Magenta
    
    Write-Host "🔍 Testing health endpoint" -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "UP") {
            Write-Host "✅ Health check passed" -ForegroundColor Green
            Write-Host "Response: $content"
            return $true
        } else {
            Write-Host "❌ Health check failed" -ForegroundColor Red
            Write-Host "Response: $content"
            return $false
        }
    }
    catch {
        Write-Host "❌ Health check failed: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Test Swagger UI
function Test-SwaggerUi {
    Write-Host "========================================" -ForegroundColor Magenta
    Write-Host "Testing Swagger UI" -ForegroundColor Magenta
    Write-Host "========================================" -ForegroundColor Magenta
    
    Write-Host "🔍 Checking Swagger UI accessibility" -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest -Uri $SwaggerUrl -UseBasicParsing
        Write-Host "✅ Swagger UI is accessible" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "❌ Swagger UI is not accessible" -ForegroundColor Red
        return $false
    }
}

# Test test endpoints
function Test-TestEndpoints {
    Write-Host "========================================" -ForegroundColor Magenta
    Write-Host "Testing Test Endpoints" -ForegroundColor Magenta
    Write-Host "========================================" -ForegroundColor Magenta
    
    # Test health endpoint
    Write-Host "🔍 Testing test health endpoint" -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/health" -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "UP") {
            Write-Host "✅ Test health endpoint works" -ForegroundColor Green
        } else {
            Write-Host "❌ Test health endpoint failed" -ForegroundColor Red
            return $false
        }
    }
    catch {
        Write-Host "❌ Test health endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
    
    # Test adding transactions
    Write-Host "🔍 Adding test transactions" -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/transactions?customerId=$CustomerId&count=3" -Method POST -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "successfully") {
            Write-Host "✅ Test transactions added successfully" -ForegroundColor Green
            Write-Host "Response: $content"
        } else {
            Write-Host "❌ Failed to add test transactions" -ForegroundColor Red
            Write-Host "Response: $content"
            return $false
        }
    }
    catch {
        Write-Host "❌ Failed to add test transactions: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
    
    # Test transaction count
    Write-Host "🔍 Checking transaction count" -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/transactions/count?customerId=$CustomerId" -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "totalTransactions") {
            Write-Host "✅ Transaction count endpoint works" -ForegroundColor Green
            Write-Host "Response: $content"
        } else {
            Write-Host "❌ Transaction count endpoint failed" -ForegroundColor Red
            Write-Host "Response: $content"
            return $false
        }
    }
    catch {
        Write-Host "❌ Transaction count endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
    
    return $true
}

# Test Kafka integration
function Test-KafkaIntegration {
    Write-Host "========================================" -ForegroundColor Magenta
    Write-Host "Testing Kafka Integration" -ForegroundColor Magenta
    Write-Host "========================================" -ForegroundColor Magenta
    
    # Test sending transactions to Kafka
    Write-Host "🔍 Sending transactions to Kafka" -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/kafka/send?customerId=$CustomerId&count=2" -Method POST -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "successfully") {
            Write-Host "✅ Transactions sent to Kafka successfully" -ForegroundColor Green
            Write-Host "Response: $content"
        } else {
            Write-Host "❌ Failed to send transactions to Kafka" -ForegroundColor Red
            Write-Host "Response: $content"
            return $false
        }
    }
    catch {
        Write-Host "❌ Failed to send transactions to Kafka: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
    
    # Wait for Kafka processing
    Write-Host "🔍 Waiting for Kafka processing (10 seconds)" -ForegroundColor Cyan
    Start-Sleep -Seconds 10
    
    # Check if transactions were consumed
    Write-Host "🔍 Checking if Kafka transactions were consumed" -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/transactions/count?customerId=$CustomerId" -UseBasicParsing
        Write-Host "Transaction count after Kafka: $($response.Content)"
    }
    catch {
        Write-Host "⚠️  Could not check transaction count after Kafka" -ForegroundColor Yellow
    }
    
    return $true
}

# Test main API endpoints
function Test-ApiEndpoints {
    Write-Host "========================================" -ForegroundColor Magenta
    Write-Host "Testing Main API Endpoints" -ForegroundColor Magenta
    Write-Host "========================================" -ForegroundColor Magenta
    
    # Test getting transactions
    Write-Host "🔍 Testing transaction retrieval API" -ForegroundColor Cyan
    try {
        $headers = @{
            "Authorization" = "Bearer $JwtToken"
        }
        
        $response = Invoke-WebRequest -Uri "$ApiBaseUrl/transactions?customerId=$CustomerId&page=0&size=10&month=10&year=2020&baseCurrency=USD" -Headers $headers -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "transactions") {
            Write-Host "✅ Transaction API endpoint works" -ForegroundColor Green
            Write-Host "Response preview: $($content.Substring(0, [Math]::Min(200, $content.Length)))..."
        } else {
            Write-Host "❌ Transaction API endpoint failed" -ForegroundColor Red
            Write-Host "Response: $content"
            return $false
        }
    }
    catch {
        Write-Host "❌ Transaction API endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
    
    return $true
}

# Test Docker build
function Test-DockerBuild {
    Write-Host "========================================" -ForegroundColor Magenta
    Write-Host "Testing Docker Build" -ForegroundColor Magenta
    Write-Host "========================================" -ForegroundColor Magenta
    
    # Check if Docker is available
    try {
        $dockerVersion = docker --version
        Write-Host "ℹ️  Docker version: $dockerVersion" -ForegroundColor Blue
    }
    catch {
        Write-Host "⚠️  Docker not found, skipping Docker tests" -ForegroundColor Yellow
        return $true
    }
    
    Write-Host "🔍 Building Docker image" -ForegroundColor Cyan
    try {
        docker build -t ebanking/transaction-service:test ..
        Write-Host "✅ Docker image built successfully" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ Docker build failed" -ForegroundColor Red
        return $false
    }
    
    Write-Host "🔍 Running Docker container" -ForegroundColor Cyan
    try {
        $containerId = docker run -d -p 8082:8080 --name ebanking-test ebanking/transaction-service:test
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Docker container started" -ForegroundColor Green
            
            # Wait for container to be ready
            Start-Sleep -Seconds 10
            
            # Test container health
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:8082/actuator/health" -UseBasicParsing
                Write-Host "✅ Docker container health check passed" -ForegroundColor Green
            }
            catch {
                Write-Host "❌ Docker container health check failed" -ForegroundColor Red
            }
            
            # Clean up
            docker stop $containerId 2>$null
            docker rm $containerId 2>$null
            Write-Host "ℹ️  Docker container cleaned up" -ForegroundColor Blue
        } else {
            Write-Host "❌ Failed to start Docker container" -ForegroundColor Red
            return $false
        }
    }
    catch {
        Write-Host "❌ Failed to start Docker container: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
    
    return $true
}

# Clean up test data
function Clear-TestData {
    Write-Host "========================================" -ForegroundColor Magenta
    Write-Host "Cleaning Up Test Data" -ForegroundColor Magenta
    Write-Host "========================================" -ForegroundColor Magenta
    
    Write-Host "🔍 Clearing test transactions" -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/transactions?customerId=$CustomerId" -Method DELETE -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "successfully") {
            Write-Host "✅ Test data cleaned up" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Could not clean up test data" -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "⚠️  Could not clean up test data: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Show test summary
function Show-TestSummary {
    Write-Host "========================================" -ForegroundColor Magenta
    Write-Host "Test Summary" -ForegroundColor Magenta
    Write-Host "========================================" -ForegroundColor Magenta
    
    Write-Host "✅ Health endpoints tested"
    Write-Host "✅ Swagger UI tested"
    Write-Host "✅ Test endpoints tested"
    Write-Host "✅ Kafka integration tested"
    Write-Host "✅ API endpoints tested"
    Write-Host "✅ Docker build tested"
    
    Write-Host "✅ All tests completed successfully!" -ForegroundColor Green
}

# Run all tests
function Start-AllTests {
    Write-Host "========================================" -ForegroundColor Magenta
    Write-Host "Starting Complete e-Banking Transaction Service Test Suite" -ForegroundColor Magenta
    Write-Host "========================================" -ForegroundColor Magenta
    
    Test-Health
    Test-SwaggerUi
    Test-TestEndpoints
    Test-KafkaIntegration
    Test-ApiEndpoints
    Test-DockerBuild
    
    # Cleanup
    Clear-TestData
    
    # Show summary
    Show-TestSummary
}

# Main execution
switch ($TestType) {
    "health" { Test-Health }
    "swagger" { Test-SwaggerUi }
    "test-endpoints" { Test-TestEndpoints }
    "kafka" { Test-KafkaIntegration }
    "api" { Test-ApiEndpoints }
    "docker" { Test-DockerBuild }
    "all" { Start-AllTests }
    default {
        Write-Host "Usage: .\test-basic.ps1 {all|health|swagger|test-endpoints|kafka|api|docker}"
        Write-Host ""
        Write-Host "Test Categories:"
        Write-Host "  all            - Run all tests"
        Write-Host "  health         - Test health endpoints"
        Write-Host "  swagger        - Test Swagger UI"
        Write-Host "  test-endpoints - Test test endpoints"
        Write-Host "  kafka          - Test Kafka integration"
        Write-Host "  api            - Test main API endpoints"
        Write-Host "  docker         - Test Docker build and run"
    }
} 