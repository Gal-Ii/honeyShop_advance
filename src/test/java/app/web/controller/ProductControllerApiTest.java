package app.web.controller;

import app.config.WebMvcConfiguration;
import app.model.entity.user.User;
import app.model.entity.product.Product;
import app.service.ProductService;
import app.service.ReviewService;
import app.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(WebMvcConfiguration.class)
@ActiveProfiles("test")
class ProductControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    @WithMockUser(authorities = "PRODUCT_CREATE")
    void createProductShouldRedirectWhenRequestIsValid()
            throws Exception {

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .param("name", "Acacia honey")
                        .param("description", "Natural honey")
                        .param("price", "15.50")
                        .param("imageUrl", "/images/akatsia.png")
                        .param("items", "10")
                        .param("isActive", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin-products"));

        verify(userService).getCurrentUser();

        verify(productService).create(
                argThat(request ->
                        request.getName().equals("Acacia honey")
                                && request.getPrice().toString()
                                .equals("15.50")
                                && request.getItems() == 10
                                && request.getIsActive()
                ),
                argThat(user ->
                        user.getId().equals(currentUser.getId())
                )
        );
    }

    @Test
    @WithMockUser(authorities = "PRODUCT_CREATE")
    void createProductShouldReturnFormWhenRequestIsInvalid()
            throws Exception {

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .param("name", "")
                        .param("description", "Natural honey")
                        .param("price", "-5.00")
                        .param("items", "-1")
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-create"))
                .andExpect(model().attributeHasFieldErrors(
                        "productCreateRequest",
                        "name",
                        "price",
                        "items"
                ));

        verify(productService, never())
                .create(any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProductShouldReturnForbiddenWithoutRequiredAuthority()
            throws Exception {

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .param("name", "Acacia honey")
                        .param("description", "Natural honey")
                        .param("price", "15.50")
                        .param("items", "10")
                        .param("isActive", "true"))
                .andExpect(status().isForbidden());

        verify(productService, never())
                .create(any(), any());
    }

    @Test
    @WithMockUser(authorities = "PRODUCT_CREATE")
    void productCreatePageShouldReturnCreateForm()
            throws Exception {

        mockMvc.perform(get("/product-create"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-create"))
                .andExpect(model().attributeExists(
                        "productCreateRequest"
                ));
    }

    @Test
    @WithMockUser(authorities = "PRODUCT_UPDATE")
    void productUpdatePageShouldReturnPopulatedUpdateForm()
            throws Exception {

        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .id(productId)
                .name("Acacia honey")
                .description("Natural honey")
                .price(new BigDecimal("15.50"))
                .imageUrl("/images/akatsia.png")
                .items(10)
                .isActive(true)
                .build();

        when(productService.getById(productId))
                .thenReturn(product);

        mockMvc.perform(get("/products/{id}/update", productId))
                .andExpect(status().isOk())
                .andExpect(view().name("product-update"))
                .andExpect(model().attribute(
                        "product",
                        product
                ))
                .andExpect(model().attributeExists(
                        "productUpdateRequest"
                ));

        verify(productService).getById(productId);
    }

    @Test
    @WithMockUser(authorities = "PRODUCT_UPDATE")
    void updateProductShouldRedirectWhenRequestIsValid()
            throws Exception {

        UUID productId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        mockMvc.perform(post(
                        "/products/{id}/update",
                        productId
                )
                        .with(csrf())
                        .param("name", "Updated honey")
                        .param("description", "Updated description")
                        .param("price", "18.50")
                        .param("imageUrl", "/images/updated.png")
                        .param("items", "20")
                        .param("isActive", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin-products"));

        verify(productService).update(
                eq(productId),
                argThat(request ->
                        request.getName().equals("Updated honey")
                                && request.getPrice().compareTo(
                                new BigDecimal("18.50")
                        ) == 0
                                && request.getItems() == 20
                                && request.getIsActive()
                ),
                eq(currentUser)
        );
    }

    @Test
    @WithMockUser(authorities = "PRODUCT_DELETE")
    void deleteProductShouldDeactivateProductAndRedirect()
            throws Exception {

        UUID productId = UUID.randomUUID();

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        mockMvc.perform(post(
                        "/products/{id}/delete",
                        productId
                ).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin-products"));

        verify(userService).getCurrentUser();
        verify(productService).delete(
                productId,
                currentUser
        );
    }

    @Test
    @WithMockUser
    void productDetailsPageShouldReturnProductAndReviews()
            throws Exception {

        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Product product = Product.builder()
                .id(productId)
                .name("Acacia honey")
                .price(new BigDecimal("15.50"))
                .items(10)
                .isActive(true)
                .build();

        User currentUser = User.builder()
                .id(userId)
                .name("Ivan Ivanov")
                .email("ivan@example.com")
                .build();

        when(productService.getById(productId))
                .thenReturn(product);

        when(userService.getCurrentUser())
                .thenReturn(currentUser);

        when(reviewService.getReviewsByProductId(productId))
                .thenReturn(List.of());

        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(view().name("product-details"))
                .andExpect(model().attribute(
                        "product",
                        product
                ))
                .andExpect(model().attribute(
                        "currentUserId",
                        userId
                ))
                .andExpect(model().attribute(
                        "reviews",
                        List.of()
                ))
                .andExpect(model().attributeExists(
                        "createReviewRequest",
                        "updateReviewRequest"
                ));

        verify(productService).getById(productId);
        verify(userService).getCurrentUser();
        verify(reviewService)
                .getReviewsByProductId(productId);
    }

    @Test
    @WithMockUser
    void createReviewShouldDelegateRequestAndRedirect()
            throws Exception {

        UUID productId = UUID.randomUUID();

        mockMvc.perform(post(
                        "/products/{id}/reviews",
                        productId
                )
                        .with(csrf())
                        .param("rating", "5")
                        .param(
                                "comment",
                                "Excellent natural honey product."
                        ))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/products/" + productId
                ));

        verify(reviewService).createReview(
                eq(productId),
                argThat(request ->
                        request.getUserId() == null
                                && request.getProductId() == null
                                && request.getAuthorName() == null
                                && request.getRating() == 5
                                && "Excellent natural honey product."
                                .equals(request.getComment())
                )
        );
    }

    @Test
    @WithMockUser
    void updateReviewShouldDelegateRequestAndRedirect()
            throws Exception {

        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        mockMvc.perform(post(
                        "/products/{productId}/reviews/{reviewId}/update",
                        productId,
                        reviewId
                )
                        .with(csrf())
                        .param("rating", "4")
                        .param(
                                "comment",
                                "Updated review comment."
                        ))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/products/" + productId
                ));

        verify(reviewService).updateReview(
                eq(reviewId),
                argThat(request ->
                        request.getUserId() == null
                                && request.getRating() == 4
                                && "Updated review comment."
                                .equals(request.getComment())
                )
        );
    }

    @Test
    @WithMockUser
    void deleteReviewShouldDelegateAndRedirect()
            throws Exception {

        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        mockMvc.perform(post(
                        "/products/{productId}/reviews/{reviewId}/delete",
                        productId,
                        reviewId
                ).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/products/" + productId
                ));

        verify(reviewService).deleteReview(reviewId);
    }
}
