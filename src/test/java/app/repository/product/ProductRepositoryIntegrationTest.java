package app.repository.product;

import app.model.entity.product.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findAllByIsActiveTrueShouldReturnOnlyActiveProducts() {
        LocalDateTime now = LocalDateTime.now();

        Product activeProduct = Product.builder()
                .name("Active honey")
                .description("Active product")
                .price(new BigDecimal("15.50"))
                .imageUrl("/images/active.png")
                .items(10)
                .isActive(true)
                .createdOn(now)
                .updatedOn(now)
                .build();

        Product inactiveProduct = Product.builder()
                .name("Inactive honey")
                .description("Inactive product")
                .price(new BigDecimal("12.50"))
                .imageUrl("/images/inactive.png")
                .items(5)
                .isActive(false)
                .createdOn(now)
                .updatedOn(now)
                .build();

        productRepository.saveAllAndFlush(
                List.of(activeProduct, inactiveProduct)
        );

        List<Product> result =
                productRepository.findAllByIsActiveTrue();

        assertEquals(1, result.size());
        assertEquals("Active honey", result.get(0).getName());
        assertTrue(result.get(0).getIsActive());
    }
}
