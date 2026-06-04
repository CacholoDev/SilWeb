package com.silvaldeweb.controller.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
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
import com.silvaldeweb.dto.category.CategoryCreateRequest;
import com.silvaldeweb.dto.category.CategoryResponse;
import com.silvaldeweb.dto.category.CategoryUpdateRequest;
import com.silvaldeweb.service.category.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Mock
        private CategoryService categoryService;

        @InjectMocks
        private CategoryController categoryController;

        @BeforeEach
        void setup() {
                mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
        }

    @Test
    void createReturnsCreatedCategory() throws Exception {
        CategoryResponse response = new CategoryResponse(1L, "Home", "Desc", true,
                OffsetDateTime.parse("2026-06-03T10:00:00Z"),
                OffsetDateTime.parse("2026-06-03T10:00:00Z"));

        when(categoryService.create(any(CategoryCreateRequest.class))).thenReturn(response);

        CategoryCreateRequest request = new CategoryCreateRequest("Home", "Desc", true);

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Home"));
    }

    @Test
    void listReturnsCategories() throws Exception {
        CategoryResponse response = new CategoryResponse(1L, "Home", "Desc", true,
                OffsetDateTime.parse("2026-06-03T10:00:00Z"),
                OffsetDateTime.parse("2026-06-03T10:00:00Z"));

        when(categoryService.list(null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getReturnsCategory() throws Exception {
        CategoryResponse response = new CategoryResponse(2L, "Kitchen", "Desc", true,
                OffsetDateTime.parse("2026-06-03T10:00:00Z"),
                OffsetDateTime.parse("2026-06-03T10:00:00Z"));

        when(categoryService.get(2L)).thenReturn(response);

        mockMvc.perform(get("/api/categories/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Kitchen"));
    }

    @Test
    void updateReturnsUpdatedCategory() throws Exception {
        CategoryResponse response = new CategoryResponse(3L, "Updated", "Desc", false,
                OffsetDateTime.parse("2026-06-03T10:00:00Z"),
                OffsetDateTime.parse("2026-06-03T10:00:00Z"));

        when(categoryService.update(any(Long.class), any(CategoryUpdateRequest.class))).thenReturn(response);

        CategoryUpdateRequest request = new CategoryUpdateRequest("Updated", "Desc", false);

        mockMvc.perform(put("/api/categories/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(categoryService).delete(4L);

        mockMvc.perform(delete("/api/categories/4"))
                .andExpect(status().isNoContent());
    }
}
