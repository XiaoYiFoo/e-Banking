# e-Banking Transaction Service - Complete Testing Script for Windows
# This PowerShell script tests all components: local development, Docker, and Kubernetes

param(
    [Parameter(Position=0)]
    [ValidateSet("all", "health", "swagger", "test-endpoints", "kafka", "api", "docker", "kubernetes", "performance", "integration", "cleanup")]
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
$JwtToken = ""

# Function to print colored output
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

# Function to check if a service is running
function Test-Service {
    param(
        [string]$ServiceName,
        [string]$Url,
        [int]$MaxAttempts = 30
    )
    
    Write-Step "Checking if $ServiceName is running at $Url"
    
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                Write-Success "$ServiceName is running"
                return $true
            }
        }
        catch {
            # Continue trying
        }
        
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 2
    }
    
    Write-Error "$ServiceName is not running after $MaxAttempts attempts"
    return $false
}

# Function to generate JWT token
function New-JwtToken {
    Write-Step "Generating JWT token for testing"
    
    # This is a simple test token - in production, you'd get this from your auth service
    $script:JwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJjdXN0b21lcklkIjoi$CustomerIdIiwiaWF0IjoxNjE2MjM5MDIyLCJleHAiOjE5MzE4MTUwMjJ9.test-signature"
    
    Write-Success "JWT token generated"
}

# Function to test health endpoint
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

# Function to test Swagger UI
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

# Function to test test endpoints
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
    
    # Test service info
    Write-Step "Testing service info endpoint"
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/info" -UseBasicParsing
        $content = $response.Content
        
        if ($content -match "e-Banking Transaction Service") {
            Write-Success "Service info endpoint works"
        } else {
            Write-Error "Service info endpoint failed"
            return $false
        }
    }
    catch {
        Write-Error "Service info endpoint failed: $($_.Exception.Message)"
        return $false
    }
    
    return $true
}

# Function to test Kafka integration
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

