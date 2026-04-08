package com.ecommerce.ecommercebackend.Product.controller;

import com.ecommerce.ecommercebackend.Product.dto.ProductRequest;
import com.ecommerce.ecommercebackend.Product.dto.ProductResponse;
import com.ecommerce.ecommercebackend.Product.service.ProductService;
import com.ecommerce.ecommercebackend.Product.service.ProductSearchService;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("ALL")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;
    private final ProductSearchService productSearchService;


    /**
     * Create a new product.
     *
     * Accepts multipart/form-data to optionally include a product image.
     * Only accessible by users with SELLER role.
     *
     * @param authentication the current authenticated user (used to get seller email)
     * @param name the product name
     * @param description the product description
     * @param price the product price
     * @param categoryId the ID of the category the product belongs to
     * @param stock the available stock quantity
     * @param file optional product image
     * @return the created ProductResponse with all product details
     * @throws IOException if there is an error reading the uploaded file
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> createProduct(
            Authentication authentication,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Double price,
            @RequestParam(required = false) Double floorPrice,
            @RequestParam Long categoryId,
            @RequestParam Integer stock,
            @RequestParam(required = false) String targetGroup,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        ProductRequest request = ProductRequest.builder()
                .name(name).description(description).price(price)
                .floorPrice(floorPrice).categoryId(categoryId)
                .stock(stock).targetGroup(targetGroup).build();
        String sellerEmail = authentication.getName();
        ProductResponse resp = productService.createProduct(request, file, sellerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }


    /**
     * Get all products.
     *
     * Public endpoint that lists all products.
     *
     * @return list of ProductResponse objects
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(){
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * Get all products owned by the authenticated seller.
     *
     * @param authentication the current authenticated seller (email from JWT)
     * @return list of ProductResponse objects for this seller's products
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<ProductResponse>> getMyProducts(Authentication authentication) {
        return ResponseEntity.ok(productService.getMyProducts(authentication.getName()));
    }

    /**
     * Get a product by its ID.
     *
     * @param id the UUID of the product
     * @return ProductResponse for the requested product
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }


    /**
     * Update an existing product.
     *
     * Only accessible by the product owner (SELLER) or ADMIN.
     * Accepts multipart/form-data to optionally update product image.
     *
     * @param id the UUID of the product
     * @param authentication the current authenticated user (used to verify owner)
     * @param name optional new product name
     * @param description optional new product description
     * @param price optional new price
     * @param categoryId optional new category ID
     * @param stock optional new stock quantity
     * @param file optional new product image
     * @return updated ProductResponse
     * @throws IOException if there is an error reading the uploaded file
     */    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false) Double floorPrice,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) String targetGroup,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        ProductRequest request = ProductRequest.builder()
                .name(name).description(description).price(price)
                .floorPrice(floorPrice).categoryId(categoryId)
                .stock(stock).targetGroup(targetGroup).build();
        String sellerEmail = authentication.getName();
        ProductResponse resp = productService.updateProduct(id, request, file, sellerEmail);
        return ResponseEntity.ok(resp);
    }



    /**
     * Delete a product by its ID.
     *
     * Only accessible by the product owner (SELLER) or ADMIN.
     *
     * @param id the UUID of the product
     * @param authentication the current authenticated user (used to verify owner)
     * @return MessageResponse indicating deletion success
     */    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteProduct(
            @PathVariable UUID id,
            Authentication authentication) {

        String sellerEmail = authentication.getName();
        MessageResponse resp = productService.deleteProduct(id, sellerEmail);
        return ResponseEntity.ok(resp);
    }

    // ============================================
    // ELASTICSEARCH SEARCH ENDPOINTS
    // ============================================

    /**
     * Search products by name.
     *
     * @param name the product name to search for
     * @return list of matching ProductResponse objects
     */
    @GetMapping("/search/name")
    public ResponseEntity<List<ProductResponse>> searchByName(
            @RequestParam(required = false) String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductResponse> results = productSearchService.searchByName(name);
        return ResponseEntity.ok(results);
    }

    /**
     * Search products by name and/or description.
     *
     * @param query the search query
     * @return list of matching ProductResponse objects
     */
    @GetMapping("/search/query")
    public ResponseEntity<List<ProductResponse>> searchByQuery(
            @RequestParam(required = false) String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductResponse> results = productSearchService.searchByNameOrDescription(query);
        return ResponseEntity.ok(results);
    }

    /**
     * Search products by category.
     *
     * @param categoryId the category ID
     * @return list of products in the specified category
     */
    @GetMapping("/search/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> searchByCategory(
            @PathVariable Long categoryId) {
        List<ProductResponse> results = productSearchService.searchByCategory(categoryId);
        return ResponseEntity.ok(results);
    }

    /**
     * Search products by price range.
     *
     * @param minPrice minimum price
     * @param maxPrice maximum price
     * @return list of products within the price range
     */
    @GetMapping("/search/price")
    public ResponseEntity<List<ProductResponse>> searchByPriceRange(
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        List<ProductResponse> results = productSearchService.searchByPriceRange(
                minPrice != null ? minPrice : 0.0,
                maxPrice != null ? maxPrice : Double.MAX_VALUE
        );
        return ResponseEntity.ok(results);
    }

    /**
     * Search products by target group.
     *
     * @param targetGroup the target group (e.g., men, women, kids, unisex)
     * @return list of products for the specified target group
     */
    @GetMapping("/search/target-group")
    public ResponseEntity<List<ProductResponse>> searchByTargetGroup(
            @RequestParam(required = false) String targetGroup) {
        if (targetGroup == null || targetGroup.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductResponse> results = productSearchService.searchByTargetGroup(targetGroup);
        return ResponseEntity.ok(results);
    }

    /**
     * Search products by minimum rating.
     *
     * @param minRating minimum average rating
     * @return list of products with rating >= minRating
     */
    @GetMapping("/search/rating")
    public ResponseEntity<List<ProductResponse>> searchByMinRating(
            @RequestParam(required = false) Float minRating) {
        if (minRating == null || minRating < 0 || minRating > 5) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductResponse> results = productSearchService.searchByMinRating(minRating);
        return ResponseEntity.ok(results);
    }

    /**
     * Advanced search with multiple filters.
     *
     * @param query search query
     * @param categoryId category ID (optional)
     * @param minPrice minimum price (optional)
     * @param maxPrice maximum price (optional)
     * @param targetGroup target group (optional)
     * @return list of products matching all filters
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<List<ProductResponse>> advancedSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String targetGroup) {
        List<ProductResponse> results = productSearchService.advancedSearch(
                query, categoryId, minPrice, maxPrice, targetGroup
        );
        return ResponseEntity.ok(results);
    }

    /**
     * Reindex all products from database to Elasticsearch.
     *
     * Only accessible by ADMIN role.
     *
     * @return MessageResponse indicating reindexing status
     */
    @PostMapping("/admin/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> reindexAllProducts() {
        try {
            productSearchService.reindexAllProducts();
            return ResponseEntity.ok(new MessageResponse("Reindexing started successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error during reindexing: " + e.getMessage()));
        }
    }
}

