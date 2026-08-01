package app.web.controller;

import app.exception.UnauthorizedActionException;
import app.model.entity.user.User;
import app.service.ProductService;
import app.service.UserService;
import app.web.dto.cart.AddToCartRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final ProductService productService;
    private final UserService userService;

    public PageController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping({"/index", "/"})
    public String indexPage(Model model) {
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("addToCartRequest", new AddToCartRequest());
        return "index";
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        try {
            User currentUser = userService.getCurrentUser();
            model.addAttribute("user", currentUser);

            return "profile";
        } catch (UnauthorizedActionException e) {
            return "redirect:/login";
        }
    }

    @GetMapping("/products")
    public String productsPage(Model model) {
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("addToCartRequest", new AddToCartRequest());

        return "products";
    }

}
