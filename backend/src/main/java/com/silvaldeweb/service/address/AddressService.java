package com.silvaldeweb.service.address;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.silvaldeweb.dto.address.AddressCreateRequest;
import com.silvaldeweb.dto.address.AddressResponse;
import com.silvaldeweb.dto.address.AddressUpdateRequest;
import com.silvaldeweb.exception.address.AddressNotFoundException;
import com.silvaldeweb.exception.user.UserNotFoundException;
import com.silvaldeweb.model.address.Address;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.address.AddressRepository;
import com.silvaldeweb.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressService.class);

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public AddressResponse create(AddressCreateRequest request) {
        log.info("Creating address userId={} postalCode='{}'", request.userId(), request.postalCode());
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Address address = Address.builder()
                .fullName(request.fullName())
                .street(request.street())
                .city(request.city())
                .province(request.province())
                .postalCode(request.postalCode())
                .country(request.country())
                .isDefault(request.isDefault() != null ? request.isDefault() : false)
                .user(user)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Address created id={} userId={}", saved.getId(), user.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        log.info("Listing addresses userId={}", userId);
        List<Address> addresses = userId == null
                ? addressRepository.findAll()
                : addressRepository.findByUserId(userId);
        log.info("Found {} addresses", addresses.size());
        return addresses.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse get(Long id) {
        log.info("Getting address id={}", id);
        return toResponse(findById(id));
    }

    @Transactional
    public AddressResponse update(Long id, AddressUpdateRequest request) {
        log.info("Updating address id={}", id);
        Address address = findById(id);

        if (!address.getUser().getId().equals(request.userId())) {
            User newUser = userRepository.findById(request.userId())
                    .orElseThrow(() -> new UserNotFoundException(request.userId()));
            address.setUser(newUser);
        }

        address.setFullName(request.fullName());
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setProvince(request.province());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        if (request.isDefault() != null) {
            address.setIsDefault(request.isDefault());
        }

        Address saved = addressRepository.save(address);
        log.info("Address updated id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting address id={}", id);
        try {
            Address address = findById(id);
            addressRepository.delete(address);
            log.info("Address id={} deleted", id);
        } catch (AddressNotFoundException exception) {
            log.warn("Delete failed: address id={} not found", id);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Delete failed for address id={}", id, exception);
            throw exception;
        }
    }

    private Address findById(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new AddressNotFoundException(id));
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getFullName(),
                address.getStreet(),
                address.getCity(),
                address.getProvince(),
                address.getPostalCode(),
                address.getCountry(),
                address.getIsDefault(),
                address.getUser() != null ? address.getUser().getId() : null,
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
