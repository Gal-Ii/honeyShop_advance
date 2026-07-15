package app.web.controller;

import app.service.UserService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {
    private final UserService userService;

    public GlobalModelAttributes(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("hasAdminPermission")
    public boolean hasAdminPermission() {
        return userService.hasAdminPermission();
    }

    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn(){
        return userService.isLoggedIn();
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
