package app.service;

import app.client.ReviewFeignClient;
import app.model.entity.user.User;
import app.web.dto.review.CreateReviewRequest;
import app.web.dto.review.ReviewResponse;
import app.web.dto.review.UpdateReviewRequest;
import app.exception.ReviewAlreadyExistsException;
import app.exception.ReviewNotFoundException;
import app.exception.UnauthorizedReviewOperationException;
import app.exception.InvalidReviewDataException;
import app.exception.ReviewServiceUnavailableException;
import feign.FeignException;
import feign.RetryableException;
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
    private final ProductService productService;

    public List<ReviewResponse> getReviewsByProductId(
            UUID productId) {

        validateProductId(productId);

        try {
            return reviewFeignClient
                    .getReviewsByProductId(productId);
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public ReviewResponse createReview(
            UUID productId,
            CreateReviewRequest formRequest) {

        validateCreateRequest(productId, formRequest);

        productService.getById(productId);

        User currentUser = userService.getCurrentUser();

        CreateReviewRequest serviceRequest =
                CreateReviewRequest.builder()
                        .productId(productId)
                        .userId(currentUser.getId())
                        .authorName(currentUser.getName())
                        .rating(formRequest.getRating())
                        .comment(formRequest.getComment().trim())
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
        } catch (FeignException exception) {
            throw translateFeignException(exception);
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

        validateUpdateRequest(reviewId, formRequest);

        User currentUser = userService.getCurrentUser();

        UpdateReviewRequest serviceRequest =
                UpdateReviewRequest.builder()
                        .userId(currentUser.getId())
                        .rating(formRequest.getRating())
                        .comment(formRequest.getComment().trim())
                        .build();

        ReviewResponse updatedReview;

        try {
            updatedReview =
                    reviewFeignClient.updateReview(
                            reviewId,
                            serviceRequest
                    );
        } catch (FeignException.NotFound exception) {
            throw new ReviewNotFoundException(
                    "Review with ID "
                            + reviewId
                            + " was not found.",
                    exception
            );
        } catch (FeignException.Forbidden exception) {
            throw new UnauthorizedReviewOperationException(
                    "You cannot update another user's review.",
                    exception
            );
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }

        log.info(
                "Review updated through review service: reviewId={}, userId={}",
                reviewId,
                currentUser.getId()
        );

        return updatedReview;
    }

    public void deleteReview(UUID reviewId) {

        validateReviewId(reviewId);

        User currentUser = userService.getCurrentUser();

        try {
            reviewFeignClient.deleteReview(
                    reviewId,
                    currentUser.getId()
            );
        } catch (FeignException.NotFound exception) {
            throw new ReviewNotFoundException(
                    "Review with ID "
                            + reviewId
                            + " was not found.",
                    exception
            );
        } catch (FeignException.Forbidden exception) {
            throw new UnauthorizedReviewOperationException(
                    "You cannot delete another user's review.",
                    exception
            );
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }

        log.info(
                "Review deleted through review service: reviewId={}, userId={}",
                reviewId,
                currentUser.getId()
        );
    }

    private void validateCreateRequest(
            UUID productId,
            CreateReviewRequest request) {

        validateProductId(productId);

        if (request == null) {
            throw new InvalidReviewDataException(
                    "Review request is required."
            );
        }

        validateReviewContent(
                request.getRating(),
                request.getComment()
        );
    }

    private void validateUpdateRequest(
            UUID reviewId,
            UpdateReviewRequest request) {

        validateReviewId(reviewId);

        if (request == null) {
            throw new InvalidReviewDataException(
                    "Review update request is required."
            );
        }

        validateReviewContent(
                request.getRating(),
                request.getComment()
        );
    }

    private void validateProductId(UUID productId) {
        if (productId == null) {
            throw new InvalidReviewDataException(
                    "Product id is required."
            );
        }
    }

    private void validateReviewId(UUID reviewId) {
        if (reviewId == null) {
            throw new InvalidReviewDataException(
                    "Review id is required."
            );
        }
    }

    private void validateReviewContent(
            Integer rating,
            String comment) {

        if (rating == null) {
            throw new InvalidReviewDataException(
                    "Rating is required."
            );
        }

        if (rating < 1 || rating > 5) {
            throw new InvalidReviewDataException(
                    "Rating must be between 1 and 5."
            );
        }

        if (comment == null || comment.isBlank()) {
            throw new InvalidReviewDataException(
                    "Comment is required."
            );
        }

        int commentLength = comment.trim().length();

        if (commentLength < 10 || commentLength > 1000) {
            throw new InvalidReviewDataException(
                    "Comment must be between 10 and 1000 symbols."
            );
        }
    }

    private RuntimeException translateFeignException(
            FeignException exception) {

        if (exception instanceof RetryableException
                || exception.status() < 0
                || exception.status() >= 500) {

            return new ReviewServiceUnavailableException(
                    "Review service is currently unavailable. "
                            + "Please try again later.",
                    exception
            );
        }

        return new InvalidReviewDataException(
                "The review service rejected the submitted data.",
                exception
        );
    }
}