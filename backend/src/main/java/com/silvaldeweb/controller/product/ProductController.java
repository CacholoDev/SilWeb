package com.silvaldeweb.controller.product;

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

import com.silvaldeweb.dto.product.ProductCreateRequest;
import com.silvaldeweb.dto.product.ProductResponse;
import com.silvaldeweb.dto.product.ProductUpdateRequest;
import com.silvaldeweb.service.product.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        log.info("POST /api/products sku='{}'", request.sku());
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) Boolean active,
                                      @RequestParam(required = false) Long categoryId) {
        log.info("GET /api/products active={} categoryId={}", active, categoryId);
        return productService.list(active, categoryId);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        log.info("GET /api/products/{}", id);
        return productService.get(id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        log.info("PUT /api/products/{}", id);
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/products/{}", id);
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
