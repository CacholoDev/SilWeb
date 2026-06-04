package com.silvaldeweb.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginReturnsJwtAndUserData() {
        LoginRequest request = new LoginRequest("admin", "secret123");
        UserDetails userDetails = User.withUsername("admin")
                .password("encoded-password")
                .roles("ADMIN")
                .build();

        // Arrange mocks so AuthService can run without Spring or a database.
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert response contents
        assertEquals("jwt-token", response.token());
        assertEquals("admin", response.username());
        assertEquals(List.of("ROLE_ADMIN"), response.roles());
        assertEquals("Bearer", response.tokenType());

        // Assert the exact credentials used to authenticate.
        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("admin", captor.getValue().getPrincipal());
        assertEquals("secret123", captor.getValue().getCredentials());
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void loginWrapsBadCredentialsAsUnauthorizedFailure() {
        LoginRequest request = new LoginRequest("admin", "wrong-password");

        // Arrange: authentication fails.
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid login"));

        // Act + Assert: service surfaces a controlled error message.
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid username or password.", exception.getMessage());

        // Token generation must not happen on failed login.
        verify(jwtService, never()).generateToken(any());
    }
}