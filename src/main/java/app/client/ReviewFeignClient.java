package app.client;

import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.ReviewResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import app.web.dto.review.UpdateReviewRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "review-service",
        url = "${review.service.base-url}"
)
public interface ReviewFeignClient {

    @GetMapping("/api/v1/reviews")
    List<ReviewResponse> getReviewsByProductId(
            @RequestParam("productId") UUID productId
    );

    @PostMapping("/api/v1/reviews")
    ReviewResponse createReview(
            @RequestBody CreateReviewRequest request
    );

    @PutMapping("/api/v1/reviews/{reviewId}")
    ReviewResponse updateReview(
            @PathVariable("reviewId") UUID reviewId,
            @RequestBody UpdateReviewRequest request
    );

    @DeleteMapping("/api/v1/reviews/{reviewId}")
    void deleteReview(
            @PathVariable("reviewId") UUID reviewId,
            @RequestParam("userId") UUID userId
    );
}
