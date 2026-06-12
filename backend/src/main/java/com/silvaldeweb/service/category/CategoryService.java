package com.silvaldeweb.service.category;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.silvaldeweb.dto.category.CategoryCreateRequest;
import com.silvaldeweb.dto.category.CategoryResponse;
import com.silvaldeweb.dto.category.CategoryUpdateRequest;
import com.silvaldeweb.exception.category.CategoryAlreadyExistsException;
import com.silvaldeweb.exception.category.CategoryNotFoundException;
import com.silvaldeweb.model.audit.Action;
import com.silvaldeweb.model.category.Category;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.category.CategoryRepository;
import com.silvaldeweb.service.audit.AuditLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request, User actor) {
        log.info("Creating category name='{}' actor={}", request.name(),
                actor != null ? actor.getEmail() : "null");
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            log.warn("Category name='{}' already exists, refusing to create", name);
            throw new CategoryAlreadyExistsException(name);
        }

        Category.CategoryBuilder builder = Category.builder()
            .name(name)
            .description(request.description());

        if (request.active() != null) {
            builder.active(request.active());
        }

        Category category = builder.build();

        Category saved = categoryRepository.save(category);
        auditLogService.record(actor, Action.CREATE, "Category", saved.getId(),
                "name=" + saved.getName(), null);
        log.info("Category created id={} name='{}'", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Boolean active) {
        log.info("Listing categories active={}", active);
        List<Category> categories = active == null
                ? categoryRepository.findAll()
                : categoryRepository.findByActive(active);

        log.info("Found {} categories", categories.size());
        return categories.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        log.info("Getting category id={}", id);
        return toResponse(findById(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateRequest request, User actor) {
        log.info("Updating category id={} newName='{}' actor={}", id, request.name(),
                actor != null ? actor.getEmail() : "null");
        Category category = findById(id);
        String name = request.name().trim();

        if (!category.getName().equalsIgnoreCase(name)
                && categoryRepository.existsByNameIgnoreCase(name)) {
            log.warn("Category name='{}' already exists, refusing to update", name);
            throw new CategoryAlreadyExistsException(name);
        }

        String previousName = category.getName();
        Boolean previousActive = category.getActive();

        category.setName(name);
        category.setDescription(request.description());
        if (request.active() != null) {
            category.setActive(request.active());
        }

        Category saved = categoryRepository.save(category);
        StringBuilder diff = new StringBuilder();
        if (!previousName.equals(saved.getName())) {
            diff.append("name: ").append(previousName).append(" -> ").append(saved.getName()).append("; ");
        }
        if (previousActive != null && !previousActive.equals(saved.getActive())) {
            diff.append("active: ").append(previousActive).append(" -> ").append(saved.getActive()).append("; ");
        }
        if (diff.length() > 0) {
            auditLogService.record(actor, Action.UPDATE, "Category", saved.getId(), diff.toString().trim(), null);
        }
        log.info("Category updated id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, User actor) {
        log.info("Deleting category id={} actor={}", id, actor != null ? actor.getEmail() : "null");
        try {
            Category category = findById(id);
            String metadata = "name=" + category.getName();
            log.info("Category id={} found, executing delete", id);
            categoryRepository.delete(category);
            auditLogService.record(actor, Action.DELETE, "Category", id, metadata, null);
            log.info("Category id={} deleted", id);
        } catch (CategoryNotFoundException exception) {
            log.warn("Delete failed: category id={} not found", id);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Delete failed for category id={}", id, exception);
            throw exception;
        }
    }

    private Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
