package com.ecommerce.ecommercebackend.Product.service;

import com.ecommerce.ecommercebackend.Product.dto.ProductRequest;
import com.ecommerce.ecommercebackend.Product.dto.ProductResponse;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public interface ProductService {

    ProductResponse createProduct(ProductRequest request, MultipartFile file , String sellerEmail) throws IOException;

    ProductResponse getProductById(UUID id);

    ProductResponse updateProduct(UUID id, ProductRequest request, MultipartFile file, String sellerEmail) throws IOException;

    MessageResponse deleteProduct(UUID id, String sellerEmail);

    List<ProductResponse> getAllProducts();

    List<ProductResponse> getMyProducts(String sellerEmail);
}

