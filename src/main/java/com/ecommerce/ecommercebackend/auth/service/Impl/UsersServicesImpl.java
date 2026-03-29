package com.ecommerce.ecommercebackend.auth.service.Impl;

import com.ecommerce.ecommercebackend.auth.service.UsersServices;
import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.repository.UsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsersServicesImpl implements UsersServices {

    @Autowired
    private UsersRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name()) // important: ROLE_ prefix
                .build();
    }
}

