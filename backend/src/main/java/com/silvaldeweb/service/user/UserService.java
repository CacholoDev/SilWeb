package com.silvaldeweb.service.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.silvaldeweb.dto.user.UserCreateRequest;
import com.silvaldeweb.dto.user.UserResponse;
import com.silvaldeweb.dto.user.UserUpdateRequest;
import com.silvaldeweb.exception.user.UserAlreadyExistsException;
import com.silvaldeweb.exception.user.UserNotFoundException;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        log.info("Creating user email='{}' role={}", request.email(), request.role());
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("User email='{}' already exists, refusing to create", email);
            throw new UserAlreadyExistsException(email);
        }

        User.UserBuilder builder = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .role(request.role() != null ? request.role() : Role.USER);

        if (request.active() != null) {
            builder.active(request.active());
        }

        User saved = userRepository.save(builder.build());
        log.info("User created id={} email='{}'", saved.getId(), saved.getEmail());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(Boolean active, Role role) {
        log.info("Listing users active={} role={}", active, role);
        List<User> users;
        if (active == null && role == null) {
            users = userRepository.findAll();
        } else if (active != null && role != null) {
            users = userRepository.findByActiveAndRole(active, role);
        } else if (active != null) {
            users = userRepository.findByActive(active);
        } else {
            users = userRepository.findByRole(role);
        }

        log.info("Found {} users", users.size());
        return users.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        log.info("Getting user id={}", id);
        return toResponse(findById(id));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        log.info("Updating user id={} newEmail='{}'", id, request.email());
        User user = findById(id);
        String email = request.email().trim().toLowerCase();

        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("User email='{}' already exists, refusing to update", email);
            throw new UserAlreadyExistsException(email);
        }

        user.setEmail(email);
        user.setName(request.name());
        user.setPhone(request.phone());

        if (request.password() != null && !request.password().isBlank()) {
            log.info("Updating password for user id={}", id);
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.active() != null) {
            user.setActive(request.active());
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        User saved = userRepository.save(user);
        log.info("User updated id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting user id={}", id);
        try {
            User user = findById(id);
            log.info("User id={} found, executing delete", id);
            userRepository.delete(user);
            log.info("User id={} deleted", id);
        } catch (UserNotFoundException exception) {
            log.warn("Delete failed: user id={} not found", id);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Delete failed for user id={}", id, exception);
            throw exception;
        }
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getActive(),
                user.getRole(),
                user.getLastLogin(),
                user.getEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
