package com.silvaldeweb.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String DEFAULT_ADMIN_PASSWORD = "change_me_admin_password";
    private static final String DEFAULT_CUSTOMER_PASSWORD = "change_me_customer_password";
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminRoleValue;
    private final String customerEmail;
    private final String customerPassword;
    private final String customerRoleValue;

    public DataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.users.admin.email:admin@example.com}") String adminEmail,
            @Value("${app.security.users.admin.password:}") String adminPassword,
            @Value("${app.security.users.admin.roles:ADMIN}") String adminRoleValue,
            @Value("${app.security.users.customer.email:customer@example.com}") String customerEmail,
            @Value("${app.security.users.customer.password:}") String customerPassword,
            @Value("${app.security.users.customer.roles:USER}") String customerRoleValue
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminRoleValue = adminRoleValue;
        this.customerEmail = customerEmail;
        this.customerPassword = customerPassword;
        this.customerRoleValue = customerRoleValue;
        validateSeedPasswords();
    }

    private void validateSeedPasswords() {
        if (DEFAULT_ADMIN_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException(
                    "APP_ADMIN_PASSWORD is the development default (change_me_admin_password). "
                            + "This is unsafe. Set APP_ADMIN_PASSWORD in your .env to a random string of at least "
                            + MIN_PASSWORD_LENGTH + " chars.");
        }
        if (DEFAULT_CUSTOMER_PASSWORD.equals(customerPassword)) {
            throw new IllegalStateException(
                    "APP_CUSTOMER_PASSWORD is the development default (change_me_customer_password). "
                            + "This is unsafe. Set APP_CUSTOMER_PASSWORD in your .env to a random string of at least "
                            + MIN_PASSWORD_LENGTH + " chars.");
        }
        if (adminPassword == null || adminPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "APP_ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters. "
                            + "Generate one with: openssl rand -base64 24 | head -c 32");
        }
        if (customerPassword == null || customerPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "APP_CUSTOMER_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters. "
                            + "Generate one with: openssl rand -base64 24 | head -c 32");
        }
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUser(adminEmail, adminPassword, parseFirstRole(adminRoleValue));
        seedUser(customerEmail, customerPassword, parseFirstRole(customerRoleValue));
    }

    private void seedUser(String email, String rawPassword, Role role) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.info("Seed user email='{}' already exists, skipping", normalizedEmail);
            return;
        }
        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .name(normalizedEmail.split("@")[0])
                .role(role)
                .active(true)
                .emailVerified(false)
                .build();
        userRepository.save(user);
        log.info("Seeded user email='{}' role={}", normalizedEmail, role);
    }

    private Role parseFirstRole(String rolesValue) {
        return Arrays.stream(rolesValue.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(String::toUpperCase)
                .map(this::safeValueOf)
                .filter(role -> role != null)
                .findFirst()
                .orElse(Role.USER);
    }

    private Role safeValueOf(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown role '{}' in env var, falling back to USER", value);
            return null;
        }
    }
}
