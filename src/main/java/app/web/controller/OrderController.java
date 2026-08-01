package app.web.controller;

import app.exception.InvalidCartDataException;
import app.exception.NotEnoughQuantityException;
import app.exception.UnauthorizedActionException;
import app.model.entity.user.User;
import app.service.CartService;
import app.service.OrderService;
import app.service.UserService;
import app.web.dto.cart.CartItemResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final CartService cartService;

    public OrderController(OrderService orderService, UserService userService, CartService cartService) {
        this.orderService = orderService;
        this.userService = userService;
        this.cartService = cartService;
    }

    @PostMapping("/orders")
    public String createOrder(Model model) {
        try {
            User currentUser = userService.getCurrentUser();
            orderService.createOrder(currentUser);
            return "redirect:/orders";
        } catch (UnauthorizedActionException e) {
            return "redirect:/login";
        } catch (InvalidCartDataException | NotEnoughQuantityException e) {
            User currentUser = userService.getCurrentUser();
            List<CartItemResponse> cartItems = cartService.getCart(currentUser);

            model.addAttribute("cartItems", cartItems);
            model.addAttribute(
                    "totalPrice",
                    cartService.calculateCartTotal(cartItems)
            );
            model.addAttribute("orderError", e.getMessage());

            return "cart";
        }
    }

    @GetMapping("/orders")
    public String myOrders(Model model) {
        try {
            User currentUser = userService.getCurrentUser();
            model.addAttribute("orders", orderService.getMyOrders(currentUser));

            return "orders";
        } catch (UnauthorizedActionException e) {
            return "redirect:/login";
        }
    }

}
