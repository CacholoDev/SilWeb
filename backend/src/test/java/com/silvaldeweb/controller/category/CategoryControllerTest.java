package com.silvaldeweb.controller.category;

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
import com.silvaldeweb.dto.category.CategoryCreateRequest;
import com.silvaldeweb.dto.category.CategoryResponse;
import com.silvaldeweb.dto.category.CategoryUpdateRequest;
import com.silvaldeweb.exception.GlobalExceptionHandler;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.category.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryController categoryController;

    private Authentication adminAuth;
    private User adminUser;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminUser = User.builder().id(1L).email("admin@example.com").role(Role.ADMIN).active(true).build();
        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void createReturnsCreatedCategory() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(adminUser));
        when(categoryService.create(any(CategoryCreateRequest.class), any(User.class)))
                .thenReturn(new CategoryResponse(1L, "Home", "Desc", true,
                        Instant.parse("2026-06-03T10:00:00Z"),
                        Instant.parse("2026-06-03T10:00:00Z")));

        CategoryCreateRequest request = new CategoryCreateRequest("Home", "Desc", true);

        mockMvc.perform(post("/api/categories")
                .principal(adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Home"));
    }

    @Test
    void listReturnsCategories() throws Exception {
        when(categoryService.list(null)).thenReturn(List.of(
                new CategoryResponse(1L, "Home", "Desc", true,
                        Instant.parse("2026-06-03T10:00:00Z"),
                        Instant.parse("2026-06-03T10:00:00Z"))));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getReturnsCategory() throws Exception {
        when(categoryService.get(2L)).thenReturn(new CategoryResponse(2L, "Kitchen", "Desc", true,
                Instant.parse("2026-06-03T10:00:00Z"),
                Instant.parse("2026-06-03T10:00:00Z")));

        mockMvc.perform(get("/api/categories/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Kitchen"));
    }

    @Test
    void updateReturnsUpdatedCategory() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(adminUser));
        when(categoryService.update(any(Long.class), any(CategoryUpdateRequest.class), any(User.class)))
                .thenReturn(new CategoryResponse(3L, "Updated", "Desc", false,
                        Instant.parse("2026-06-03T10:00:00Z"),
                        Instant.parse("2026-06-03T10:00:00Z")));

        CategoryUpdateRequest request = new CategoryUpdateRequest("Updated", "Desc", false);

        mockMvc.perform(put("/api/categories/3")
                .principal(adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(adminUser));

        mockMvc.perform(delete("/api/categories/4").principal(adminAuth))
                .andExpect(status().isNoContent());
    }
}
