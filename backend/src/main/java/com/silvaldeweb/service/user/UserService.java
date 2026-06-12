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
import com.silvaldeweb.model.audit.Action;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.audit.AuditLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public UserResponse create(UserCreateRequest request, User actor) {
        log.info("Creating user email='{}' role={} actor={}", request.email(), request.role(),
                actor != null ? actor.getEmail() : "null");
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
        auditLogService.record(actor, Action.CREATE, "User", saved.getId(),
                "email=" + saved.getEmail() + " role=" + saved.getRole(), null);
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
    public UserResponse update(Long id, UserUpdateRequest request, User actor) {
        log.info("Updating user id={} newEmail='{}' actor={}", id, request.email(),
                actor != null ? actor.getEmail() : "null");
        User user = findById(id);
        String email = request.email().trim().toLowerCase();

        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("User email='{}' already exists, refusing to update", email);
            throw new UserAlreadyExistsException(email);
        }

        String previousEmail = user.getEmail();
        Role previousRole = user.getRole();
        Boolean previousActive = user.getActive();
        boolean passwordChanged = request.password() != null && !request.password().isBlank();

        user.setEmail(email);
        user.setName(request.name());
        user.setPhone(request.phone());

        if (passwordChanged) {
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
        StringBuilder diff = new StringBuilder();
        if (!previousEmail.equals(saved.getEmail())) {
            diff.append("email: ").append(previousEmail).append(" -> ").append(saved.getEmail()).append("; ");
        }
        if (previousRole != saved.getRole()) {
            diff.append("role: ").append(previousRole).append(" -> ").append(saved.getRole()).append("; ");
        }
        if (previousActive != null && !previousActive.equals(saved.getActive())) {
            diff.append("active: ").append(previousActive).append(" -> ").append(saved.getActive()).append("; ");
        }
        if (passwordChanged) {
            diff.append("password changed; ");
        }
        if (diff.length() > 0) {
            auditLogService.record(actor, Action.UPDATE, "User", saved.getId(), diff.toString().trim(), null);
        }
        log.info("User updated id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, User actor) {
        log.info("Deleting user id={} actor={}", id, actor != null ? actor.getEmail() : "null");
        try {
            User user = findById(id);
            String metadata = "email=" + user.getEmail() + " role=" + user.getRole();
            log.info("User id={} found, executing delete", id);
            userRepository.delete(user);
            auditLogService.record(actor, Action.DELETE, "User", id, metadata, null);
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
