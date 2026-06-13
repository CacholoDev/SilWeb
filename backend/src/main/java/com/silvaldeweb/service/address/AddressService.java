package com.silvaldeweb.service.address;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
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
    public AddressResponse create(User actor, AddressCreateRequest request) {
        log.info("Creating address userId={} postalCode='{}'", actor.getId(), request.postalCode());
        User user = userRepository.findById(actor.getId())
                .orElseThrow(() -> new UserNotFoundException(actor.getId()));

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
    public List<AddressResponse> list(User actor, boolean isAdmin, Long userId) {
        if (isAdmin) {
            log.info("ADMIN listing all addresses requestedBy userId={} filterUserId={}", actor.getId(), userId);
            List<Address> addresses = userId == null
                    ? addressRepository.findAll()
                    : addressRepository.findByUserId(userId);
            log.info("Found {} addresses", addresses.size());
            return addresses.stream().map(this::toResponse).toList();
        }
        log.info("Listing addresses for owner userId={}", actor.getId());
        List<Address> addresses = addressRepository.findByUserId(actor.getId());
        log.info("Found {} addresses", addresses.size());
        return addresses.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse get(User actor, boolean isAdmin, Long id) {
        log.info("Getting address id={} userId={} isAdmin={}", id, actor.getId(), isAdmin);
        Address address = findById(id);
        if (!isAdmin && !address.getUser().getId().equals(actor.getId())) {
            log.warn("Access denied: userId={} tried to read address id={} owned by userId={}",
                    actor.getId(), id, address.getUser().getId());
            throw new AccessDeniedException("You can only access your own addresses.");
        }
        return toResponse(address);
    }

    @Transactional
    public AddressResponse update(User actor, boolean isAdmin, Long id, AddressUpdateRequest request) {
        log.info("Updating address id={} userId={} isAdmin={}", id, actor.getId(), isAdmin);
        Address address = findById(id);
        if (!isAdmin && !address.getUser().getId().equals(actor.getId())) {
            log.warn("Access denied: userId={} tried to update address id={} owned by userId={}",
                    actor.getId(), id, address.getUser().getId());
            throw new AccessDeniedException("You can only update your own addresses.");
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
    public void delete(User actor, boolean isAdmin, Long id) {
        log.info("Deleting address id={} userId={} isAdmin={}", id, actor.getId(), isAdmin);
        try {
            Address address = findById(id);
            if (!isAdmin && !address.getUser().getId().equals(actor.getId())) {
                log.warn("Access denied: userId={} tried to delete address id={} owned by userId={}",
                        actor.getId(), id, address.getUser().getId());
                throw new AccessDeniedException("You can only delete your own addresses.");
            }
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
