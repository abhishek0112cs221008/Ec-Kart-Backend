package com.ecommerce.ecommercebackend.address.Controller;

import com.ecommerce.ecommercebackend.address.dto.AddressDTO;
import com.ecommerce.ecommercebackend.address.service.AddressService;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressDTO> addAddress(@RequestBody AddressDTO addressDTO, Authentication authentication) {
        return ResponseEntity.ok(addressService.addAddress(authentication.getName(), addressDTO));
    }

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getAddresses(Authentication authentication) {
        return ResponseEntity.ok(addressService.getUserAddresses(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long id, @RequestBody AddressDTO addressDTO, Authentication authentication) {
        return ResponseEntity.ok(addressService.updateAddress(id, authentication.getName(), addressDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteAddress(@PathVariable Long id, Authentication authentication) {
        addressService.deleteAddress(id, authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Address deleted successfully"));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<AddressDTO> setDefault(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(addressService.setDefaultAddress(id, authentication.getName()));
    }
}