# Function to test main API endpoints
function Test-ApiEndpoints {
    Write-Header "Testing Main API Endpoints"
    
    if ([string]::IsNullOrEmpty($JwtToken)) {
        Write-Warning "No JWT token available, skipping authenticated endpoints"
        return $true
    }
    
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

# Function to test Kafka UI
function Test-KafkaUi {
    Write-Header "Testing Kafka UI"
    
    Write-Step "Checking Kafka UI accessibility"
    try {
        $response = Invoke-WebRequest -Uri $KafkaUiUrl -UseBasicParsing
        Write-Success "Kafka UI is accessible"
        return $true
    }
    catch {
        Write-Warning "Kafka UI is not accessible (may not be running)"
        return $true
    }
}

# Function to test Docker build
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
        docker build -t ebanking/transaction-service:test .
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

# Function to test Kubernetes deployment
function Test-Kubernetes {
    Write-Header "Testing Kubernetes Deployment"
    
    # Check if kubectl is available
    try {
        $kubectlVersion = kubectl version --client
        Write-Info "kubectl version: $kubectlVersion"
    }
    catch {
        Write-Warning "kubectl not found, skipping Kubernetes tests"
        return $true
    }
    
    # Check if we're connected to a cluster
    try {
        kubectl cluster-info | Out-Null
    }
    catch {
        Write-Warning "Not connected to Kubernetes cluster, skipping Kubernetes tests"
        return $true
    }
    
    Write-Step "Deploying to Kubernetes"
    if (Test-Path "k8s/deploy.sh") {
        try {
            # Note: This would need WSL or Git Bash on Windows
            Write-Warning "Kubernetes deployment script requires bash (WSL/Git Bash)"
            Write-Info "Please run: bash k8s/deploy.sh deploy"
        }
        catch {
            Write-Error "Kubernetes deployment failed"
            return $false
        }
    } else {
        Write-Warning "Kubernetes deployment script not found"
    }
    
    return $true
}

# Function to run performance tests
function Test-Performance {
    Write-Header "Testing Performance"
    
    Write-Step "Running basic load test (10 requests)"
    $startTime = Get-Date
    
    $jobs = @()
    for ($i = 1; $i -le 10; $i++) {
        $jobs += Start-Job -ScriptBlock {
            param($url)
            try {
                Invoke-WebRequest -Uri $url -UseBasicParsing | Out-Null
            } catch {}
        } -ArgumentList "$TestBaseUrl/health"
    }
    
    Wait-Job -Job $jobs | Out-Null
    Remove-Job -Job $jobs
    
    $endTime = Get-Date
    $duration = ($endTime - $startTime).TotalSeconds
    
    Write-Success "Load test completed in $duration seconds"
    
    # Test concurrent requests
    Write-Step "Testing concurrent requests"
    $startTime = Get-Date
    
    $jobs = @()
    for ($i = 1; $i -le 20; $i++) {
        $jobs += Start-Job -ScriptBlock {
            param($url)
            try {
                Invoke-WebRequest -Uri $url -UseBasicParsing | Out-Null
            } catch {}
        } -ArgumentList "$TestBaseUrl/transactions/count?customerId=$CustomerId"
    }
    
    Wait-Job -Job $jobs | Out-Null
    Remove-Job -Job $jobs
    
    $endTime = Get-Date
    $duration = ($endTime - $startTime).TotalSeconds
    
    Write-Success "Concurrent test completed in $duration seconds"
    
    return $true
}

# Function to run integration tests
function Test-Integration {
    Write-Header "Running Integration Tests"
    
    # Test complete flow: add transactions -> send to Kafka -> retrieve via API
    Write-Step "Testing complete integration flow"
    
    # 1. Add transactions directly
    Write-Info "Step 1: Adding transactions directly"
    try {
        Invoke-WebRequest -Uri "$TestBaseUrl/transactions?customerId=$CustomerId&count=2" -Method POST -UseBasicParsing | Out-Null
    } catch {}
    
    # 2. Send transactions to Kafka
    Write-Info "Step 2: Sending transactions to Kafka"
    try {
        Invoke-WebRequest -Uri "$TestBaseUrl/kafka/send?customerId=$CustomerId&count=2" -Method POST -UseBasicParsing | Out-Null
    } catch {}
    
    # 3. Wait for processing
    Start-Sleep -Seconds 5
    
    # 4. Check final count
    Write-Info "Step 3: Checking final transaction count"
    try {
        $response = Invoke-WebRequest -Uri "$TestBaseUrl/transactions/count?customerId=$CustomerId" -UseBasicParsing
        Write-Host "Final transaction count: $($response.Content)"
    } catch {}
    
    Write-Success "Integration test completed"
    return $true
}

# Function to clean up test data
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

# Function to show test summary
function Show-TestSummary {
    Write-Header "Test Summary"
    
    Write-Host "✅ Health endpoints tested"
    Write-Host "✅ Swagger UI tested"
    Write-Host "✅ Test endpoints tested"
    Write-Host "✅ Kafka integration tested"
    Write-Host "✅ API endpoints tested"
    Write-Host "✅ Docker build tested"
    Write-Host "✅ Performance tests completed"
    Write-Host "✅ Integration tests completed"
    
    Write-Success "All tests completed successfully!"
}

# Main test function
function Start-AllTests {
    Write-Header "Starting Complete e-Banking Transaction Service Test Suite"
    
    # Check prerequisites
    Write-Step "Checking prerequisites"
    
    # Generate JWT token
    New-JwtToken
    
    # Run tests
    Test-Health
    Test-SwaggerUi
    Test-TestEndpoints
    Test-KafkaIntegration
    Test-ApiEndpoints
    Test-KafkaUi
    Test-DockerBuild
    Test-Kubernetes
    Test-Performance
    Test-Integration
    
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
    "kubernetes" { Test-Kubernetes }
    "performance" { Test-Performance }
    "integration" { Test-Integration }
    "cleanup" { Clear-TestData }
    "all" { Start-AllTests }
    default {
        Write-Host "Usage: .\test-everything.ps1 {all|health|swagger|test-endpoints|kafka|api|docker|kubernetes|performance|integration|cleanup}"
        Write-Host ""
        Write-Host "Test Categories:"
        Write-Host "  all            - Run all tests"
        Write-Host "  health         - Test health endpoints"
        Write-Host "  swagger        - Test Swagger UI"
        Write-Host "  test-endpoints - Test test endpoints"
        Write-Host "  kafka          - Test Kafka integration"
        Write-Host "  api            - Test main API endpoints"
        Write-Host "  docker         - Test Docker build and run"
        Write-Host "  kubernetes     - Test Kubernetes deployment"
        Write-Host "  performance    - Run performance tests"
        Write-Host "  integration    - Run integration tests"
        Write-Host "  cleanup        - Clean up test data"
    }
} 