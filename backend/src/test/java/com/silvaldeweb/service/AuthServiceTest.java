package com.silvaldeweb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.silvaldeweb.config.JwtService;
import com.silvaldeweb.dto.AuthResponse;
import com.silvaldeweb.dto.LoginRequest;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.audit.AuditLogService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginReturnsJwtAndUserData() {
        LoginRequest request = new LoginRequest("admin@example.com", "secret123");
        UserDetails userDetails = User.withUsername("admin@example.com")
                .password("encoded-password")
                .roles("ADMIN")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(java.util.Optional.of(
                        com.silvaldeweb.model.user.User.builder()
                                .id(1L).email("admin@example.com").role(Role.ADMIN).active(true).build()));

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.token());
        assertEquals("admin@example.com", response.email());
        assertEquals(List.of("ROLE_ADMIN"), response.roles());
        assertEquals("Bearer", response.tokenType());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("admin@example.com", captor.getValue().getPrincipal());
        assertEquals("secret123", captor.getValue().getCredentials());
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void loginWrapsBadCredentialsAsUnauthorizedFailure() {
        LoginRequest request = new LoginRequest("admin@example.com", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid login"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid email or password.", exception.getMessage());

        verify(jwtService, never()).generateToken(any());
    }
}
