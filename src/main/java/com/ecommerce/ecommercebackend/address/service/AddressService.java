package com.ecommerce.ecommercebackend.address.service;

import com.ecommerce.ecommercebackend.address.dto.AddressDTO;
import java.util.List;

public interface AddressService {
    AddressDTO addAddress(String userEmail, AddressDTO addressDTO);
    List<AddressDTO> getUserAddresses(String userEmail);
    AddressDTO updateAddress(Long addressId, String userEmail, AddressDTO addressDTO);
    void deleteAddress(Long addressId, String userEmail);
    AddressDTO setDefaultAddress(Long addressId, String userEmail);
}
