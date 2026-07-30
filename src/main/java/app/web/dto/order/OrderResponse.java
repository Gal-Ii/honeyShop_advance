package app.web.dto.order;

import app.model.entity.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private UUID id;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private LocalDateTime createdOn;
    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();
}
