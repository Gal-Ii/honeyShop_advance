package app.web.controller;

import app.service.UserService;
import app.web.dto.user.LoginRequest;
import app.web.dto.user.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import app.exception.UserAlreadyExistsException;


@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerPage(Model model){
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerRequest") RegisterRequest request, BindingResult bindingResult){
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(request);
        } catch (UserAlreadyExistsException e) {
            if (e.getMessage().toLowerCase().contains("email")) {
                bindingResult.rejectValue(
                        "email",
                        "email.exists",
                        "Потребител с този email вече съществува."
                );
            } else if (e.getMessage().toLowerCase().contains("name")) {
                bindingResult.rejectValue(
                        "name",
                        "name.exists",
                        "Потребител с това име вече съществува."
                );
            } else {
                bindingResult.reject(
                        "user.exists",
                        "Потребител с тези данни вече съществува."
                );
            }

            return "register";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(Model model){
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @GetMapping("/logout")
    public String logoutPage() {
        return "logout";
    }
}
