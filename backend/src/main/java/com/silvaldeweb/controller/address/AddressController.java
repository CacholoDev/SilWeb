package com.silvaldeweb.controller.address;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.silvaldeweb.config.AuthUtils;
import com.silvaldeweb.dto.address.AddressCreateRequest;
import com.silvaldeweb.dto.address.AddressResponse;
import com.silvaldeweb.dto.address.AddressUpdateRequest;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.address.AddressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private static final Logger log = LoggerFactory.getLogger(AddressController.class);

    private final AddressService addressService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressCreateRequest request,
                                                  Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        log.info("POST /api/addresses userId={} postalCode='{}'", actor.getId(), request.postalCode());
        AddressResponse response = addressService.create(actor, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<AddressResponse> list(@RequestParam(required = false) Long userId,
                                      Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("GET /api/addresses userId={} isAdmin={} filterUserId={}", actor.getId(), isAdmin, userId);
        return addressService.list(actor, isAdmin, userId);
    }

    @GetMapping("/{id}")
    public AddressResponse get(@PathVariable Long id, Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("GET /api/addresses/{} userId={} isAdmin={}", id, actor.getId(), isAdmin);
        return addressService.get(actor, isAdmin, id);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressUpdateRequest request,
                                  Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("PUT /api/addresses/{} userId={} isAdmin={}", id, actor.getId(), isAdmin);
        return addressService.update(actor, isAdmin, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("DELETE /api/addresses/{} userId={} isAdmin={}", id, actor.getId(), isAdmin);
        addressService.delete(actor, isAdmin, id);
        return ResponseEntity.noContent().build();
    }
}
