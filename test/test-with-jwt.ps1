# Test script for e-Banking Transaction Service with JWT Authentication
# This script generates a valid JWT token and tests all endpoints

Write-Host "=== e-Banking Transaction Service JWT Test ===" -ForegroundColor Green

# Configuration
$BASE_URL = "http://localhost:8080"
$JWT_SECRET = "your-secret-key-here-make-it-long-and-secure-in-production"
$CUSTOMER_ID = "test-customer-12345"

# Function to generate JWT token using PowerShell
function Generate-JWT {
    param(
        [string]$Secret,
        [string]$Subject,
        [long]$IssuedAt,
        [lo

    # Create header
    $header = @{
        alg = "HS256"
        typ = "JWT"
    }
    $headerJson = $header | ConvertTo-Json -Compress
    $headerBase64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($headerJson)).Replace('+', '-').Replace('/', '_').Replace('=', '')

    # Create payload
    $payload = @{
        sub = $Subject
        iat = $IssuedAt
        exp = $Expiration
    }
    $payloadJson = $payload | ConvertTo-Json -Compress
    $payloadBase64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($payloadJson)).Replace('+', '-').Replace('/', '_').Replace('=', '')

    # Create signature
    $dataToSign = "$headerBase64.$payloadBase64"
    $hmac = New-Object System.Security.Cryptography.HMACSHA256
    $hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($Secret)
    $signature = [Convert]::ToBase64String($hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($dataToSign))).Replace('+', '-').Replace('/', '_').Replace('=', '')

    return "$headerBase64.$payloadBase64.$signature"
}ng]$Expiration
    )

# Generate JWT token
Write-Host "Generating JWT token..." -ForegroundColor Yellow
$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$exp = $now + 86400  # 24 hours from now
$jwtToken = Generate-JWT -Secret $JWT_SECRET -Subject $CUSTOMER_ID -IssuedAt $now -Expiration $exp

Write-Host "Generated JWT Token:" -ForegroundColor Cyan
Write-Host $jwtToken -ForegroundColor White
Write-Host ""

# Test 1: Health Check
Write-Host "=== Test 1: Health Check ===" -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/actuator/health" -Method GET -ContentType "application/json"
    Write-Host "Health Check: SUCCESS" -ForegroundColor Green
    Write-Host "Status: $($response.status)" -ForegroundColor White
} catch {
    Write-Host "Health Check: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: Test Endpoints (No Auth Required)
Write-Host "=== Test 2: Test Endpoints (No Auth Required) ===" -ForegroundColor Green

# Test endpoint to add sample transactions (using correct endpoint)
Write-Host "Adding sample transactions..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/test/transactions?customerId=$CUSTOMER_ID&count=5" -Method POST -ContentType "application/json"
    Write-Host "Add Sample Transactions: SUCCESS" -ForegroundColor Green
    Write-Host "Added transactions: $($response.message)" -ForegroundColor White
    Write-Host "Success count: $($response.successCount)" -ForegroundColor White
} catch {
    Write-Host "Add Sample Transactions: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test endpoint to get transaction count
Write-Host "Getting transaction count..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/test/transactions/count?customerId=$CUSTOMER_ID" -Method GET -ContentType "application/json"
    Write-Host "Transaction Count: SUCCESS" -ForegroundColor Green
    Write-Host "Total transactions: $($response.totalTransactions)" -ForegroundColor White
} catch {
    Write-Host "Transaction Count: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test service info endpoint
Write-Host "Getting service info..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/test/info" -Method GET -ContentType "application/json"
    Write-Host "Service Info: SUCCESS" -ForegroundColor Green
    Write-Host "Service: $($response.service)" -ForegroundColor White
    Write-Host "Version: $($response.version)" -ForegroundColor White
} catch {
    Write-Host "Service Info: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 3: Main API Endpoints (JWT Auth Required)
Write-Host "=== Test 3: Main API Endpoints (JWT Auth Required) ===" -ForegroundColor Green

# Test main transactions endpoint
Write-Host "Testing main transactions endpoint..." -ForegroundColor Yellow
try {
    $headers = @{
        "Authorization" = "Bearer $jwtToken"
        "Content-Type" = "application/json"
    }
    $response = Invoke-RestMethod -Uri "$BASE_URL/api/v1/transactions?customerId=$CUSTOMER_ID&page=0&size=10&month=10&year=2020&baseCurrency=USD" -Method GET -Headers $headers
    Write-Host "Main Transactions API: SUCCESS" -ForegroundColor Green
    Write-Host "Total transactions: $($response.totalElements)" -ForegroundColor White
    Write-Host "Total credit: $($response.totalCredit)" -ForegroundColor White
    Write-Host "Total debit: $($response.totalDebit)" -ForegroundColor White
} catch {
    Write-Host "Main Transactions API: FAILED - $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
    }
}
Write-Host ""

