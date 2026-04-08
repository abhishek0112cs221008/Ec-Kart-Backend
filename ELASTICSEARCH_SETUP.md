# Elasticsearch Setup Guide for Ec-Kart Backend

## Overview
This guide explains how to set up and use Elasticsearch for full-text search functionality in the Ec-Kart e-commerce platform. Elasticsearch provides fast, scalable search capabilities for products.

---

## Table of Contents
1. [Installation](#installation)
2. [Configuration](#configuration)
3. [Running Elasticsearch](#running-elasticsearch)
4. [Indexing Products](#indexing-products)
5. [API Endpoints](#api-endpoints)
6. [Advanced Search](#advanced-search)
7. [Troubleshooting](#troubleshooting)

---

## Installation

### Option 1: Using Docker (Recommended)

#### Prerequisites
- Docker and Docker Compose installed

#### Steps

1. **Create or update `docker-compose.yml`** in the project root:

```yaml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    networks:
      - ecommerce-network

  ecommerce-backend:
    build:
      context: ./ecommerce-backend
      dockerfile: Dockerfile
    container_name: ecommerce-backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_ELASTICSEARCH_URIS=http://elasticsearch:9200
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ecommerce_db
    depends_on:
      - elasticsearch
      - postgres
    networks:
      - ecommerce-network

  postgres:
    image: postgres:15
    container_name: postgres
    environment:
      - POSTGRES_DB=ecommerce_db
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=change_me
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - ecommerce-network

volumes:
  elasticsearch_data:
  postgres_data:

networks:
  ecommerce-network:
```

2. **Start services**:
```bash
docker-compose up -d
```

3. **Verify Elasticsearch is running**:
```bash
curl http://localhost:9200
```

### Option 2: Manual Installation

#### For Windows/Mac/Linux

1. **Download Elasticsearch 8.11.0** from [elastic.co](https://www.elastic.co/downloads/elasticsearch)

2. **Extract and navigate** to the installation directory:
```bash
cd elasticsearch-8.11.0
```

3. **Run Elasticsearch**:
```bash
# Windows
bin\elasticsearch.bat

# Mac/Linux
bin/elasticsearch
```

4. **Verify installation**:
```bash
curl http://localhost:9200
```

---

## Configuration

### Update `application.properties`

Add or update the following properties in `src/main/resources/application.properties`:

```properties
# ============================================
# ELASTICSEARCH CONFIGURATION
# ============================================
spring.elasticsearch.uris=http://localhost:9200
spring.elasticsearch.username=
spring.elasticsearch.password=
```

### Environment Variables (for deployment)

For production deployments, use environment variables:

```bash
# For Docker/Kubernetes
ELASTICSEARCH_URI=https://elasticsearch.example.com:9200
ELASTICSEARCH_USERNAME=elastic
ELASTICSEARCH_PASSWORD=your_secure_password
```

---

## Running Elasticsearch

### Development Environment

1. **Start Docker containers** (if using Docker):
```bash
docker-compose up -d
```

2. **Start Spring Boot application**:
```bash
./mvnw spring-boot:run
```

3. **Verify connection**:
```bash
curl http://localhost:9200
```

### Production Environment

See `.env.production` file for production configuration variables.

---

## Indexing Products

### Automatic Indexing

Products are automatically indexed when:
- ✅ A new product is created
- ✅ A product is updated
- ✅ A product is deleted (removed from index)

### Manual Reindexing (Admin Only)

To reindex all products from the database to Elasticsearch:

```bash
# Admin endpoint to reindex all products
POST /api/v1/products/admin/reindex

# Response
{
  "message": "Reindexing started successfully"
}
```

**Requirements**: Must have `ADMIN` role

---

## API Endpoints

### Basic Search Endpoints

#### 1. Search by Name
```
GET /api/v1/products/search/name?name=laptop
```

**Description**: Search products by name  
**Parameters**: 
- `name` (required): Product name to search

**Example Response**:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Gaming Laptop Pro",
    "description": "High-performance gaming laptop",
    "price": 1299.99,
    "stock": 15
  }
]
```

#### 2. Search by Query (Name & Description)
```
GET /api/v1/products/search/query?query=wireless+headphones
```

**Description**: Search products by name and description  
**Parameters**:
- `query` (required): Search term

#### 3. Search by Category
```
GET /api/v1/products/search/category/1
```

**Description**: Get all products in a specific category  
**Parameters**:
- `categoryId` (path parameter): Category ID

#### 4. Search by Price Range
```
GET /api/v1/products/search/price?minPrice=100&maxPrice=500
```

**Description**: Find products within a price range  
**Parameters**:
- `minPrice` (optional): Minimum price (default: 0)
- `maxPrice` (optional): Maximum price (default: unlimited)

#### 5. Search by Target Group
```
GET /api/v1/products/search/target-group?targetGroup=women
```

**Description**: Find products for a specific target group  
**Parameters**:
- `targetGroup` (required): e.g., `men`, `women`, `kids`, `unisex`

#### 6. Search by Minimum Rating
```
GET /api/v1/products/search/rating?minRating=4.0
```

**Description**: Find products with a minimum average rating  
**Parameters**:
- `minRating` (required): Minimum rating (0-5)

---

## Advanced Search

### Complex Search with Multiple Filters
```
GET /api/v1/products/search/advanced?query=laptop&categoryId=2&minPrice=800&maxPrice=2000&targetGroup=unisex
```

**Description**: Perform advanced search with multiple filter criteria  
**Parameters** (all optional):
- `query`: Search term for name and description
- `categoryId`: Category ID
- `minPrice`: Minimum price
- `maxPrice`: Maximum price
- `targetGroup`: Target demographic

**Example Response**:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Gaming Laptop Pro",
    "description": "High-performance gaming laptop with RTX 4090",
    "price": 1899.99,
    "categoryId": 2,
    "categoryName": "Electronics",
    "targetGroup": "unisex",
    "averageRating": 4.5,
    "reviewCount": 125
  }
]
```

---

## Index Management

### Check Index Status
```bash
curl http://localhost:9200/products
```

### Delete Index (caution!)
```bash
curl -X DELETE http://localhost:9200/products
```

### View Index Mapping
```bash
curl http://localhost:9200/products/_mapping
```

### Search Index Directly (for debugging)
```bash
curl -X GET "localhost:9200/products/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match": {
      "name": "laptop"
    }
  }
}
'
```

---

## Performance Tips

### 1. Bulk Indexing
For large product imports, use the reindex endpoint to efficiently index multiple products:
```bash
POST /api/v1/products/admin/reindex
```

### 2. Index Settings
The index is configured with:
- **Shards**: 1 (for development)
- **Replicas**: 0 (for development)

For production, adjust in `ElasticsearchConfig.java`:
```java
// Modify index settings for production
// Increase shards and replicas for better performance
```

### 3. Caching
Elasticsearch caches frequently accessed data. Warm up the cache after deployment:
```bash
# Make a few search requests to warm up the cache
curl http://localhost:9200/products/_search?q=*
```

---

## Troubleshooting

### Issue: Connection Refused
**Error**: `Connection refused connecting to localhost:9200`

**Solutions**:
1. Verify Elasticsearch is running:
   ```bash
   curl http://localhost:9200
   ```
2. Check port is not blocked (default: 9200)
3. Verify firewall settings
4. Restart Elasticsearch service

### Issue: Index Not Updated
**Error**: Search results don't show recently created products

**Solutions**:
1. Check if Elasticsearch is connected:
   ```bash
   curl http://localhost:9200
   ```
2. Check application logs for indexing errors
3. Manually reindex products:
   ```bash
   POST /api/v1/products/admin/reindex
   ```
4. Verify product is `active=true`

### Issue: OutOfMemory Error
**Error**: `OutOfMemory` when indexing large datasets

**Solutions**:
1. Increase Elasticsearch heap memory:
   ```bash
   export ES_JAVA_OPTS="-Xms2g -Xmx2g"
   ```
2. For Docker:
   ```yaml
   environment:
     - "ES_JAVA_OPTS=-Xms2g -Xmx2g"
   ```

### Issue: Slow Queries
**Problem**: Search queries are slow

**Solutions**:
1. Check index stats:
   ```bash
   curl http://localhost:9200/_nodes/stats
   ```
2. Monitor Elasticsearch performance:
   ```bash
   curl http://localhost:9200/_cat/indices?v
   ```
3. Consider increasing memory allocation to Elasticsearch
4. Enable query caching in ElasticsearchConfig

### Debug Mode
Enable debug logging in `application.properties`:
```properties
logging.level.com.ecommerce.ecommercebackend.Product=DEBUG
logging.level.org.springframework.data.elasticsearch=DEBUG
```

---

## Database Sync

### Keeping DB and Elasticsearch in Sync

The application automatically syncs products:
- ✅ Product created → Auto-indexed
- ✅ Product updated → Auto-reindexed
- ✅ Product deleted → Removed from index

### Manual Sync (if needed)

```bash
# Reindex all products from database
POST /api/v1/products/admin/reindex
```

---

## Next Steps

1. **Frontend Integration**: Update search UI to use new endpoints
2. **Analytics**: Track search trends and popular queries
3. **A/B Testing**: Test different search ranking algorithms
4. **Multi-language**: Support search in multiple languages
5. **Autocomplete**: Implement search suggestions using Elasticsearch

---

## Related Documentation

- [Elasticsearch Official Docs](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Spring Data Elasticsearch](https://spring.io/projects/spring-data-elasticsearch)
- [REST API Reference](#api-endpoints)

---

## Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review Spring Boot logs: `./mvnw clean spring-boot:run`
3. Check Elasticsearch logs: Docker logs or installation directory
4. Contact the development team

---

**Last Updated**: April 2026
