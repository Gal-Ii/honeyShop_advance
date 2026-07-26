package app.web.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponse {

    private UUID id;
    private UUID productId;
    private UUID userId;
    private String authorName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}
