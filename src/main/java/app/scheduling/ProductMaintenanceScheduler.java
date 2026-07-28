package app.scheduling;

import app.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductMaintenanceScheduler {

    private final ProductService productService;

    @Scheduled(
            cron = "${scheduling.products.deactivate-out-of-stock-cron}",
            zone = "${scheduling.zone}"
    )
    public void deactivateOutOfStockProducts() {

        int deactivatedProducts =
                productService.deactivateOutOfStockProducts();

        if (deactivatedProducts == 0) {
            log.debug(
                    "No out-of-stock products required deactivation."
            );
        }
    }
}
