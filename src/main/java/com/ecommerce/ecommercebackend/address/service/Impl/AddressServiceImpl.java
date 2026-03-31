package com.ecommerce.ecommercebackend.address.service.Impl;

import com.ecommerce.ecommercebackend.address.dto.AddressDTO;
import com.ecommerce.ecommercebackend.address.entity.Address;
import com.ecommerce.ecommercebackend.address.repository.AddressRepository;
import com.ecommerce.ecommercebackend.address.service.AddressService;
import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.UsersRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UsersRepo usersRepo;

    @Override
    @Transactional
    public AddressDTO addAddress(String userEmail, AddressDTO addressDTO) {
        Users user = usersRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Address address = Address.builder()
                .user(user)
                .fullName(addressDTO.getFullName())
                .phoneNumber(addressDTO.getPhoneNumber())
                .streetAddress(addressDTO.getStreetAddress())
                .city(addressDTO.getCity())
                .state(addressDTO.getState())
                .pinCode(addressDTO.getPinCode())
                .isDefault(addressDTO.isDefault())
                .build();

        if (address.isDefault()) {
            handleDefaultStatus(user);
        } else if (addressRepository.findAllByUserOrderByCreatedAtDesc(user).isEmpty()) {
            address.setDefault(true);
        }

        return toDto(addressRepository.save(address));
    }

    @Override
    public List<AddressDTO> getUserAddresses(String userEmail) {
        Users user = usersRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return addressRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long addressId, String userEmail, AddressDTO addressDTO) {
        Users user = usersRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        address.setFullName(addressDTO.getFullName());
        address.setPhoneNumber(addressDTO.getPhoneNumber());
        address.setStreetAddress(addressDTO.getStreetAddress());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setPinCode(addressDTO.getPinCode());

        if (addressDTO.isDefault() && !address.isDefault()) {
            handleDefaultStatus(user);
            address.setDefault(true);
        }

        return toDto(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId, String userEmail) {
        Users user = usersRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        
        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                    .findFirst()
                    .ifPresent(first -> {
                        first.setDefault(true);
                        addressRepository.save(first);
                    });
        }
    }

    @Override
    @Transactional
    public AddressDTO setDefaultAddress(Long addressId, String userEmail) {
        Users user = usersRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        handleDefaultStatus(user);
        address.setDefault(true);
        return toDto(addressRepository.save(address));
    }

    private void handleDefaultStatus(Users user) {
        List<Address> defaults = addressRepository.findAllByUserAndIsDefaultTrue(user);
        for (Address a : defaults) {
            a.setDefault(false);
            addressRepository.save(a);
        }
    }

    private AddressDTO toDto(Address a) {
        return AddressDTO.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phoneNumber(a.getPhoneNumber())
                .streetAddress(a.getStreetAddress())
                .city(a.getCity())
                .state(a.getState())
                .pinCode(a.getPinCode())
                .isDefault(a.isDefault())
                .build();
    }
}
