package app.service;

import app.client.ReviewFeignClient;
import app.model.entity.user.User;
import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.ReviewResponse;
import app.web.dto.review.UpdateReviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewFeignClient reviewFeignClient;
    private final UserService userService;

    public List<ReviewResponse> getReviewsByProductId(UUID productId) {
        return reviewFeignClient.getReviewsByProductId(productId);
    }

    public ReviewResponse createReview(
            UUID productId,
            CreateReviewRequest formRequest) {

        User currentUser = userService.getCurrentUser();

        CreateReviewRequest serviceRequest =
                CreateReviewRequest.builder()
                        .productId(productId)
                        .userId(currentUser.getId())
                        .authorName(currentUser.getName())
                        .rating(formRequest.getRating())
                        .comment(formRequest.getComment())
                        .build();

        ReviewResponse createdReview =
                reviewFeignClient.createReview(serviceRequest);

        log.info(
                "Review created through review service: reviewId={}, productId={}, userId={}",
                createdReview.getId(),
                productId,
                currentUser.getId()
        );

        return createdReview;
    }

    public ReviewResponse updateReview(
            UUID reviewId,
            UpdateReviewRequest formRequest) {

        User currentUser = userService.getCurrentUser();

        UpdateReviewRequest serviceRequest =
                UpdateReviewRequest.builder()
                        .userId(currentUser.getId())
                        .rating(formRequest.getRating())
                        .comment(formRequest.getComment())
                        .build();

        ReviewResponse updatedReview =
                reviewFeignClient.updateReview(
                        reviewId,
                        serviceRequest
                );

        log.info(
                "Review updated through review service: reviewId={}, userId={}",
                reviewId,
                currentUser.getId()
        );

        return updatedReview;
    }

    public void deleteReview(UUID reviewId) {
        User currentUser = userService.getCurrentUser();

        reviewFeignClient.deleteReview(
                reviewId,
                currentUser.getId()
        );

        log.info(
                "Review deleted through review service: reviewId={}, userId={}",
                reviewId,
                currentUser.getId()
        );
    }
}