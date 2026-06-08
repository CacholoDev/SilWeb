package com.silvaldeweb.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silvaldeweb.dto.user.UserCreateRequest;
import com.silvaldeweb.dto.user.UserResponse;
import com.silvaldeweb.dto.user.UserUpdateRequest;
import com.silvaldeweb.exception.GlobalExceptionHandler;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.service.user.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Mock
        private UserService userService;

        @InjectMocks
        private UserController userController;

        @BeforeEach
        void setup() {
                mockMvc = MockMvcBuilders.standaloneSetup(userController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

    private UserResponse sampleResponse(Long id, String email) {
        return new UserResponse(
                id,
                email,
                "John Doe",
                "123456789",
                true,
                Role.USER,
                null,
                false,
                Instant.parse("2026-06-07T10:00:00Z"),
                Instant.parse("2026-06-07T10:00:00Z")
        );
    }

    @Test
    void createReturnsCreatedUser() throws Exception {
        UserResponse response = sampleResponse(1L, "user@example.com");
        when(userService.create(any(UserCreateRequest.class))).thenReturn(response);

        UserCreateRequest request = new UserCreateRequest(
                "user@example.com",
                "plainPassword",
                "John Doe",
                "123456789",
                true,
                Role.USER
        );

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void createRejectsEmailWithoutTld() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "admin@example",
                "plainPassword",
                "Admin",
                "123456789",
                true,
                Role.ADMIN
        );

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void createRejectsPhoneNotNineDigits() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "user@example.com",
                "plainPassword",
                "User",
                "12345",
                true,
                Role.USER
        );

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("phone"));
    }

    @Test
    void listReturnsUsers() throws Exception {
        UserResponse response = sampleResponse(1L, "user@example.com");
        when(userService.list(null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].email").value("user@example.com"));
    }

    @Test
    void getReturnsUser() throws Exception {
        UserResponse response = sampleResponse(2L, "two@example.com");
        when(userService.get(2L)).thenReturn(response);

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.email").value("two@example.com"));
    }

    @Test
    void updateReturnsUpdatedUser() throws Exception {
        UserResponse response = sampleResponse(3L, "updated@example.com");
        when(userService.update(any(Long.class), any(UserUpdateRequest.class))).thenReturn(response);

        UserUpdateRequest request = new UserUpdateRequest(
                "updated@example.com",
                null,
                "Updated Name",
                null,
                null,
                Role.ADMIN
        );

        mockMvc.perform(put("/api/users/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(userService).delete(4L);

        mockMvc.perform(delete("/api/users/4"))
                .andExpect(status().isNoContent());
    }
}
