package com.silvaldeweb.controller.address;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.silvaldeweb.dto.address.AddressCreateRequest;
import com.silvaldeweb.dto.address.AddressResponse;
import com.silvaldeweb.dto.address.AddressUpdateRequest;
import com.silvaldeweb.exception.GlobalExceptionHandler;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.address.AddressService;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AddressService addressService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressController addressController;

    private Authentication userAuth;
    private User normalUser;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(addressController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        normalUser = User.builder().id(2L).email("customer@example.com").role(Role.USER).active(true).build();
        userAuth = new UsernamePasswordAuthenticationToken(
                "customer@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private AddressResponse sampleResponse(Long id) {
        return new AddressResponse(
                id, "John Doe", "Calle Mayor 1", "Madrid", "Madrid", "28001", "España",
                true, 2L,
                Instant.parse("2026-06-08T10:00:00Z"),
                Instant.parse("2026-06-08T10:00:00Z")
        );
    }

    @Test
    void createReturnsCreatedAddress() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(normalUser));
        when(addressService.create(any(User.class), any(AddressCreateRequest.class)))
                .thenReturn(sampleResponse(10L));

        AddressCreateRequest request = new AddressCreateRequest(
                "John Doe", "Calle Mayor 1", "Madrid", "Madrid", "28001", "España", true
        );

        mockMvc.perform(post("/api/addresses")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.postalCode").value("28001"))
                .andExpect(jsonPath("$.userId").value(2L));
    }

    @Test
    void createRejectsBadPostalCode() throws Exception {
        AddressCreateRequest request = new AddressCreateRequest(
                "John Doe", "Calle 1", "Madrid", "Madrid", "BAD", "España", true
        );

        mockMvc.perform(post("/api/addresses")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("postalCode"));
    }

    @Test
    void listReturnsAddresses() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(normalUser));
        when(addressService.list(eq(normalUser), eq(false), eq(null)))
                .thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/addresses").principal(userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getReturnsAddress() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(normalUser));
        when(addressService.get(eq(normalUser), eq(false), eq(2L))).thenReturn(sampleResponse(2L));

        mockMvc.perform(get("/api/addresses/2").principal(userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L));
    }

    @Test
    void updateReturnsUpdatedAddress() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(normalUser));
        when(addressService.update(eq(normalUser), eq(false), eq(3L), any(AddressUpdateRequest.class)))
                .thenReturn(sampleResponse(3L));

        AddressUpdateRequest request = new AddressUpdateRequest(
                "Updated", "Calle 3", "Madrid", "Madrid", "28003", "España", false
        );

        mockMvc.perform(put("/api/addresses/3")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(normalUser));

        mockMvc.perform(delete("/api/addresses/4").principal(userAuth))
                .andExpect(status().isNoContent());
    }
}
