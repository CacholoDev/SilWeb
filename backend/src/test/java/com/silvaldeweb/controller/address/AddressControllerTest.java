package com.silvaldeweb.controller.address;

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
import com.silvaldeweb.dto.address.AddressCreateRequest;
import com.silvaldeweb.dto.address.AddressResponse;
import com.silvaldeweb.dto.address.AddressUpdateRequest;
import com.silvaldeweb.exception.GlobalExceptionHandler;
import com.silvaldeweb.service.address.AddressService;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Mock
        private AddressService addressService;

        @InjectMocks
        private AddressController addressController;

        @BeforeEach
        void setup() {
                mockMvc = MockMvcBuilders.standaloneSetup(addressController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

    private AddressResponse sampleResponse(Long id) {
        return new AddressResponse(
                id,
                "John Doe",
                "Calle Mayor 1",
                "Madrid",
                "Madrid",
                "28001",
                "España",
                true,
                1L,
                Instant.parse("2026-06-08T10:00:00Z"),
                Instant.parse("2026-06-08T10:00:00Z")
        );
    }

    @Test
    void createReturnsCreatedAddress() throws Exception {
        AddressResponse response = sampleResponse(10L);
        when(addressService.create(any(AddressCreateRequest.class))).thenReturn(response);

        AddressCreateRequest request = new AddressCreateRequest(
                "John Doe", "Calle Mayor 1", "Madrid", "Madrid", "28001", "España", true, 1L
        );

        mockMvc.perform(post("/api/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.postalCode").value("28001"))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void createRejectsBadPostalCode() throws Exception {
        AddressCreateRequest request = new AddressCreateRequest(
                "John Doe", "Calle 1", "Madrid", "Madrid", "BAD", "España", true, 1L
        );

        mockMvc.perform(post("/api/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("postalCode"));
    }

    @Test
    void listReturnsAddresses() throws Exception {
        when(addressService.list(null)).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getReturnsAddress() throws Exception {
        when(addressService.get(2L)).thenReturn(sampleResponse(2L));

        mockMvc.perform(get("/api/addresses/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L));
    }

    @Test
    void updateReturnsUpdatedAddress() throws Exception {
        when(addressService.update(any(Long.class), any(AddressUpdateRequest.class))).thenReturn(sampleResponse(3L));

        AddressUpdateRequest request = new AddressUpdateRequest(
                "Updated", "Calle 3", "Madrid", "Madrid", "28003", "España", false, 1L
        );

        mockMvc.perform(put("/api/addresses/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(addressService).delete(4L);

        mockMvc.perform(delete("/api/addresses/4"))
                .andExpect(status().isNoContent());
    }
}
