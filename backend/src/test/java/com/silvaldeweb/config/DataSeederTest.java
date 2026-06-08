package com.silvaldeweb.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void seedsAdminAndCustomerWhenMissing() {
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        DataSeeder seeder = new DataSeeder(
                userRepository, passwordEncoder,
                "admin@example.com", "adminPass", "ADMIN",
                "customer@example.com", "customerPass", "USER"
        );

        seeder.run();

        verify(userRepository, times(2)).save(any(User.class));
        verify(passwordEncoder).encode("adminPass");
        verify(passwordEncoder).encode("customerPass");
    }

    @Test
    void skipsExistingUsers() {
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        DataSeeder seeder = new DataSeeder(
                userRepository, passwordEncoder,
                "admin@example.com", "adminPass", "ADMIN",
                "customer@example.com", "customerPass", "USER"
        );

        seeder.run();

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }
}
