package app.scheduling;

import app.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartMaintenanceSchedulerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartMaintenanceScheduler scheduler;

    @Test
    void removeExpiredCartItemsShouldUseDateSevenDaysAgo() {

        when(cartService.removeExpiredCartItems(
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(2);

        LocalDateTime expectedDate =
                LocalDateTime.now().minusDays(7);

        scheduler.removeExpiredCartItems();

        ArgumentCaptor<LocalDateTime> captor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        verify(cartService)
                .removeExpiredCartItems(captor.capture());

        LocalDateTime actualDate = captor.getValue();

        assertTrue(actualDate.isAfter(expectedDate.minusSeconds(1)));
        assertTrue(actualDate.isBefore(expectedDate.plusSeconds(1)));
    }
}
