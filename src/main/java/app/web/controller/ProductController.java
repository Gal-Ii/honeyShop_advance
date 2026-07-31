package app.web.controller;

import app.model.entity.product.Product;
import app.model.entity.user.User;
import app.service.ProductService;
import app.service.ReviewService;
import app.service.UserService;
import app.web.dto.product.ProductCreateRequest;
import app.web.dto.product.ProductUpdateRequest;
import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.UpdateReviewRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import app.exception.ProductAlreadyExistsException;
import app.exception.ReviewAlreadyExistsException;
import app.exception.UnauthorizedReviewOperationException;

import java.util.UUID;

@Controller
public class ProductController {

    private final ProductService productService;
    private final UserService userService;
    private final ReviewService reviewService;

    @Autowired
    public ProductController(ProductService productService, UserService userService, ReviewService reviewService) {
        this.userService = userService;
        this.productService = productService;
        this.reviewService = reviewService;
    }

    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute("productCreateRequest") ProductCreateRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "product-create";
        }

        try {
            User currentUser = userService.getCurrentUser();
            productService.create(request, currentUser);
        } catch (ProductAlreadyExistsException e) {
            bindingResult.rejectValue(
                    "name",
                    "product.name.exists",
                    "Продукт с това име вече съществува."
            );

            return "product-create";
        }

        return "redirect:/admin-products";
    }

    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @PostMapping("/products/{id}/update")
    public String updateProduct(@PathVariable UUID id, @Valid @ModelAttribute("productUpdateRequest") ProductUpdateRequest updateRequest, BindingResult bindingResult, Model model){
         if (bindingResult.hasErrors()) {
            model.addAttribute("product", productService.getById(id));
            return "product-update";
        }

        try {
            User currentUser = userService.getCurrentUser();
            productService.update(id, updateRequest, currentUser);
        } catch (ProductAlreadyExistsException e) {
            bindingResult.rejectValue(
                    "name",
                    "product.name.exists",
                    "Продукт с това име вече съществува."
            );

            model.addAttribute("product", productService.getById(id));
            return "product-update";
        }

        return "redirect:/admin-products";
    }

    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable UUID id){
        User currentUser = userService.getCurrentUser();
        productService.delete(id, currentUser);
        return "redirect:/admin-products";
    }

    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    @GetMapping("/product-create")
    public String productCreatePage(Model model){
        ProductCreateRequest request = new ProductCreateRequest();
        request.setIsActive(true);

        model.addAttribute("productCreateRequest", request);

        return "product-create";
    }


    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @GetMapping("/products/{id}/update")
    public String productUpdatePage(@PathVariable UUID id, Model model){
        Product product = productService.getById(id);

        ProductUpdateRequest productUpdateRequest = ProductUpdateRequest.builder()
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .items(product.getItems())
                .isActive(product.getIsActive())
                .build();

        model.addAttribute("product", product);
        model.addAttribute("productUpdateRequest", productUpdateRequest);
        return "product-update";
    }

    @GetMapping("/products/{id}")
    public String productDetailsPage(
            @PathVariable UUID id,
            Model model) {

        populateProductDetailsModel(id, model);

        return "product-details";
    }

    @PostMapping("/products/{id}/reviews")
    public String createReview(
            @PathVariable UUID id,
            @Valid
            @ModelAttribute("createReviewRequest")
            CreateReviewRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            populateProductDetailsModel(id, model);
            return "product-details";
        }

        try {
            reviewService.createReview(id, request);
        } catch (ReviewAlreadyExistsException exception) {
            bindingResult.rejectValue(
                    "comment",
                    "review.already.exists",
                    "Вече сте оставили отзив за този продукт."
            );

            populateProductDetailsModel(id, model);

            return "product-details";
        }

        return "redirect:/products/" + id;
    }

    @PostMapping("/products/{productId}/reviews/{reviewId}/update")
    public String updateReview(
            @PathVariable UUID productId,
            @PathVariable UUID reviewId,
            @Valid
            @ModelAttribute("updateReviewRequest")
            UpdateReviewRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            populateProductDetailsModel(productId, model);
            return "product-details";
        }

        try {
            reviewService.updateReview(
                    reviewId,
                    request
            );
        } catch (UnauthorizedReviewOperationException exception) {
            bindingResult.reject(
                    "review.update.forbidden",
                    "Нямате право да редактирате този отзив."
            );

            populateProductDetailsModel(productId, model);

            return "product-details";
        }

        return "redirect:/products/" + productId;
    }

    @PostMapping("/products/{productId}/reviews/{reviewId}/delete")
    public String deleteReview(
            @PathVariable UUID productId,
            @PathVariable UUID reviewId) {

        reviewService.deleteReview(reviewId);

        return "redirect:/products/" + productId;
    }

    private void populateProductDetailsModel(
            UUID productId,
            Model model) {

        User currentUser = userService.getCurrentUser();

        model.addAttribute(
                "product",
                productService.getById(productId)
        );

        model.addAttribute(
                "currentUserId",
                currentUser.getId()
        );

        model.addAttribute(
                "reviews",
                reviewService.getReviewsByProductId(productId)
        );

        if (!model.containsAttribute("createReviewRequest")) {
            model.addAttribute(
                    "createReviewRequest",
                    new CreateReviewRequest()
            );
        }

        if (!model.containsAttribute("updateReviewRequest")) {
            model.addAttribute(
                    "updateReviewRequest",
                    new UpdateReviewRequest()
            );
        }
    }
}
