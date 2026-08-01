package app.web.controller;

import app.config.WebMvcConfiguration;
import app.exception.InvalidCartDataException;
import app.model.entity.product.Product;
import app.model.entity.user.User;
import app.service.CartService;
import app.service.ProductService;
import app.service.UserService;
import app.web.dto.cart.AddToCartRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({
        PageController.class,
        CartController.class
})
@Import(WebMvcConfiguration.class)
@ActiveProfiles("test")
class CatalogControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CartService cartService;

    @Test
    void productsPageShouldLoadOnlyActiveProducts()
            throws Exception {

        List<Product> activeProducts = List.of();

        when(productService.getAllActiveProducts())
                .thenReturn(activeProducts);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attribute(
                        "products",
                        activeProducts
                ))
                .andExpect(model().attributeExists(
                        "addToCartRequest"
                ));

        verify(productService).getAllActiveProducts();
        verify(productService, never()).getAllProducts();
    }

    @Test
    @WithMockUser
    void addToCartShouldDisplayErrorWhenProductWasDeactivated()
            throws Exception {

        UUID productId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        List<Product> activeProducts = List.of();

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        when(productService.getAllActiveProducts())
                .thenReturn(activeProducts);

        when(cartService.addToCart(
                eq(currentUser),
                any(AddToCartRequest.class)
        )).thenThrow(new InvalidCartDataException(
                "Product is not active"
        ));

        mockMvc.perform(post("/cart")
                        .with(csrf())
                        .param(
                                "productId",
                                productId.toString()
                        )
                        .param("quantity", "1")
                        .param("source", "products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attribute(
                        "cartError",
                        "Product is not active"
                ))
                .andExpect(model().attribute(
                        "products",
                        activeProducts
                ));

        verify(cartService).addToCart(
                eq(currentUser),
                argThat(request ->
                        productId.equals(request.getProductId())
                                && request.getQuantity() == 1
                )
        );

        verify(productService).getAllActiveProducts();
        verify(productService, never()).getAllProducts();
    }
}
