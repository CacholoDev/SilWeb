package com.silvaldeweb.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.silvaldeweb.config.JwtService;
import com.silvaldeweb.dto.AuthResponse;
import com.silvaldeweb.dto.LoginRequest;
import com.silvaldeweb.model.audit.Action;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.audit.AuditLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt email='{}'", request.email());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            User user = userRepository.findByEmailIgnoreCase(userDetails.getUsername()).orElse(null);
            if (user != null) {
                user.setLastLogin(Instant.now());
                userRepository.save(user);
            }
            auditLogService.record(user, Action.LOGIN, "User", user != null ? user.getId() : null, null, null);

            log.info("Login success email='{}' roles={}", userDetails.getUsername(), roles);
            return new AuthResponse(token, userDetails.getUsername(), roles, "Bearer");
        } catch (org.springframework.security.core.AuthenticationException exception) {
            log.warn("Login failed email='{}': {}", request.email(), exception.getMessage());
            throw new BadCredentialsException("Invalid email or password.");
        }
    }
}
