# Elasticsearch Quick Start Guide

## 🚀 Quick Setup (5 minutes)

### Prerequisites
- Docker and Docker Compose installed
- Java 21+ (for running Spring Boot)
- Maven (for building the project)

---

## Option 1: Docker Compose (Recommended for Development)

### Start Services
```bash
# Navigate to backend directory
cd ecommerce-backend

# Start Elasticsearch and PostgreSQL
docker-compose up -d

# Verify Elasticsearch is running
curl http://localhost:9200

# Expected output
{
  "name" : "abc123def",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "...",
  "version" : {
    "number" : "8.11.0",
    ...
  }
}
```

### Start Spring Boot
```bash
# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run
```

The backend will be available at: `http://localhost:8080`

---

## Option 2: Elasticsearch Only (if you have PostgreSQL already)

### Start Elasticsearch
```bash
docker run -d \
  --name elasticsearch \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  -p 9200:9200 \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0
```

### Verify Connection
```bash
curl http://localhost:9200
```

---

## 📝 Configuration

### Update `.env` (copy from `.env.example`)
```bash
cp .env.example .env
```

### Minimal Configuration
```env
ELASTICSEARCH_URI=http://localhost:9200
ELASTICSEARCH_USERNAME=
ELASTICSEARCH_PASSWORD=
ELASTICSEARCH_SSL_VERIFICATION_MODE=none
```

---

## 🔍 Test Search Functionality

### 1. Create a Product
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: multipart/form-data" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "name=Gaming Laptop" \
  -F "description=High-performance gaming laptop" \
  -F "price=1299.99" \
  -F "categoryId=1" \
  -F "stock=10"
```

### 2. Search by Name
```bash
curl http://localhost:8080/api/v1/products/search/name?name=laptop
```

### 3. Advanced Search
```bash
curl "http://localhost:8080/api/v1/products/search/advanced?query=laptop&categoryId=1&minPrice=500&maxPrice=2000"
```

---

## 🛠️ Troubleshooting

### Elasticsearch not responding
```bash
# Check if running
curl http://localhost:9200

# View logs
docker logs elasticsearch

# Restart
docker restart elasticsearch
```

### Products not indexed
```bash
# Reindex all products (Admin only)
curl -X POST http://localhost:8080/api/v1/products/admin/reindex \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

### Connection refused
- Verify Elasticsearch is running: `docker ps | grep elasticsearch`
- Check port 9200: `netstat -an | grep 9200`
- Update `ELASTICSEARCH_URI` in `.env`

---

## 📚 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/products/search/name?name=...` | GET | Search by product name |
| `/api/v1/products/search/query?query=...` | GET | Search by name or description |
| `/api/v1/products/search/category/{id}` | GET | Search by category |
| `/api/v1/products/search/price` | GET | Search by price range |
| `/api/v1/products/search/target-group` | GET | Search by target group |
| `/api/v1/products/search/rating?minRating=...` | GET | Search by minimum rating |
| `/api/v1/products/search/advanced` | GET | Advanced search with multiple filters |
| `/api/v1/products/admin/reindex` | POST | Reindex all products |

---

## 🔌 Stop Services

```bash
# Stop all containers
docker-compose down

# OR just stop Elasticsearch
docker stop elasticsearch
```

---

## 📖 More Information

- [Full Elasticsearch Setup Guide](./ELASTICSEARCH_SETUP.md)
- [Docker Compose Reference](./docker-compose.yml)
- [API Documentation](./API_POSTMAN_TESTING_GUIDE.md)

---

## ✅ Verification Checklist

- [ ] Elasticsearch container running (`docker ps`)
- [ ] Spring Boot application started
- [ ] Can access `http://localhost:9200`
- [ ] Can access `http://localhost:8080`
- [ ] Created test product
- [ ] Search endpoint returns results
- [ ] Admin reindex endpoint works

---

Need help? Check `ELASTICSEARCH_SETUP.md` for detailed troubleshooting.
