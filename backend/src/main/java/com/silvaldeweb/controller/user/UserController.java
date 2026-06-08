package com.silvaldeweb.controller.user;

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

import com.silvaldeweb.dto.user.UserCreateRequest;
import com.silvaldeweb.dto.user.UserResponse;
import com.silvaldeweb.dto.user.UserUpdateRequest;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.service.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        log.info("POST /api/users email='{}'", request.email());
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) Boolean active,
                                    @RequestParam(required = false) Role role) {
        log.info("GET /api/users active={} role={}", active, role);
        return userService.list(active, role);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        log.info("GET /api/users/{}", id);
        return userService.get(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        log.info("PUT /api/users/{}", id);
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/users/{}", id);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
