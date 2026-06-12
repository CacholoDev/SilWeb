package com.silvaldeweb.controller.category;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.silvaldeweb.config.AuthUtils;
import com.silvaldeweb.dto.category.CategoryCreateRequest;
import com.silvaldeweb.dto.category.CategoryResponse;
import com.silvaldeweb.dto.category.CategoryUpdateRequest;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.category.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request,
                                                   Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        log.info("POST /api/categories name='{}' actor={}", request.name(), actor.getEmail());
        CategoryResponse response = categoryService.create(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<CategoryResponse> list(@RequestParam(required = false) Boolean active) {
        log.info("GET /api/categories active={}", active);
        return categoryService.list(active);
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable Long id) {
        log.info("GET /api/categories/{}", id);
        return categoryService.get(id);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request,
                                    Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        log.info("PUT /api/categories/{} actor={}", id, actor.getEmail());
        return categoryService.update(id, request, actor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        log.info("DELETE /api/categories/{} actor={}", id, actor.getEmail());
        categoryService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
