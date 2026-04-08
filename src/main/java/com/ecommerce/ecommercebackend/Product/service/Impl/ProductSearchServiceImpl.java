package com.ecommerce.ecommercebackend.Product.service.Impl;

import com.ecommerce.ecommercebackend.Product.dto.ProductResponse;
import com.ecommerce.ecommercebackend.Product.elasticsearch.ProductES;
import com.ecommerce.ecommercebackend.Product.entity.Product;
import com.ecommerce.ecommercebackend.Product.repository.ProductRepository;
import com.ecommerce.ecommercebackend.Product.repository.elasticsearch.ProductESRepository;
import com.ecommerce.ecommercebackend.Product.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implementation of ProductSearchService using Elasticsearch
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductESRepository esRepository;
    private final ProductRepository dbRepository;
    private final ReviewServiceHelper reviewServiceHelper; // Helper to get review info

    @Override
    public List<ProductResponse> searchByName(String name) {
        log.info("Searching products by name: {}", name);
        return esRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> searchByNameOrDescription(String query) {
        log.info("Searching products by name or description: {}", query);
        return esRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query)
                .stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> searchByCategory(Long categoryId) {
        log.info("Searching products by category: {}", categoryId);
        return esRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> searchByPriceRange(Double minPrice, Double maxPrice) {
        log.info("Searching products by price range: {} - {}", minPrice, maxPrice);
        return esRepository.findByPriceBetween(minPrice, maxPrice)
                .stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> searchByTargetGroup(String targetGroup) {
        log.info("Searching products by target group: {}", targetGroup);
        return esRepository.findByTargetGroup(targetGroup)
                .stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> searchByMinRating(Float minRating) {
        log.info("Searching products with minimum rating: {}", minRating);
        return esRepository.findByAverageRatingGreaterThanEqual(minRating)
                .stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> advancedSearch(String query, Long categoryId, Double minPrice, Double maxPrice,
            String targetGroup) {
        log.info("Performing advanced search with query: {}, category: {}, price: {}-{}, targetGroup: {}",
                query, categoryId, minPrice, maxPrice, targetGroup);

        List<ProductES> results = StreamSupport.stream(esRepository.findAll().spliterator(), false)
                .filter(p -> p.getActive() != null && p.getActive()) // Only active products
                .filter(p -> query == null || p.getName().toLowerCase().contains(query.toLowerCase())
                        || (p.getDescription() != null
                                && p.getDescription().toLowerCase().contains(query.toLowerCase())))
                .filter(p -> categoryId == null || p.getCategoryId().equals(categoryId))
                .filter(p -> minPrice == null || p.getPrice() >= minPrice)
                .filter(p -> maxPrice == null || p.getPrice() <= maxPrice)
                .filter(p -> targetGroup == null
                        || (p.getTargetGroup() != null && p.getTargetGroup().equals(targetGroup)))
                .collect(Collectors.toList());

        return results.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void indexProduct(ProductResponse productResponse) {
        log.info("Indexing product: {} with id: {}", productResponse.getName(), productResponse.getId());
        try {
            ProductES productES = convertToProductES(productResponse);
            esRepository.save(productES);
            log.info("Successfully indexed product: {}", productResponse.getId());
        } catch (Exception e) {
            log.error("Error indexing product: {}", productResponse.getId(), e);
        }
    }

    @Override
    public void deleteProductFromIndex(String productId) {
        log.info("Deleting product from index: {}", productId);
        try {
            esRepository.deleteById(productId);
            log.info("Successfully deleted product from index: {}", productId);
        } catch (Exception e) {
            log.error("Error deleting product from index: {}", productId, e);
        }
    }

    @Override
    public void reindexAllProducts() {
        log.info("Starting reindexing of all products");
        try {
            // Clear existing index
            esRepository.deleteAll();
            log.info("Cleared Elasticsearch index");

            // Fetch all products from database
            List<Product> allProducts = dbRepository.findAll();
            log.info("Found {} products to reindex", allProducts.size());

            // Convert and save to Elasticsearch
            List<ProductES> productESList = allProducts.stream()
                    .map(this::convertProductToProductES)
                    .collect(Collectors.toList());

            esRepository.saveAll(productESList);
            log.info("Successfully reindexed {} products", productESList.size());
        } catch (Exception e) {
            log.error("Error reindexing all products", e);
        }
    }

    @Override
    public List<ProductResponse> getAllIndexedProducts() {
        log.info("Fetching all indexed products");
        return esRepository.findByActiveTrue()
                .stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert ProductES to ProductResponse
     */
    private ProductResponse convertToProductResponse(ProductES productES) {
        return ProductResponse.builder()
                .id(parseUuid(productES.getId()))
                .name(productES.getName())
                .description(productES.getDescription())
                .price(toBigDecimal(productES.getPrice()))
                .floorPrice(toBigDecimal(productES.getFloorPrice()))
                .stock(productES.getStock())
                .categoryId(productES.getCategoryId())
                .categoryName(productES.getCategoryName())
                .imageUrl(productES.getImageUrl())
                .targetGroup(productES.getTargetGroup())
                .active(productES.getActive())
                .sellerId(parseUuid(productES.getSellerId()))
                .sellerEmail(productES.getSellerEmail())
                .averageRating(toDouble(productES.getAverageRating()))
                .reviewCount(toLong(productES.getReviewCount()))
                .message("Search result")
                .build();
    }

    /**
     * Convert ProductResponse to ProductES
     */
    private ProductES convertToProductES(ProductResponse productResponse) {
        return ProductES.builder()
                .id(productResponse.getId() != null ? productResponse.getId().toString() : null)
                .name(productResponse.getName())
                .description(productResponse.getDescription())
                .price(toDouble(productResponse.getPrice()))
                .floorPrice(toDouble(productResponse.getFloorPrice()))
                .stock(productResponse.getStock())
                .categoryId(productResponse.getCategoryId())
                .categoryName(productResponse.getCategoryName())
                .imageUrl(productResponse.getImageUrl())
                .targetGroup(productResponse.getTargetGroup())
                .active(productResponse.getActive())
                .sellerId(productResponse.getSellerId() != null ? productResponse.getSellerId().toString() : null)
                .sellerEmail(productResponse.getSellerEmail())
                .averageRating(toFloat(productResponse.getAverageRating()))
                .reviewCount(toInteger(productResponse.getReviewCount()))
                .build();
    }

    /**
     * Convert Product entity to ProductES
     */
    private ProductES convertProductToProductES(Product product) {
        return ProductES.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice().doubleValue())
                .floorPrice(product.getFloorPrice() != null ? product.getFloorPrice().doubleValue() : null)
                .stock(product.getStock())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .imageUrl(product.getImageUrl())
                .targetGroup(product.getTargetGroup())
                .active(product.getActive())
                .sellerId(product.getSeller().getId().toString())
                .sellerEmail(product.getSeller().getEmail())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .averageRating(0f)
                .reviewCount(0)
                .build();
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    private Double toDouble(Float value) {
        return value != null ? value.doubleValue() : null;
    }

    private Float toFloat(Double value) {
        return value != null ? value.floatValue() : null;
    }

    private Long toLong(Integer value) {
        return value != null ? value.longValue() : null;
    }

    private Integer toInteger(Long value) {
        return value != null ? value.intValue() : null;
    }
}

/**
 * Helper class to manage review service calls
 */
@Slf4j
@org.springframework.stereotype.Component
@RequiredArgsConstructor
class ReviewServiceHelper {

    // This can be injected later if ReviewService exists
    public Float getAverageRating(String productId) {
        // Implement review service call here
        return 0f;
    }

    public Integer getReviewCount(String productId) {
        // Implement review service call here
        return 0;
    }
}
