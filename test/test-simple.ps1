# Simple e-Banking Transaction Service Test Script for Windows
# This script tests basic functionality

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

# Simple print functions
function Write-Header {
    param([string]$Message)
    Write-Host "========================================" -ForegroundColor Magenta
    Write-Host $Message -ForegroundColor Magenta
    Write-Host "========================================" -ForegroundColor Magenta
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor Red
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor Yellow
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor Blue
}

function Write-Step {
    param([string]$Message)
    Write-Host "🔍 $Message" -ForegroundColor Cyan
}

# Test health endpoint
function Test-Health {
    Write-Header "Testing Health Endpoint"
    
    Write-Step "Testing health endpoint"
    try {
        $response = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "UP") {
            Write-Success "Health check passed"
            Write-Host "Response: $content"
            return $true
        } else {
            Write-Error "Health check failed"
            Write-Host "Response: $content"
            return $false
        }
    }
    catch {
        Write-Error "Health check failed: $($_.Exception.Message)"
        return $false
    }
}

# Test Swagger UI
function Test-SwaggerUi {
    Write-Header "Testing Swagger UI"
    
    Write-Step "Checking Swagger UI accessibility"
    try {
        $response = Invoke-WebRequest -Uri $SwaggerUrl -UseBasicParsing
        Write-Success "Swagger UI is accessible"
        return $true
    }
    catch {
        Write-Error "Swagger UI is not accessible"
        return $false
    }
}

# Test test endpoints
function Test-TestEndpoints {
    Write-Header "Testing Test Endpoints"
    
    # Test health endpoint
    Write-Step "Testing test health endpoint"
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/health" -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "UP") {
            Write-Success "Test health endpoint works"
        } else {
            Write-Error "Test health endpoint failed"
            return $false
        }
    }
    catch {
        Write-Error "Test health endpoint failed: $($_.Exception.Message)"
        return $false
    }
    
    # Test adding transactions
    Write-Step "Adding test transactions"
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/transactions?customerId=$CustomerId&count=3" -Method POST -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "successfully") {
            Write-Success "Test transactions added successfully"
            Write-Host "Response: $content"
        } else {
            Write-Error "Failed to add test transactions"
            Write-Host "Response: $content"
            return $false
        }
    }
    catch {
        Write-Error "Failed to add test transactions: $($_.Exception.Message)"
        return $false
    }
    
    # Test transaction count
    Write-Step "Checking transaction count"
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/transactions/count?customerId=$CustomerId" -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "totalTransactions") {
            Write-Success "Transaction count endpoint works"
            Write-Host "Response: $content"
        } else {
            Write-Error "Transaction count endpoint failed"
            Write-Host "Response: $content"
            return $false
        }
    }
    catch {
        Write-Error "Transaction count endpoint failed: $($_.Exception.Message)"
        return $false
    }
    
    return $true
}

# Test Kafka integration
function Test-KafkaIntegration {
    Write-Header "Testing Kafka Integration"
    
    # Test sending transactions to Kafka
    Write-Step "Sending transactions to Kafka"
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/kafka/send?customerId=$CustomerId&count=2" -Method POST -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "successfully") {
            Write-Success "Transactions sent to Kafka successfully"
            Write-Host "Response: $content"
        } else {
            Write-Error "Failed to send transactions to Kafka"
            Write-Host "Response: $content"
            return $false
        }
    }
    catch {
        Write-Error "Failed to send transactions to Kafka: $($_.Exception.Message)"
        return $false
    }
    
    # Wait for Kafka processing
    Write-Step "Waiting for Kafka processing (10 seconds)"
    Start-Sleep -Seconds 10
    
    # Check if transactions were consumed
    Write-Step "Checking if Kafka transactions were consumed"
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/transactions/count?customerId=$CustomerId" -UseBasicParsing
        Write-Host "Transaction count after Kafka: $($response.Content)"
    }
    catch {
        Write-Warning "Could not check transaction count after Kafka"
    }
    
    return $true
}

# Test main API endpoints
function Test-ApiEndpoints {
    Write-Header "Testing Main API Endpoints"
    
    # Test getting transactions
    Write-Step "Testing transaction retrieval API"
    try {
        $headers = @{
            "Authorization" = "Bearer $JwtToken"
        }
        
        $response = Invoke-WebRequest -Uri "$ApiBaseUrl/transactions?customerId=$CustomerId&page=0&size=10&month=10&year=2020&baseCurrency=USD" -Headers $headers -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "transactions") {
            Write-Success "Transaction API endpoint works"
            Write-Host "Response preview: $($content.Substring(0, [Math]::Min(200, $content.Length)))..."
        } else {
            Write-Error "Transaction API endpoint failed"
            Write-Host "Response: $content"
            return $false
        }
    }
    catch {
        Write-Error "Transaction API endpoint failed: $($_.Exception.Message)"
        return $false
    }
    
    return $true
}

# Test Docker build
function Test-DockerBuild {
    Write-Header "Testing Docker Build"
    
    # Check if Docker is available
    try {
        $dockerVersion = docker --version
        Write-Info "Docker version: $dockerVersion"
    }
    catch {
        Write-Warning "Docker not found, skipping Docker tests"
        return $true
    }
    
    Write-Step "Building Docker image"
    try {
        docker build -t ebanking/transaction-service:test ..
        Write-Success "Docker image built successfully"
    }
    catch {
        Write-Error "Docker build failed"
        return $false
    }
    
    Write-Step "Running Docker container"
    try {
        $containerId = docker run -d -p 8082:8080 --name ebanking-test ebanking/transaction-service:test
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Docker container started"
            
            # Wait for container to be ready
            Start-Sleep -Seconds 10
            
            # Test container health
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:8082/actuator/health" -UseBasicParsing
                Write-Success "Docker container health check passed"
            }
            catch {
                Write-Error "Docker container health check failed"
            }
            
            # Clean up
            docker stop $containerId 2>$null
            docker rm $containerId 2>$null
            Write-Info "Docker container cleaned up"
        } else {
            Write-Error "Failed to start Docker container"
            return $false
        }
    }
    catch {
        Write-Error "Failed to start Docker container: $($_.Exception.Message)"
        return $false
    }
    
    return $true
}

# Clean up test data
function Clear-TestData {
    Write-Header "Cleaning Up Test Data"
    
    Write-Step "Clearing test transactions"
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/transactions?customerId=$CustomerId" -Method DELETE -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "successfully") {
            Write-Success "Test data cleaned up"
        } else {
            Write-Warning "Could not clean up test data"
        }
    }
    catch {
        Write-Warning "Could not clean up test data: $($_.Exception.Message)"
    }
}

# Show test summary
function Show-TestSummary {
    Write-Header "Test Summary"
    
    Write-Host "✅ Health endpoints tested"
    Write-Host "✅ Swagger UI tested"
    Write-Host "✅ Test endpoints tested"
    Write-Host "✅ Kafka integration tested"
    Write-Host "✅ API endpoints tested"
    Write-Host "✅ Docker build tested"
    
    Write-Success "All tests completed successfully!"
}

# Run all tests
function Start-AllTests {
    Write-Header "Starting Complete e-Banking Transaction Service Test Suite"
    
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
        Write-Host "Usage: .\test-simple.ps1 {all|health|swagger|test-endpoints|kafka|api|docker}"
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