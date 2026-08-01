package app.exception;

public class ReviewNotFoundException
        extends RuntimeException {

    public ReviewNotFoundException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}