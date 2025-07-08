# e-Banking Transaction Service - Kubernetes Deployment

This directory contains Kubernetes manifests for deploying the e-Banking Transaction Service microservice with Kafka integration, monitoring, and auto-scaling capabilities.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Kubernetes Cluster                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐  │
│  │   Ingress       │    │   Load Balancer │    │   Monitoring│  │
│  │   Controller    │    │   (HPA)         │    │   Stack     │  │
│  └─────────────────┘    └─────────────────┘    └─────────────┘  │
│           │                       │                       │     │
│           ▼                       ▼                       ▼     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    ebanking namespace                       │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │ │
│  │  │ Transaction     │  │ Transaction     │  │ ConfigMap   │ │ │
│  │  │ Service         │  │ Service         │  │ & Secrets   │ │ │
│  │  │ (Deployment)    │  │ (Service)       │  │             │ │ │
│  │  └─────────────────┘  └─────────────────┘  └─────────────┘ │ │
│  └─────────────────────────────────────────────────────────────┘ │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                     kafka namespace                         │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │ │
│  │  │ Zookeeper   │  │   Kafka     │  │     Kafka UI        │ │ │
│  │  │ (StatefulSet)│  │(StatefulSet)│  │   (Deployment)     │ │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## 📁 File Structure

```
k8s/
├── namespace.yaml          # Namespace definitions
├── configmap.yaml          # Application configuration
├── secret.yaml            # Sensitive data (JWT secrets, API keys)
├── deployment.yaml        # Application deployments (main + canary)
├── service.yaml           # Service definitions
├── ingress.yaml           # Ingress configuration with SSL
├── hpa.yaml              # Horizontal Pod Autoscaler
├── kafka.yaml            # Kafka cluster deployment
├── monitoring.yaml       # Prometheus, Grafana, alerts
├── deploy.sh             # Deployment automation script
└── README.md             # This file
```

## 🚀 Quick Start

### Prerequisites

1. **Kubernetes Cluster** (1.20+)
   - Minikube, Docker Desktop, or cloud provider (GKE, EKS, AKS)
   - Ingress controller (NGINX Ingress)
   - Cert-manager (for SSL certificates)

2. **kubectl** configured to access your cluster

3. **Docker** (optional, for building images)

### 1. Build and Deploy

```bash
# Make the deployment script executable
chmod +x k8s/deploy.sh

# Deploy everything
./k8s/deploy.sh deploy
```

### 2. Deploy Components Individually

```bash
# Deploy only Kafka
./k8s/deploy.sh kafka

# Deploy only application configuration
./k8s/deploy.sh config

# Deploy only the application
./k8s/deploy.sh app

# Deploy only monitoring
./k8s/deploy.sh monitoring
```

### 3. Check Status

```bash
# View deployment status
./k8s/deploy.sh status

# View application logs
./k8s/deploy.sh logs
```

## 🔧 Configuration

### Environment Variables

The application uses the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | JWT signing secret | From Kubernetes Secret |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `kubernetes` |
| `KUBERNETES_NAMESPACE` | Kubernetes namespace | Auto-detected |
| `POD_NAME` | Pod name | Auto-detected |
| `POD_IP` | Pod IP | Auto-detected |

### Resource Limits

| Component | CPU Request | CPU Limit | Memory Request | Memory Limit |
|-----------|-------------|-----------|----------------|--------------|
| Transaction Service | 250m | 500m | 512Mi | 1Gi |
| Kafka Broker | 500m | 1000m | 1Gi | 2Gi |
| Zookeeper | 250m | 500m | 512Mi | 1Gi |
| Kafka UI | 100m | 200m | 256Mi | 512Mi |

### Scaling Configuration

- **Min Replicas**: 3
- **Max Replicas**: 10
- **CPU Threshold**: 70%
- **Memory Threshold**: 80%

## 📊 Monitoring & Observability

### Prometheus Metrics

The application exposes metrics at `/actuator/prometheus`:

- HTTP request rates and response times
- JVM memory and GC metrics
- Kafka consumer lag
- Custom business metrics

### Grafana Dashboards

Pre-configured dashboards include:

- **Transaction Service Overview**: Request rates, response times, error rates
- **JVM Metrics**: Memory usage, GC performance
- **Kafka Metrics**: Consumer lag, throughput
- **Infrastructure**: CPU, memory, network usage

### Alerting Rules

Configured alerts:

- **TransactionServiceDown**: Service unavailable
- **HighResponseTime**: Response time > 2s (95th percentile)
- **HighErrorRate**: Error rate > 5%
- **KafkaConsumerLag**: Consumer lag > 1000 messages

## 🔐 Security

### Network Security

- **Ingress**: SSL/TLS termination with Let's Encrypt certificates
- **Services**: ClusterIP (internal access only)
- **Pods**: Network policies (if configured)

