package com.ecommerce.ecommercebackend.address.repository;

import com.ecommerce.ecommercebackend.address.entity.Address;
import com.ecommerce.ecommercebackend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findAllByUserOrderByCreatedAtDesc(Users user);
    Optional<Address> findByIdAndUser(Long id, Users user);
    List<Address> findAllByUserAndIsDefaultTrue(Users user);
}
