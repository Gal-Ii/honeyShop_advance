package app.exception;

import feign.FeignException;
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

    @ExceptionHandler(ProductNotFoundException.class)
    public ModelAndView handleProductNotFound(
            ProductNotFoundException exception) {

        ModelAndView modelAndView = new ModelAndView("error/404");

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
            FeignException.Forbidden.class
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