### Application Security

- **JWT Authentication**: Required for API endpoints
- **Secrets Management**: Kubernetes Secrets for sensitive data
- **RBAC**: Service accounts with minimal required permissions

### Container Security

- **Non-root user**: Application runs as `appuser`
- **Read-only filesystem**: Configuration mounted as read-only
- **Resource limits**: Prevents resource exhaustion attacks

## 🌐 Access Points

### External Access

| Service | URL | Description |
|---------|-----|-------------|
| Transaction API | `https://api.ebanking.com/api/v1/*` | Main API endpoints |
| Swagger UI | `https://api.ebanking.com/swagger-ui/` | API documentation |
| Health Check | `https://api.ebanking.com/actuator/health` | Health status |
| Test Endpoints | `https://api.ebanking.com/test/*` | Testing endpoints |

### Internal Access

| Service | URL | Description |
|---------|-----|-------------|
| Transaction Service | `transaction-service.ebanking.svc.cluster.local:80` | Internal service |
| Kafka | `kafka.kafka.svc.cluster.local:9092` | Kafka brokers |
| Kafka UI | `kafka-ui.kafka.svc.cluster.local:80` | Kafka management UI |

## 🔄 Deployment Strategies

### Rolling Updates

```bash
# Update to new version
kubectl set image deployment/transaction-service \
  transaction-service=ebanking/transaction-service:v1.1.0 \
  -n ebanking
```

### Canary Deployment

The deployment includes a canary version that receives 10% of traffic:

```bash
# Adjust canary weight
kubectl patch ingress transaction-service-canary-ingress \
  -p '{"metadata":{"annotations":{"nginx.ingress.kubernetes.io/canary-weight":"20"}}}' \
  -n ebanking
```

### Blue-Green Deployment

```bash
# Deploy new version
kubectl apply -f k8s/deployment-v2.yaml

# Switch traffic
kubectl patch service transaction-service \
  -p '{"spec":{"selector":{"version":"v2"}}}' \
  -n ebanking
```

## 🛠️ Troubleshooting

### Common Issues

#### 1. Pods Not Starting

```bash
# Check pod status
kubectl get pods -n ebanking

# Check pod events
kubectl describe pod <pod-name> -n ebanking

# Check pod logs
kubectl logs <pod-name> -n ebanking
```

#### 2. Kafka Connection Issues

```bash
# Check Kafka pods
kubectl get pods -n kafka

# Check Kafka logs
kubectl logs -l app=kafka -n kafka

# Test Kafka connectivity
kubectl exec -it <transaction-service-pod> -n ebanking -- \
  curl -f http://kafka.kafka.svc.cluster.local:9092
```

#### 3. Ingress Not Working

```bash
# Check ingress status
kubectl get ingress -n ebanking

# Check ingress controller
kubectl get pods -n ingress-nginx

# Check SSL certificate
kubectl get certificates -n ebanking
```

#### 4. High Resource Usage

```bash
# Check resource usage
kubectl top pods -n ebanking

# Check HPA status
kubectl get hpa -n ebanking

# Check metrics
kubectl get --raw /apis/metrics.k8s.io/v1beta1/namespaces/ebanking/pods
```

### Debug Commands

```bash
# Port forward to access services locally
kubectl port-forward svc/transaction-service 8080:80 -n ebanking

# Access Kafka UI
kubectl port-forward svc/kafka-ui 8081:80 -n kafka

# Execute commands in pods
kubectl exec -it <pod-name> -n ebanking -- /bin/bash

# View real-time logs
kubectl logs -f -l app=ebanking-transaction-service -n ebanking
```

## 📈 Performance Tuning

### JVM Tuning

```yaml
env:
- name: JAVA_OPTS
  value: "-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Kafka Tuning

```yaml
env:
- name: KAFKA_NUM_NETWORK_THREADS
  value: "8"
- name: KAFKA_NUM_IO_THREADS
  value: "8"
- name: KAFKA_SOCKET_SEND_BUFFER_BYTES
  value: "102400"
```

### Resource Optimization

```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"
```

## 🔄 CI/CD Integration

### GitHub Actions Example

```yaml
name: Deploy to Kubernetes
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Build and push image
      run: |
        docker build -t ${{ secrets.REGISTRY }}/transaction-service:${{ github.sha }} .
        docker push ${{ secrets.REGISTRY }}/transaction-service:${{ github.sha }}
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/transaction-service \
          transaction-service=${{ secrets.REGISTRY }}/transaction-service:${{ github.sha }} \
          -n ebanking
```

## 📚 Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [Kafka on Kubernetes](https://strimzi.io/)
- [Prometheus Operator](https://prometheus-operator.dev/)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test the deployment
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details. 