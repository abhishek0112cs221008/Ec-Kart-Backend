package com.ecommerce.ecommercebackend.ai.repository;

import com.ecommerce.ecommercebackend.Product.entity.Product;
import com.ecommerce.ecommercebackend.ai.entity.NegotiatedOffer;
import com.ecommerce.ecommercebackend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NegotiatedOfferRepository extends JpaRepository<NegotiatedOffer, UUID> {
    Optional<NegotiatedOffer> findByUserAndProductAndExpiryDateAfter(Users user, Product product, LocalDateTime now);
    List<NegotiatedOffer> findByUserAndExpiryDateAfter(Users user, LocalDateTime now);
}
