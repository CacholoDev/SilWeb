package com.silvaldeweb.controller.address;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.silvaldeweb.dto.address.AddressCreateRequest;
import com.silvaldeweb.dto.address.AddressResponse;
import com.silvaldeweb.dto.address.AddressUpdateRequest;
import com.silvaldeweb.service.address.AddressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private static final Logger log = LoggerFactory.getLogger(AddressController.class);

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressCreateRequest request) {
        log.info("POST /api/addresses userId={}", request.userId());
        AddressResponse response = addressService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<AddressResponse> list(@RequestParam(required = false) Long userId) {
        log.info("GET /api/addresses userId={}", userId);
        return addressService.list(userId);
    }

    @GetMapping("/{id}")
    public AddressResponse get(@PathVariable Long id) {
        log.info("GET /api/addresses/{}", id);
        return addressService.get(id);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressUpdateRequest request) {
        log.info("PUT /api/addresses/{}", id);
        return addressService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/addresses/{}", id);
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
