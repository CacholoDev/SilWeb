package com.silvaldeweb.controller.category;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.silvaldeweb.dto.category.CategoryCreateRequest;
import com.silvaldeweb.dto.category.CategoryResponse;
import com.silvaldeweb.dto.category.CategoryUpdateRequest;
import com.silvaldeweb.service.category.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        log.info("POST /api/categories name='{}'", request.name());
        CategoryResponse response = categoryService.create(request);
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
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        log.info("PUT /api/categories/{}", id);
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/categories/{}", id);
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
