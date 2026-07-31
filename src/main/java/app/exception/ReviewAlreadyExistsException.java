package app.exception;

public class ReviewAlreadyExistsException
        extends RuntimeException {

    public ReviewAlreadyExistsException(String message) {
        super(message);
    }

    public ReviewAlreadyExistsException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}
