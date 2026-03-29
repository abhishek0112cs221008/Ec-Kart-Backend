package com.ecommerce.ecommercebackend.Product.repository;

import com.ecommerce.ecommercebackend.Category.entity.Category;
import com.ecommerce.ecommercebackend.Product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsByNameIgnoreCase(String name);

    // check if their is a product in the category (for category deletion)
    boolean existsByCategory(Category category);

    // fetch all products belonging to a seller by their email
    List<Product> findBySellerEmail(String email);

}
