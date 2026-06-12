package com.silvaldeweb.service.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.silvaldeweb.dto.category.CategoryCreateRequest;
import com.silvaldeweb.dto.category.CategoryResponse;
import com.silvaldeweb.dto.category.CategoryUpdateRequest;
import com.silvaldeweb.exception.category.CategoryAlreadyExistsException;
import com.silvaldeweb.model.category.Category;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.category.CategoryRepository;
import com.silvaldeweb.service.audit.AuditLogService;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CategoryService categoryService;

    private User adminActor() {
        return User.builder().id(1L).email("admin@example.com").role(Role.ADMIN).build();
    }

    @Test
    void createUsesDefaultActiveWhenNull() {
        CategoryCreateRequest request = new CategoryCreateRequest("Home", "Desc", null);

        when(categoryRepository.existsByNameIgnoreCase("Home")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CategoryResponse response = categoryService.create(request, adminActor());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());

        assertEquals(true, captor.getValue().getActive());
        assertEquals(1L, response.id());
        assertEquals("Home", response.name());
        assertEquals(true, response.active());
    }

    @Test
    void createThrowsWhenNameExists() {
        CategoryCreateRequest request = new CategoryCreateRequest("Home", "Desc", true);
        when(categoryRepository.existsByNameIgnoreCase("Home")).thenReturn(true);

        assertThrows(CategoryAlreadyExistsException.class, () -> categoryService.create(request, adminActor()));
    }

    @Test
    void updateSavesChanges() {
        Category existing = Category.builder()
                .id(5L)
                .name("Old")
                .description("Old desc")
                .active(true)
                .build();

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCase("New")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryUpdateRequest request = new CategoryUpdateRequest("New", "New desc", false);
        CategoryResponse response = categoryService.update(5L, request, adminActor());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());

        assertEquals("New", captor.getValue().getName());
        assertEquals(false, captor.getValue().getActive());
        assertEquals("New", response.name());
        assertEquals(false, response.active());
    }
}
