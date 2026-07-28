package app.service;

import app.client.ReviewFeignClient;
import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import app.web.dto.review.UpdateReviewRequest;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewFeignClient reviewFeignClient;

    public List<ReviewResponse> getReviewsByProductId(UUID productId) {

        return reviewFeignClient.getReviewsByProductId(productId);
    }

    public ReviewResponse createReview(CreateReviewRequest request) {

        return reviewFeignClient.createReview(request);
    }

    public ReviewResponse updateReview(
            UUID reviewId,
            UpdateReviewRequest request) {

        return reviewFeignClient.updateReview(reviewId, request);
    }

    public void deleteReview(UUID reviewId, UUID userId) {

        reviewFeignClient.deleteReview(reviewId, userId);
    }
}