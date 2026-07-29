package app.web.controller;

import app.exception.UserAlreadyExistsException;
import app.model.entity.user.Country;
import app.model.entity.user.Gender;
import app.model.entity.user.User;
import app.service.UserService;
import app.web.dto.user.LoginRequest;
import app.web.dto.user.RegisterRequest;
import app.web.dto.user.UpdateProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService);
    }

    @Test
    void registerPageShouldReturnRegisterView() {
        String view = userController.registerPage(model);

        assertEquals("register", view);

        verify(model).addAttribute(
                eq("registerRequest"),
                any(RegisterRequest.class)
        );
        verifyNoInteractions(userService);
    }

    @Test
    void loginPageShouldReturnLoginView() {
        String view = userController.loginPage(model);

        assertEquals("login", view);

        verify(model).addAttribute(
                eq("loginRequest"),
                any(LoginRequest.class)
        );
        verifyNoInteractions(userService);
    }

    @Test
    void logoutPageShouldReturnLogoutView() {
        String view = userController.logoutPage();

        assertEquals("logout", view);
        verifyNoInteractions(userService, model);
    }

    @Test
    void getProfileEditPageShouldPopulateFormData() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Ivan Ivanov")
                .email("ivan@example.com")
                .country(Country.BULGARIA)
                .gender(Gender.MALE)
                .profilePicture("/images/avatar.png")
                .build();

        when(userService.getCurrentUser())
                .thenReturn(user);

        String view =
                userController.getProfileEditPage(model);

        assertEquals("profile-edit", view);

        verify(userService).getCurrentUser();

        verify(model).addAttribute(
                eq("updateProfileRequest"),
                any(UpdateProfileRequest.class)
        );
        verify(model).addAttribute(
                "countries",
                Country.values()
        );
        verify(model).addAttribute(
                "genders",
                Gender.values()
        );
    }

    @Test
    void registerUserShouldRedirectToLoginWhenRequestIsValid() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Ivan Ivanov")
                .email("ivan@example.com")
                .password("password123")
                .country(Country.BULGARIA)
                .gender(Gender.MALE)
                .build();

        when(bindingResult.hasErrors())
                .thenReturn(false);

        String view = userController.registerUser(
                request,
                bindingResult
        );

        assertEquals("redirect:/login", view);

        verify(bindingResult).hasErrors();
        verify(userService).register(request);
    }

    @Test
    void registerUserShouldReturnFormWhenValidationFails() {
        RegisterRequest request = new RegisterRequest();

        when(bindingResult.hasErrors())
                .thenReturn(true);

        String view = userController.registerUser(
                request,
                bindingResult
        );

        assertEquals("register", view);

        verify(bindingResult).hasErrors();
        verifyNoInteractions(userService);
    }

    @Test
    void updateProfileShouldRedirectToProfileWhenEmailIsUnchanged() {
        UpdateProfileRequest request =
                UpdateProfileRequest.builder()
                        .name("Ivan Ivanov")
                        .email("ivan@example.com")
                        .country(Country.BULGARIA)
                        .gender(Gender.MALE)
                        .profilePicture("/images/avatar.png")
                        .build();

        when(bindingResult.hasErrors())
                .thenReturn(false);

        when(userService.updateCurrentUserProfile(request))
                .thenReturn(false);

        String view = userController.updateProfile(
                request,
                bindingResult,
                model,
                httpRequest,
                httpResponse
        );

        assertEquals("redirect:/profile", view);

        verify(bindingResult).hasErrors();
        verify(userService)
                .updateCurrentUserProfile(request);

        verifyNoInteractions(
                model,
                httpRequest,
                httpResponse
        );
    }

    @Test
    void updateProfileShouldReturnFormWhenValidationFails() {
        UpdateProfileRequest request =
                new UpdateProfileRequest();

        when(bindingResult.hasErrors())
                .thenReturn(true);

        String view = userController.updateProfile(
                request,
                bindingResult,
                model,
                httpRequest,
                httpResponse
        );

        assertEquals("profile-edit", view);

        verify(bindingResult).hasErrors();

        verify(model).addAttribute(
                "countries",
                Country.values()
        );
        verify(model).addAttribute(
                "genders",
                Gender.values()
        );

        verifyNoInteractions(
                userService,
                httpRequest,
                httpResponse
        );
    }

    @Test
    void updateProfileShouldReturnFormWhenEmailAlreadyExists() {
        UpdateProfileRequest request =
                UpdateProfileRequest.builder()
                        .name("Ivan Ivanov")
                        .email("existing@example.com")
                        .country(Country.BULGARIA)
                        .gender(Gender.MALE)
                        .profilePicture("/images/avatar.png")
                        .build();

        when(bindingResult.hasErrors())
                .thenReturn(false);

        when(userService.updateCurrentUserProfile(request))
                .thenThrow(
                        new UserAlreadyExistsException(
                                "User with this email already exists."
                        )
                );

        String view = userController.updateProfile(
                request,
                bindingResult,
                model,
                httpRequest,
                httpResponse
        );

        assertEquals("profile-edit", view);

        verify(userService)
                .updateCurrentUserProfile(request);

        verify(bindingResult).rejectValue(
                eq("email"),
                eq("email.exists"),
                anyString()
        );

        verify(model).addAttribute(
                "countries",
                Country.values()
        );

        verify(model).addAttribute(
                "genders",
                Gender.values()
        );

        verifyNoInteractions(
                httpRequest,
                httpResponse
        );
    }

    @Test
    void registerUserShouldReturnFormWhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Ivan Ivanov")
                .email("existing@example.com")
                .password("password123")
                .country(Country.BULGARIA)
                .gender(Gender.MALE)
                .build();

        when(bindingResult.hasErrors())
                .thenReturn(false);

        doThrow(
                new UserAlreadyExistsException(
                        "User with this email already exists."
                )
        ).when(userService).register(request);

        String view = userController.registerUser(
                request,
                bindingResult
        );

        assertEquals("register", view);

        verify(userService).register(request);

        verify(bindingResult).rejectValue(
                eq("email"),
                eq("email.exists"),
                anyString()
        );
    }
}
