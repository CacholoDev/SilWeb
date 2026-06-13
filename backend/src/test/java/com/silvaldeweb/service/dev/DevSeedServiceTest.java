package com.silvaldeweb.service.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.silvaldeweb.model.category.Category;
import com.silvaldeweb.model.product.Product;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.address.AddressRepository;
import com.silvaldeweb.repository.category.CategoryRepository;
import com.silvaldeweb.repository.product.ProductRepository;
import com.silvaldeweb.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class DevSeedServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private DevSeedService devSeedService;

    @Test
    void seedCreatesCategoriesAndProductsWhenDbIsEmpty() {
        User customer = User.builder().id(2L).email("customer@example.com").role(Role.USER).build();
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(categoryRepository.count()).thenReturn(0L);
        when(productRepository.count()).thenReturn(0L);
        when(categoryRepository.findByNameIgnoreCase(any())).thenAnswer(i -> {
            String name = i.getArgument(0);
            return Optional.of(Category.builder().id(1L).name(name).build());
        });
        when(categoryRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(addressRepository.findByUserId(2L)).thenReturn(List.of());

        Map<String, Object> result = devSeedService.seed();

        verify(categoryRepository, times(1)).saveAll(any());
        verify(productRepository, times(1)).saveAll(any());
        verify(addressRepository, times(1)).save(any());
        assertTrue(((List<?>) result.get("created")).contains("4 categories"));
        assertTrue(((List<?>) result.get("created")).contains("8 products"));
        assertTrue(((List<?>) result.get("created")).contains("1 address for customer"));
        assertEquals(0L, result.get("categoriesInDb"));
        assertEquals(0L, result.get("productsInDb"));
    }

    @Test
    void seedSkipsWhenDataAlreadyExists() {
        User customer = User.builder().id(2L).email("customer@example.com").role(Role.USER).build();
        com.silvaldeweb.model.address.Address existing = com.silvaldeweb.model.address.Address.builder()
                .id(99L).user(customer).build();
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(categoryRepository.count()).thenReturn(4L);
        when(productRepository.count()).thenReturn(8L);
        when(addressRepository.findByUserId(2L)).thenReturn(List.of(existing));

        Map<String, Object> result = devSeedService.seed();

        verify(categoryRepository, never()).saveAll(any());
        verify(productRepository, never()).saveAll(any());
        verify(addressRepository, never()).save(any());
        assertTrue(((List<?>) result.get("created")).isEmpty());
    }
}
