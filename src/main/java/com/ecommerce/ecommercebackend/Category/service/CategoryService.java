package com.ecommerce.ecommercebackend.Category.service;

import com.ecommerce.ecommercebackend.Category.dto.CategoryRequest;
import com.ecommerce.ecommercebackend.Category.dto.CategoryResponse;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;

import java.util.List;


public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse getCategoryById(Long id);

    List<CategoryResponse> getAllCategories();

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    MessageResponse deleteCategory(Long id);
}

