package com.silvaldeweb.service.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.silvaldeweb.dto.product.ProductCreateRequest;
import com.silvaldeweb.dto.product.ProductResponse;
import com.silvaldeweb.dto.product.ProductUpdateRequest;
import com.silvaldeweb.exception.category.CategoryNotFoundException;
import com.silvaldeweb.exception.product.ProductAlreadyExistsException;
import com.silvaldeweb.model.category.Category;
import com.silvaldeweb.model.product.Product;
import com.silvaldeweb.repository.category.CategoryRepository;
import com.silvaldeweb.repository.product.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createThrowsWhenSkuExists() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Lamp",
                "SKU-01",
                "Desc",
                BigDecimal.valueOf(9.99),
                10,
                1L,
                true
        );

        when(productRepository.existsBySkuIgnoreCase("SKU-01")).thenReturn(true);

        assertThrows(ProductAlreadyExistsException.class, () -> productService.create(request));
    }

    @Test
    void createThrowsWhenCategoryMissing() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Lamp",
                "SKU-01",
                "Desc",
                BigDecimal.valueOf(9.99),
                10,
                1L,
                true
        );

        when(productRepository.existsBySkuIgnoreCase("SKU-01")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> productService.create(request));
    }

    @Test
    void updateSavesChanges() {
        Category category = Category.builder().id(1L).name("Home").build();
        Product existing = Product.builder()
                .id(10L)
                .name("Old")
                .sku("SKU-01")
                .price(BigDecimal.valueOf(5.00))
                .stock(5)
                .category(category)
                .build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuIgnoreCase("SKU-02")).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(Category.builder().id(2L).name("NewCat").build()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductUpdateRequest request = new ProductUpdateRequest(
                "New",
                "SKU-02",
                "Desc",
                BigDecimal.valueOf(12.50),
                7,
                2L,
                false
        );

        ProductResponse response = productService.update(10L, request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        assertEquals("SKU-02", captor.getValue().getSku());
        assertEquals(false, captor.getValue().getActive());
        assertEquals("SKU-02", response.sku());
    }
}
