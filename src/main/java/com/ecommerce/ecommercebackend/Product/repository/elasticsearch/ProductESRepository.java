package com.ecommerce.ecommercebackend.Product.repository.elasticsearch;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.ecommercebackend.Product.elasticsearch.ProductES;

@Repository
public interface ProductESRepository extends ElasticsearchRepository<ProductES, String> {

    // ✅ Search for products by name (case-insensitive)
    List<ProductES> findByNameContainingIgnoreCase(String name);

    // ✅ Search by name OR description (case-insensitive)
    List<ProductES> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);

    // ✅ Find by category
    List<ProductES> findByCategoryId(Long categoryId);

    // ✅ Only active products
    List<ProductES> findByActiveTrue();

    // ✅ By seller email
    List<ProductES> findBySellerEmail(String sellerEmail);

    // ✅ Active products by seller
    List<ProductES> findBySellerEmailAndActiveTrue(String sellerEmail);

    // ✅ Products in stock
    List<ProductES> findByStockGreaterThan(Integer stock);

    // ✅ By target group
    List<ProductES> findByTargetGroup(String targetGroup);

    // ✅ Custom Elasticsearch query
    @Query("""
        {
          "bool": {
            "must": [
              { "match": { "name": "?0" } }
            ],
            "filter": [
              { "term": { "active": true } }
            ]
          }
        }
    """)
    List<ProductES> searchActiveByName(String name);

    // ✅ Price range
    List<ProductES> findByPriceBetween(Double minPrice, Double maxPrice);

    // ✅ Rating filter
    List<ProductES> findByAverageRatingGreaterThanEqual(Float minRating);
}