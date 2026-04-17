package com.ecommerce.ecommercebackend.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class GeneralController {

    @GetMapping("/")
    public String index() {
        return "Welcome to the E-commerce Backend API!";
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("message", "Backend is running smoothly");
        return status;
    }
}
