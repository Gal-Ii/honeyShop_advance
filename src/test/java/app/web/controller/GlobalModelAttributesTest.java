package app.web.controller;

import app.model.entity.user.User;
import app.service.CartService;
import app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalModelAttributesTest {

    @Mock
    private UserService userService;

    @Mock
    private CartService cartService;

    private GlobalModelAttributes globalModelAttributes;

    @BeforeEach
    void setUp() {
        globalModelAttributes = new GlobalModelAttributes(
                userService,
                cartService
        );
    }

    @Test
    void cartItemCountShouldReturnZeroForAnonymousUser() {
        when(userService.isLoggedIn()).thenReturn(false);

        long result =
                globalModelAttributes.cartItemCount();

        assertEquals(0L, result);

        verify(userService).isLoggedIn();
        verifyNoMoreInteractions(userService);
        verifyNoInteractions(cartService);
    }

    @Test
    void cartItemCountShouldReturnCurrentUserCartQuantity() {
        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .build();

        when(userService.isLoggedIn()).thenReturn(true);
        when(userService.getCurrentUser())
                .thenReturn(currentUser);
        when(cartService.getCartItemCount(currentUser))
                .thenReturn(5L);

        long result =
                globalModelAttributes.cartItemCount();

        assertEquals(5L, result);

        verify(userService).isLoggedIn();
        verify(userService).getCurrentUser();
        verify(cartService).getCartItemCount(currentUser);
        verifyNoMoreInteractions(userService, cartService);
    }
}
