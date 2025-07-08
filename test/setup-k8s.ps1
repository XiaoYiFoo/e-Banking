# Setup script for Kubernetes testing
# This script helps set up a local Kubernetes environment for testing

Write-Host "=== Kubernetes Setup for e-Banking Testing ===" -ForegroundColor Green

# Check if kubectl is available
Write-Host "Checking kubectl..." -ForegroundColor Yellow
try {
    $kubectlVersion = kubectl version --client 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "kubectl is available" -ForegroundColor Green
    } else {
        Write-Host "kubectl is not working properly" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "kubectl not found. Please install kubectl first." -ForegroundColor Red
    exit 1
}

# Check Docker Desktop
Write-Host "Checking Docker Desktop..." -ForegroundColor Yellow
try {
    $dockerVersion = docker version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Docker Desktop is running" -ForegroundColor Green
    } else {
        Write-Host "Docker Desktop is not running" -ForegroundColor Red
        Write-Host "Please start Docker Desktop first" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "Docker not found. Please install Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Check Kubernetes contexts
Write-Host "Checking Kubernetes contexts..." -ForegroundColor Yellow
$contexts = kubectl config get-contexts 2>$null
if ($contexts -and $contexts.Count -gt 1) {
    Write-Host "Kubernetes contexts found:" -ForegroundColor Green
    $contexts | ForEach-Object { Write-Host "  $_" -ForegroundColor White }
} else {
    Write-Host "No Kubernetes contexts found" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To enable Kubernetes in Docker Desktop:" -ForegroundColor Cyan
    Write-Host "1. Open Docker Desktop" -ForegroundColor White
    Write-Host "2. Go to Settings > Kubernetes" -ForegroundColor White
    Write-Host "3. Check 'Enable Kubernetes'" -ForegroundColor White
    Write-Host "4. Click 'Apply & Restart'" -ForegroundColor White
    Write-Host ""
    Write-Host "Alternatively, you can use Minikube:" -ForegroundColor Cyan
    Write-Host "1. Install Minikube: choco install minikube" -ForegroundColor White
    Write-Host "2. Start Minikube: minikube start" -ForegroundColor White
    Write-Host ""
    Write-Host "After setting up Kubernetes, run this script again." -ForegroundColor Yellow
    exit 1
}

# Test cluster connectivity
Write-Host "Testing cluster connectivity..." -ForegroundColor Yellow
try {
    $nodes = kubectl get nodes 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Cluster is accessible" -ForegroundColor Green
        Write-Host "Available nodes:" -ForegroundColor White
        $nodes | ForEach-Object { Write-Host "  $_" -ForegroundColor White }
    } else {
        Write-Host "Cannot connect to cluster" -ForegroundColor Red
        Write-Host "Please ensure Kubernetes is running" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "Cluster connectivity test failed" -ForegroundColor Red
    exit 1
}

# Create namespace for Kafka
Write-Host "Creating Kafka namespace..." -ForegroundColor Yellow
try {
    kubectl create namespace kafka --dry-run=client -o yaml | kubectl apply -f - 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Kafka namespace created/verified" -ForegroundColor Green
    } else {
        Write-Host "Failed to create Kafka namespace" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Namespace creation failed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Setup Complete ===" -ForegroundColor Green
Write-Host "Kubernetes is ready for testing!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Deploy Kafka: kubectl apply -f ../k8s/kafka.yaml" -ForegroundColor White
Write-Host "2. Wait for pods to be ready: kubectl get pods -n kafka" -ForegroundColor White
Write-Host "3. Run the test script: .\test-with-jwt.ps1" -ForegroundColor White
Write-Host ""
Write-Host "To access Kafka UI after deployment:" -ForegroundColor Cyan
Write-Host "kubectl port-forward -n kafka svc/kafka-ui 8081:80" -ForegroundColor White
Write-Host "Then open: http://localhost:8081" -ForegroundColor White 