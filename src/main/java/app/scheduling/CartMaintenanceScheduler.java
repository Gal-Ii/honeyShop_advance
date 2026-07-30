package app.scheduling;

import app.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartMaintenanceScheduler {

    private static final int CART_EXPIRATION_DAYS = 7;

    private final CartService cartService;

    @Scheduled(
            fixedDelayString =
                    "${scheduling.cart.cleanup-fixed-delay}"
    )
    public void removeExpiredCartItems() {

        LocalDateTime expirationDate =
                LocalDateTime.now()
                        .minusDays(CART_EXPIRATION_DAYS);

        int removedItems =
                cartService.removeExpiredCartItems(expirationDate);

        if (removedItems == 0) {
            log.debug("No expired cart items were found.");
        }
    }
}
