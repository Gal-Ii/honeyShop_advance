package app.exception;

public class ReviewServiceUnavailableException
        extends RuntimeException {

    public ReviewServiceUnavailableException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}
