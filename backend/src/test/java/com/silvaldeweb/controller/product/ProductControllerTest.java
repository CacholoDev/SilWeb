package com.silvaldeweb.controller.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.silvaldeweb.dto.product.ProductCreateRequest;
import com.silvaldeweb.dto.product.ProductResponse;
import com.silvaldeweb.dto.product.ProductUpdateRequest;
import com.silvaldeweb.exception.GlobalExceptionHandler;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.product.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductController productController;

    private Authentication adminAuth;
    private User adminUser;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminUser = User.builder().id(1L).email("admin@example.com").role(Role.ADMIN).active(true).build();
        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private ProductResponse sampleResponse(Long id, String name, String sku) {
        return new ProductResponse(id, name, sku, "Desc",
                BigDecimal.valueOf(9.99), 10, true, 1L, "Home",
                Instant.parse("2026-06-03T10:00:00Z"),
                Instant.parse("2026-06-03T10:00:00Z"));
    }

    @Test
    void createReturnsCreatedProduct() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(adminUser));
        when(productService.create(any(ProductCreateRequest.class), any(User.class)))
                .thenReturn(sampleResponse(1L, "Lamp", "SKU-01"));

        ProductCreateRequest request = new ProductCreateRequest(
                "Lamp", "SKU-01", "Desc", BigDecimal.valueOf(9.99), 10, 1L, true
        );

        mockMvc.perform(post("/api/products")
                .principal(adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.sku").value("SKU-01"));
    }

    @Test
    void listReturnsProducts() throws Exception {
        when(productService.list(null, null)).thenReturn(List.of(sampleResponse(1L, "Lamp", "SKU-01")));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getReturnsProduct() throws Exception {
        when(productService.get(2L)).thenReturn(sampleResponse(2L, "Chair", "SKU-02"));

        mockMvc.perform(get("/api/products/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.sku").value("SKU-02"));
    }

    @Test
    void updateReturnsUpdatedProduct() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(adminUser));
        when(productService.update(any(Long.class), any(ProductUpdateRequest.class), any(User.class)))
                .thenReturn(new ProductResponse(3L, "Updated", "SKU-03", "Desc",
                        BigDecimal.valueOf(29.99), 5, false, 2L, "Kitchen",
                        Instant.parse("2026-06-03T10:00:00Z"),
                        Instant.parse("2026-06-03T10:00:00Z")));

        ProductUpdateRequest request = new ProductUpdateRequest(
                "Updated", "SKU-03", "Desc", BigDecimal.valueOf(29.99), 5, 2L, false
        );

        mockMvc.perform(put("/api/products/3")
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

        mockMvc.perform(delete("/api/products/4").principal(adminAuth))
                .andExpect(status().isNoContent());
    }
}
