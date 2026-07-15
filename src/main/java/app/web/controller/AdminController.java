package app.web.controller;

import app.model.entity.order.OrderStatus;
import app.model.entity.user.UserRole;
import app.service.OrderService;
import app.service.ProductService;
import app.service.UserService;
import app.web.dto.order.UpdateOrderStatusRequest;
import app.web.dto.user.UpdateUserRoleRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Arrays;
import java.util.UUID;

@Controller
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;

    public AdminController(ProductService productService, OrderService orderService, UserService userService) {
        this.productService = productService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('ORDER_STATUS_UPDATE')")
    @GetMapping("/admin")
    public String adminPage(Model model){
        model.addAttribute("orders", orderService.getAllOrders());
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("updateOrderStatusRequest", new UpdateOrderStatusRequest());
        return "admin";
    }

    @PreAuthorize("hasAuthority('ORDER_STATUS_UPDATE')")
    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable UUID id,
                                    @Valid @ModelAttribute("updateOrderStatusRequest") UpdateOrderStatusRequest updateOrderStatusRequest,
                                    BindingResult bindingResult,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("orders", orderService.getAllOrders());
            model.addAttribute("orderStatuses", OrderStatus.values());
            return "admin";
        }

        orderService.updateStatus(id, updateOrderStatusRequest);
        return "redirect:/admin";
    }

    @PreAuthorize("""
        hasAnyAuthority(
            'PRODUCT_CREATE',
            'PRODUCT_UPDATE',
            'PRODUCT_DELETE'
        )
        """)
    @GetMapping("/admin-products")
    public String adminProductsPage(Model model){
        model.addAttribute("products", productService.getAllProducts());
        return "admin-products";
    }

    @PreAuthorize("hasAuthority('USER_VIEW')")
    @GetMapping("/admin-users")
    public String adminUsersPage(Model model) {
        populateUsersModel(model);
        return "admin-users";
    }

    @PreAuthorize("hasAuthority('USER_ACTIVATE')")
    @PostMapping("/admin/users/{id}/activate")
    public String activateUser(@PathVariable UUID id) {
        userService.activateUser(id);
        return "redirect:/admin-users";
    }

    @PreAuthorize("hasAuthority('USER_DEACTIVATE')")
    @PostMapping("/admin/users/{id}/deactivate")
    public String deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
        return "redirect:/admin-users";
    }

    @PreAuthorize("hasAuthority('USER_ROLE_UPDATE')")
    @PostMapping("/admin/users/{id}/role")
    public String updateUserRole(
            @PathVariable UUID id,
            @Valid @ModelAttribute("updateUserRoleRequest")
            UpdateUserRoleRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            populateUsersModel(model);
            return "admin-users";
        }

        userService.updateUserRole(id, request.getRole());

        return "redirect:/admin-users";
    }

    private void populateUsersModel(Model model) {
        model.addAttribute("users", userService.getAll());

        model.addAttribute(
                "userRoles",
                Arrays.stream(UserRole.values())
                        .filter(role -> role != UserRole.ADMIN)
                        .toList()
        );
    }
}
