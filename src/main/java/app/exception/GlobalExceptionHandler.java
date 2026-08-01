package app.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ProductNotFoundException.class,
            UserNotFoundException.class,
            ReviewNotFoundException.class
    })
    public ModelAndView handleNotFound(
            RuntimeException exception) {

        ModelAndView modelAndView =
                new ModelAndView("error/404");

        modelAndView.setStatus(HttpStatus.NOT_FOUND);

        modelAndView.addObject(
                "errorMessage",
                exception.getMessage()
        );

        return modelAndView;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception) {

        ModelAndView modelAndView = new ModelAndView("error/400");

        modelAndView.setStatus(HttpStatus.BAD_REQUEST);
        modelAndView.addObject(
                "errorMessage",
                "Подаденият адрес съдържа невалидна стойност."
        );

        return modelAndView;
    }

    @ExceptionHandler({
            AccessDeniedException.class,
            UnauthorizedActionException.class,
            UnauthorizedReviewOperationException.class
    })
    public ModelAndView handleForbiddenOperation() {

        ModelAndView modelAndView = new ModelAndView("error/403");

        modelAndView.setStatus(HttpStatus.FORBIDDEN);
        modelAndView.addObject(
                "errorMessage",
                "Нямате право да извършите тази операция."
        );

        return modelAndView;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleMissingResource() {

        ModelAndView modelAndView =
                new ModelAndView("error/404");

        modelAndView.setStatus(HttpStatus.NOT_FOUND);
        modelAndView.addObject(
                "errorMessage",
                "The requested resource was not found."
        );

        return modelAndView;
    }

    @ExceptionHandler({
            InvalidCartDataException.class,
            InvalidOrderDataException.class,
            InvalidProductDataException.class,
            InvalidReviewDataException.class,
            InvalidUserDataException.class
    })
    public ModelAndView handleInvalidOperation(
            RuntimeException exception) {

        ModelAndView modelAndView =
                new ModelAndView("error/400");

        modelAndView.setStatus(HttpStatus.BAD_REQUEST);

        modelAndView.addObject(
                "errorMessage",
                exception.getMessage()
        );

        return modelAndView;
    }

    @ExceptionHandler({
            NotEnoughQuantityException.class,
            ProductAlreadyExistsException.class,
            ReviewAlreadyExistsException.class,
            UserAlreadyExistsException.class
    })
    public ModelAndView handleConflict(
            RuntimeException exception) {

        ModelAndView modelAndView =
                new ModelAndView("error/409");

        modelAndView.setStatus(HttpStatus.CONFLICT);

        modelAndView.addObject(
                "errorMessage",
                exception.getMessage()
        );

        return modelAndView;
    }

    @ExceptionHandler(
            ReviewServiceUnavailableException.class
    )
    public ModelAndView handleReviewServiceUnavailable(
            ReviewServiceUnavailableException exception) {

        ModelAndView modelAndView =
                new ModelAndView("error/503");

        modelAndView.setStatus(
                HttpStatus.SERVICE_UNAVAILABLE
        );

        modelAndView.addObject(
                "errorMessage",
                exception.getMessage()
        );

        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpectedException(
            Exception exception) {

        log.error(
                "Unexpected application error.",
                exception
        );

        ModelAndView modelAndView = new ModelAndView("error/500");

        modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        modelAndView.addObject(
                "errorMessage",
                "Възникна неочаквана грешка. Моля, опитайте отново."
        );

        return modelAndView;
    }
}
