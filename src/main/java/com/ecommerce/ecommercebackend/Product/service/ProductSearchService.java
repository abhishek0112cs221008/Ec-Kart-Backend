package com.ecommerce.ecommercebackend.Product.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.ecommercebackend.Product.dto.ProductResponse;

/**
 * Service interface for Elasticsearch product search operations
 */
@Service
public interface ProductSearchService {

    /**
     * Search products by name
     */
    List<ProductResponse> searchByName(String name);

    /**
     * Search products by name and/or description
     */
    List<ProductResponse> searchByNameOrDescription(String query);

    /**
     * Search products by category
     */
    List<ProductResponse> searchByCategory(Long categoryId);

    /**
     * Search products by price range
     */
    List<ProductResponse> searchByPriceRange(Double minPrice, Double maxPrice);

    /**
     * Search products by target group
     */
    List<ProductResponse> searchByTargetGroup(String targetGroup);

    /**
     * Get products with minimum average rating
     */
    List<ProductResponse> searchByMinRating(Float minRating);

    /**
     * Complex search with multiple filters
     */
    List<ProductResponse> advancedSearch(String query, Long categoryId, Double minPrice, Double maxPrice, String targetGroup);

    /**
     * Index a product in Elasticsearch
     */
    void indexProduct(ProductResponse productResponse);

    /**
     * Delete a product from Elasticsearch index
     */
    void deleteProductFromIndex(String productId);

    /**
     * Reindex all products from database to Elasticsearch
     */
    void reindexAllProducts();

    /**
     * Get all indexed products
     */
    List<ProductResponse> getAllIndexedProducts();
}
