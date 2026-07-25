package app.web.controller;

import app.model.entity.user.Country;
import app.model.entity.user.Gender;
import app.model.entity.user.User;
import app.service.UserService;
import app.web.dto.user.LoginRequest;
import app.web.dto.user.RegisterRequest;
import app.web.dto.user.UpdateProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
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

    @GetMapping("/profile/edit")
    public String getProfileEditPage(Model model){
        User user = userService.getCurrentUser();
        model.addAttribute("updateProfileRequest", UpdateProfileRequest.from(user));
        model.addAttribute("countries", Country.values());
        model.addAttribute("genders", Gender.values());

        return "profile-edit";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute("updateProfileRequest") UpdateProfileRequest request,
                                BindingResult bindingResult,
                                Model model,
                                HttpServletRequest httpRequest,
                                HttpServletResponse httpResponse){
        if (bindingResult.hasErrors()) {
            model.addAttribute("countries", Country.values());
            model.addAttribute("genders", Gender.values());
            return "profile-edit";
        }
        try {
            boolean emailChanged =
                    userService.updateCurrentUserProfile(request);

            if (emailChanged) {
                Authentication authentication =
                        SecurityContextHolder.getContext()
                                .getAuthentication();

                new SecurityContextLogoutHandler().logout(
                        httpRequest,
                        httpResponse,
                        authentication
                );

                return "redirect:/login?emailChanged=true";
            }

            return "redirect:/profile";

        } catch (UserAlreadyExistsException exception) {
            bindingResult.rejectValue(
                    "email",
                    "email.exists",
                    "Потребител с този email вече съществува."
            );

            model.addAttribute("countries", Country.values());
            model.addAttribute("genders", Gender.values());
            return "profile-edit";
        }
    }


}