# Test 4: Swagger UI
Write-Host "=== Test 4: Swagger UI ===" -ForegroundColor Green
Write-Host "Swagger UI should be available at: $BASE_URL/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host "Use the JWT token above in the Authorize button" -ForegroundColor Yellow
Write-Host ""

# Test 5: Kafka Integration Test
Write-Host "=== Test 5: Kafka Integration Test ===" -ForegroundColor Green
Write-Host "Testing Kafka transaction producer..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/test/kafka/send-single?customerId=$CUSTOMER_ID&amount=150.00&currency=EUR&description=Test transaction from PowerShell" -Method POST -ContentType "application/json"
    Write-Host "Kafka Transaction Producer: SUCCESS" -ForegroundColor Green
    Write-Host "Transaction ID: $($response.transactionId)" -ForegroundColor White
    Write-Host "Message: $($response.message)" -ForegroundColor White
} catch {
    Write-Host "Kafka Transaction Producer: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Test bulk Kafka send
Write-Host "Testing bulk Kafka send..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/test/kafka/send?customerId=$CUSTOMER_ID&count=3" -Method POST -ContentType "application/json"
    Write-Host "Bulk Kafka Send: SUCCESS" -ForegroundColor Green
    Write-Host "Message: $($response.message)" -ForegroundColor White
    Write-Host "Count: $($response.count)" -ForegroundColor White
} catch {
    Write-Host "Bulk Kafka Send: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 6: Exchange Rate Service
Write-Host "=== Test 6: Exchange Rate Service ===" -ForegroundColor Green
Write-Host "Testing exchange rate conversion..." -ForegroundColor Yellow
try {
    $headers = @{
        "Authorization" = "Bearer $jwtToken"
        "Content-Type" = "application/json"
    }
    $response = Invoke-RestMethod -Uri "$BASE_URL/api/v1/transactions?customerId=$CUSTOMER_ID&page=0&size=5&baseCurrency=EUR" -Method GET -Headers $headers
    Write-Host "Exchange Rate Service: SUCCESS" -ForegroundColor Green
    Write-Host "Base Currency: $($response.baseCurrency)" -ForegroundColor White
    Write-Host "Exchange Rate Used: $($response.exchangeRate)" -ForegroundColor White
} catch {
    Write-Host "Exchange Rate Service: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 7: Kafka UI Testing
Write-Host "=== Test 7: Kafka UI Testing ===" -ForegroundColor Green

# Check if kubectl is available
Write-Host "Checking kubectl availability..." -ForegroundColor Yellow
try {
    $kubectlVersion = kubectl version --client --short 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "kubectl is available" -ForegroundColor Green
        
        # Check if Kafka namespace exists
        Write-Host "Checking Kafka namespace..." -ForegroundColor Yellow
        $namespaceCheck = kubectl get namespace kafka 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Kafka namespace exists" -ForegroundColor Green
            
            # Check Kafka UI pod status
            Write-Host "Checking Kafka UI pod status..." -ForegroundColor Yellow
            $kafkaUIPod = kubectl get pods -n kafka -l app=kafka-ui -o jsonpath='{.items[0].status.phase}' 2>$null
            if ($kafkaUIPod -eq "Running") {
                Write-Host "Kafka UI pod is running" -ForegroundColor Green
                
                # Test Kafka UI API endpoints
                Write-Host "Testing Kafka UI API endpoints..." -ForegroundColor Yellow
                
                # Port forward Kafka UI service temporarily
                Write-Host "Setting up port forward for Kafka UI..." -ForegroundColor Yellow
                Start-Job -ScriptBlock {
                    kubectl port-forward -n kafka svc/kafka-ui 8081:80
                } | Out-Null
                
                # Wait a moment for port forward to establish
                Start-Sleep -Seconds 3
                
                # Test Kafka UI health endpoint
                try {
                    $kafkaUIResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/clusters" -Method GET -ContentType "application/json" -TimeoutSec 10
                    Write-Host "Kafka UI API: SUCCESS" -ForegroundColor Green
                    Write-Host "Available clusters: $($kafkaUIResponse.Count)" -ForegroundColor White
                } catch {
                    Write-Host "Kafka UI API: FAILED - $($_.Exception.Message)" -ForegroundColor Red
                }
                
                # Test Kafka UI topics endpoint
                try {
                    $topicsResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/clusters/local/topics" -Method GET -ContentType "application/json" -TimeoutSec 10
                    Write-Host "Kafka UI Topics: SUCCESS" -ForegroundColor Green
                    Write-Host "Available topics: $($topicsResponse.Count)" -ForegroundColor White
                    
                    # Check if transactions topic exists
                    $transactionsTopic = $topicsResponse | Where-Object { $_.name -eq "transactions" }
                    if ($transactionsTopic) {
                        Write-Host "Transactions topic found: $($transactionsTopic.name)" -ForegroundColor Green
                        Write-Host "Partitions: $($transactionsTopic.partitions.Count)" -ForegroundColor White
                    } else {
                        Write-Host "Transactions topic not found" -ForegroundColor Yellow
                    }
                } catch {
                    Write-Host "Kafka UI Topics: FAILED - $($_.Exception.Message)" -ForegroundColor Red
                }
                
                # Stop port forward job
                Get-Job | Stop-Job
                Get-Job | Remove-Job
                
                Write-Host "Kafka UI Web Interface: http://localhost:8081" -ForegroundColor Cyan
                Write-Host "Use 'kubectl port-forward -n kafka svc/kafka-ui 8081:80' to access" -ForegroundColor Yellow
                
            } else {
                Write-Host "Kafka UI pod is not running. Status: $kafkaUIPod" -ForegroundColor Red
            }
        } else {
            Write-Host "Kafka namespace not found. Deploy Kafka first using: kubectl apply -f k8s/kafka.yaml" -ForegroundColor Red
        }
    } else {
        Write-Host "kubectl not available. Install kubectl to test Kafka UI" -ForegroundColor Red
    }
} catch {
    Write-Host "Kafka UI Testing: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 8: Clear Test Data
Write-Host "=== Test 8: Clear Test Data ===" -ForegroundColor Green
Write-Host "Clearing test transactions..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/test/transactions?customerId=$CUSTOMER_ID" -Method DELETE -ContentType "application/json"
    Write-Host "Clear Test Data: SUCCESS" -ForegroundColor Green
    Write-Host "Removed count: $($response.removedCount)" -ForegroundColor White
} catch {
    Write-Host "Clear Test Data: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "=== Test Summary ===" -ForegroundColor Green
Write-Host "JWT Token generated successfully" -ForegroundColor Green
Write-Host "Use this token in Swagger UI or for API testing:" -ForegroundColor Yellow
Write-Host "Authorization: Bearer $jwtToken" -ForegroundColor Cyan
Write-Host ""
Write-Host "Service URLs:" -ForegroundColor Yellow
Write-Host "- Application: $BASE_URL" -ForegroundColor White
Write-Host "- Swagger UI: $BASE_URL/swagger-ui/index.html" -ForegroundColor White
Write-Host "- Kafka UI: http://localhost:8081 (after port-forward)" -ForegroundColor White
Write-Host ""
Write-Host "All tests completed!" -ForegroundColor Green 