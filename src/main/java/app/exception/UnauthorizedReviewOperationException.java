package app.exception;

public class UnauthorizedReviewOperationException
        extends RuntimeException {

    public UnauthorizedReviewOperationException(String message) {
        super(message);
    }

    public UnauthorizedReviewOperationException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}
