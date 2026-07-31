package app.service;

import app.client.ReviewFeignClient;
import app.model.entity.user.User;
import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.ReviewResponse;
import app.web.dto.review.UpdateReviewRequest;
import app.exception.ReviewAlreadyExistsException;
import app.exception.UnauthorizedReviewOperationException;
import feign.FeignException;
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

        ReviewResponse createdReview;

        try {
            createdReview =
                    reviewFeignClient.createReview(serviceRequest);
        } catch (FeignException.Conflict exception) {
            throw new ReviewAlreadyExistsException(
                    "You have already reviewed this product.",
                    exception
            );
        }

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

        ReviewResponse updatedReview;

        try {
            updatedReview =
                    reviewFeignClient.updateReview(
                            reviewId,
                            serviceRequest
                    );
        } catch (FeignException.Forbidden exception) {
            throw new UnauthorizedReviewOperationException(
                    "You cannot update another user's review.",
                    exception
            );
        }

        log.info(
                "Review updated through review service: reviewId={}, userId={}",
                reviewId,
                currentUser.getId()
        );

        return updatedReview;
    }

    public void deleteReview(UUID reviewId) {
        User currentUser = userService.getCurrentUser();

        try {
            reviewFeignClient.deleteReview(
                    reviewId,
                    currentUser.getId()
            );
        } catch (FeignException.Forbidden exception) {
            throw new UnauthorizedReviewOperationException(
                    "You cannot delete another user's review.",
                    exception
            );
        }

        log.info(
                "Review deleted through review service: reviewId={}, userId={}",
                reviewId,
                currentUser.getId()
        );
    }
}