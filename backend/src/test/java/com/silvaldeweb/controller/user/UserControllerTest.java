package com.silvaldeweb.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silvaldeweb.dto.user.UserCreateRequest;
import com.silvaldeweb.dto.user.UserResponse;
import com.silvaldeweb.dto.user.UserUpdateRequest;
import com.silvaldeweb.exception.GlobalExceptionHandler;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.user.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    private Authentication adminAuth;
    private User adminUser;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminUser = User.builder().id(1L).email("admin@example.com").role(Role.ADMIN).active(true).build();
        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private UserResponse sampleResponse(Long id, String email) {
        return new UserResponse(
                id, email, "John Doe", "123456789",
                true, Role.USER, null, false,
                Instant.parse("2026-06-07T10:00:00Z"),
                Instant.parse("2026-06-07T10:00:00Z")
        );
    }

    @Test
    void createReturnsCreatedUser() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(adminUser));
        when(userService.create(any(UserCreateRequest.class), any(User.class)))
                .thenReturn(sampleResponse(1L, "user@example.com"));

        UserCreateRequest request = new UserCreateRequest(
                "user@example.com", "plainPassword", "John Doe", "123456789", true, Role.USER
        );

        mockMvc.perform(post("/api/users")
                .principal(adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void listReturnsUsers() throws Exception {
        when(userService.list(null, null)).thenReturn(List.of(sampleResponse(1L, "user@example.com")));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getReturnsUser() throws Exception {
        when(userService.get(1L)).thenReturn(sampleResponse(1L, "user@example.com"));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void updateReturnsUpdatedUser() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(adminUser));
        when(userService.update(any(Long.class), any(UserUpdateRequest.class), any(User.class)))
                .thenReturn(sampleResponse(1L, "user@example.com"));

        UserUpdateRequest request = new UserUpdateRequest(
                "user@example.com", null, "Updated Name", null, null, null
        );

        mockMvc.perform(put("/api/users/1")
                .principal(adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(adminUser));

        mockMvc.perform(delete("/api/users/1").principal(adminAuth))
                .andExpect(status().isNoContent());
    }
}
