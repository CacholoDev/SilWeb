package com.silvaldeweb.service.dev;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.silvaldeweb.model.address.Address;
import com.silvaldeweb.model.category.Category;
import com.silvaldeweb.model.product.Product;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.address.AddressRepository;
import com.silvaldeweb.repository.category.CategoryRepository;
import com.silvaldeweb.repository.product.ProductRepository;
import com.silvaldeweb.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DevSeedService {

    private static final Logger log = LoggerFactory.getLogger(DevSeedService.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public Map<String, Object> seed() {
        log.info("Dev seed: starting");
        List<String> created = new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);

        User customer = userRepository.findByEmailIgnoreCase("customer@example.com").orElse(null);
        if (customer == null) {
            log.warn("Dev seed: 'customer@example.com' not found. Make sure DataSeeder has run first.");
        }

        if (categoryRepository.count() == 0) {
            seedCategories();
            created.add("4 categories");
            log.info("Dev seed: created 4 categories");
        } else {
            log.info("Dev seed: categories already exist, skipping");
        }

        if (productRepository.count() == 0) {
            seedProducts();
            created.add("8 products");
            log.info("Dev seed: created 8 products");
        } else {
            log.info("Dev seed: products already exist, skipping");
        }

        if (customer != null && addressRepository.findByUserId(customer.getId()).isEmpty()) {
            seedAddressFor(customer);
            created.add("1 address for customer");
            log.info("Dev seed: created address for customer");
        } else {
            log.info("Dev seed: customer has address already, skipping");
        }

        result.put("productsInDb", productRepository.count());
        result.put("categoriesInDb", categoryRepository.count());
        result.put("addressesInDb", addressRepository.findAll().size());
        result.put("adminEmail", "admin@example.com");
        result.put("customerEmail", "customer@example.com");
        result.put("adminPasswordInEnv", "APP_ADMIN_PASSWORD (see .env)");
        result.put("customerPasswordInEnv", "APP_CUSTOMER_PASSWORD (see .env)");

        log.info("Dev seed: done. created={}", created);
        return result;
    }

    private void seedCategories() {
        categoryRepository.saveAll(List.of(
                Category.builder().name("Hogar").description("Productos para el hogar").active(true).build(),
                Category.builder().name("Cocina").description("Cocina, menaje, pequeños electrodomésticos").active(true).build(),
                Category.builder().name("Iluminación").description("Lámparas, bombillas, luz decorativa").active(true).build(),
                Category.builder().name("Textil").description("Textil del hogar: cortinas, alfombras, ropa de cama").active(true).build()
        ));
    }

    private void seedProducts() {
        Category hogar = categoryRepository.findByNameIgnoreCase("Hogar").orElseThrow();
        Category cocina = categoryRepository.findByNameIgnoreCase("Cocina").orElseThrow();
        Category iluminacion = categoryRepository.findByNameIgnoreCase("Iluminación").orElseThrow();
        Category textil = categoryRepository.findByNameIgnoreCase("Textil").orElseThrow();

        productRepository.saveAll(List.of(
                Product.builder()
                        .name("Set de sartenes antiadherentes")
                        .sku("SAR-ANTI-001")
                        .description("Juego de 3 sartenes 20/24/28 cm, mango ergonómico, aptas para todo tipo de cocinas.")
                        .price(new BigDecimal("49.99"))
                        .stock(100)
                        .active(true)
                        .category(hogar)
                        .build(),
                Product.builder()
                        .name("Cafetera italiana 6 tazas")
                        .sku("CAF-ITA-006")
                        .description("Aluminio, apta para inducción con adaptador. Capacidad 300 ml.")
                        .price(new BigDecimal("24.50"))
                        .stock(50)
                        .active(true)
                        .category(cocina)
                        .build(),
                Product.builder()
                        .name("Lámpara de pie nórdica")
                        .sku("LMP-PIE-NOR")
                        .description("Lámpara de pie estilo nórdico, altura 150 cm, pantalla de lino.")
                        .price(new BigDecimal("89.00"))
                        .stock(20)
                        .active(true)
                        .category(iluminacion)
                        .build(),
                Product.builder()
                        .name("Bombilla LED E27 9W")
                        .sku("LED-E27-9W")
                        .description("Bombilla LED E27 9W equivalente a 60W, luz cálida 2700K, 800 lúmenes.")
                        .price(new BigDecimal("5.95"))
                        .stock(500)
                        .active(true)
                        .category(iluminacion)
                        .build(),
                Product.builder()
                        .name("Set de toallas 3 piezas")
                        .sku("TOL-3PZ-ALG")
                        .description("Set de toallas 100% algodón: 1 baño + 1 ducha + 1 lavabo. Colores neutros.")
                        .price(new BigDecimal("29.95"))
                        .stock(75)
                        .active(true)
                        .category(textil)
                        .build(),
                Product.builder()
                        .name("Cortina opaca 140x270")
                        .sku("CUR-OP-140")
                        .description("Cortina opaca (blackout) 140x270 cm, varios colores disponibles.")
                        .price(new BigDecimal("32.00"))
                        .stock(40)
                        .active(true)
                        .category(textil)
                        .build(),
                Product.builder()
                        .name("Tetera eléctrica de cristal")
                        .sku("TET-ELE-CRT")
                        .description("Tetera eléctrica 1.7L con cuerpo de cristal y base LED.")
                        .price(new BigDecimal("42.00"))
                        .stock(35)
                        .active(true)
                        .category(cocina)
                        .build(),
                Product.builder()
                        .name("Espejo redondo de pared")
                        .sku("ESP-RED-60")
                        .description("Espejo decorativo redondo 60 cm, marco de metal negro mate.")
                        .price(new BigDecimal("38.50"))
                        .stock(25)
                        .active(true)
                        .category(hogar)
                        .build()
        ));
    }

    private void seedAddressFor(User customer) {
        Address address = Address.builder()
                .fullName("Cliente Demo")
                .street("Calle Mayor 1, 2ºA")
                .city("Madrid")
                .province("Madrid")
                .postalCode("28001")
                .country("España")
                .isDefault(true)
                .user(customer)
                .build();
        addressRepository.save(address);
    }
}
