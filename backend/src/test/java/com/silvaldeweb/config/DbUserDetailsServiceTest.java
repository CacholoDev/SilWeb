package com.silvaldeweb.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class DbUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DbUserDetailsService service;

    @Test
    void loadUserByUsernameReturnsUserDetailsWithRoleAuthority() {
        User user = User.builder()
                .id(1L)
                .email("admin@example.com")
                .password("hashed-password")
                .name("Admin")
                .role(Role.ADMIN)
                .active(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin@example.com");

        assertEquals("admin@example.com", details.getUsername());
        assertEquals("hashed-password", details.getPassword());
        assertEquals(1, details.getAuthorities().size());
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsernameNormalizesEmailToLowercase() {
        User user = User.builder()
                .email("admin@example.com")
                .password("hashed")
                .role(Role.ADMIN)
                .active(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("ADMIN@EXAMPLE.COM");
        assertEquals("admin@example.com", details.getUsername());
    }

    @Test
    void loadUserByUsernameThrowsWhenNotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@example.com"));
    }

    @Test
    void loadUserByUsernameThrowsWhenUserInactive() {
        User user = User.builder()
                .email("inactive@example.com")
                .password("hashed")
                .role(Role.USER)
                .active(false)
                .build();

        when(userRepository.findByEmailIgnoreCase("inactive@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(org.springframework.security.authentication.DisabledException.class,
                () -> service.loadUserByUsername("inactive@example.com"));
    }
}
