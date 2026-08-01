package app.web.controller;

import app.model.entity.user.User;
import app.service.CartService;
import app.service.UserService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {
    private final UserService userService;
    private final CartService cartService;

    public GlobalModelAttributes(UserService userService, CartService cartService) {
        this.userService = userService;
        this.cartService = cartService;
    }

    @ModelAttribute("hasAdminPermission")
    public boolean hasAdminPermission() {
        return userService.hasAdminPermission();
    }

    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn() {
        return userService.isLoggedIn();
    }

    @ModelAttribute("cartItemCount")
    public long cartItemCount() {
        if (!userService.isLoggedIn()) {
            return 0L;
        }

        User currentUser = userService.getCurrentUser();

        return cartService.getCartItemCount(currentUser);
    }

    @ModelAttribute("canManageProducts")
    public boolean canManageProducts() {
        return userService.hasAnyAuthority(
                "PRODUCT_CREATE",
                "PRODUCT_UPDATE",
                "PRODUCT_DELETE"
        );
    }

    @ModelAttribute("canManageOrders")
    public boolean canManageOrders() {
        return userService.hasAnyAuthority(
                "ORDER_STATUS_UPDATE"
        );
    }

    @ModelAttribute("canManageUsers")
    public boolean canManageUsers() {
        return userService.hasAnyAuthority(
                "USER_VIEW",
                "USER_ACTIVATE",
                "USER_DEACTIVATE"
        );
    }

    @ModelAttribute("canManageRoles")
    public boolean canManageRoles() {
        return userService.hasAnyAuthority("USER_ROLE_UPDATE");
    }
}
