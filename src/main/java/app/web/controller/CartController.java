package app.web.controller;

import app.exception.InvalidCartDataException;
import app.exception.NotEnoughQuantityException;
import app.exception.UnauthorizedActionException;
import app.model.entity.user.User;
import app.service.CartService;
import app.service.ProductService;
import app.service.UserService;
import app.web.dto.cart.AddToCartRequest;
import app.web.dto.cart.CartItemResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
public class CartController {
    private final CartService cartService;
    private final UserService userService;
    private final ProductService productService;

    public CartController(CartService cartService, UserService userService, ProductService productService) {
        this.cartService = cartService;
        this.userService = userService;
        this.productService = productService;
    }

    @GetMapping("/cart")
    public String cartPage(Model model) {
        try {
            User currentUser = userService.getCurrentUser();
            List<CartItemResponse> cartItems = cartService.getCart(currentUser);
            model.addAttribute("cartItems", cartItems);

            model.addAttribute(
                    "totalPrice",
                    cartService.calculateCartTotal(cartItems)
            );

            return "cart";
        } catch (UnauthorizedActionException e) {
            return "redirect:/login";
        }
    }

    @PostMapping("/cart")
    public String addToCart(@Valid @ModelAttribute("addToCartRequest") AddToCartRequest request,
                            BindingResult bindingResult,
                            @RequestParam(defaultValue = "products") String source,
                            Model model) {
        User currentUser;
        try {
            currentUser = userService.getCurrentUser();
        } catch (UnauthorizedActionException e) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            return returnProductPage(source, model);
        }

        try {
            cartService.addToCart(currentUser, request);
        } catch (UnauthorizedActionException e) {
            return "redirect:/login";
        } catch (InvalidCartDataException e) {
            model.addAttribute(
                    "cartError",
                    e.getMessage()
            );

            return returnProductPage(source, model);
        } catch (NotEnoughQuantityException e) {
            bindingResult.rejectValue("quantity", "cart.quantity.notEnough", e.getMessage());
            return returnProductPage(source, model);
        }

        return "redirect:/cart";
    }

    private String returnProductPage(
            String source,
            Model model) {

        model.addAttribute(
                "products",
                productService.getAllActiveProducts()
        );

        if ("index".equals(source)) {
            return "index";
        }

        return "products";
    }

    @PostMapping("/cart/{id}/delete")
    public String removeFromCart(@PathVariable UUID id) {
        User currentUser = userService.getCurrentUser();
        cartService.removeFromCart(currentUser, id);

        return "redirect:/cart";
    }
}
