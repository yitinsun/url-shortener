package com.yitinsun.urlshortener.web;

import com.yitinsun.urlshortener.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return FriendlyErrorPages.entity(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler({
            SanitizationException.class,
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> badRequest(Exception ex) {
        String message = messageOf(ex);
        return error(HttpStatus.BAD_REQUEST, message, null);
    }

    @ExceptionHandler({DataAccessException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ErrorResponse> unavailable(Exception ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Database unavailable", null);
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message, String shortCode) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message, shortCode);
        return ResponseEntity.status(status).body(body);
    }

    private static String messageOf(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException validation) {
            return validation.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .orElse("Validation failed");
        }
        return ex.getMessage() == null ? "Bad request" : ex.getMessage();
    }
}
