package rs.nopressurewear.exception;

import io.sentry.Sentry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(UNAUTHORIZED.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(FORBIDDEN.value())
                .message("Nemate ovlašćenje za ovu akciju.")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(FORBIDDEN).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(BAD_REQUEST.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // keep the first message per field (a field can carry multiple constraints)
            validationErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }

        ErrorResponse error = ErrorResponse.builder()
                .status(BAD_REQUEST.value())
                .message("Validation failed")
                .errors(validationErrors)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String param = ex.getName();
        Class<?> required = ex.getRequiredType();
        String allowed = "";
        if (required != null && required.isEnum()) {
            allowed = " Allowed values: "
                    + Arrays.stream(required.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "))
                    + ".";
        }

        ErrorResponse error = ErrorResponse.builder()
                .status(BAD_REQUEST.value())
                .message("Invalid value '" + ex.getValue() + "' for parameter '" + param + "'." + allowed)
                .errors(Map.of(param, "Invalid value"))
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String path = v.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            validationErrors.putIfAbsent(field, v.getMessage());
        }

        ErrorResponse error = ErrorResponse.builder()
                .status(BAD_REQUEST.value())
                .message("Validation failed")
                .errors(validationErrors)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ErrorResponse> handleFieldValidation(FieldValidationException ex) {
        ErrorResponse.ErrorResponseBuilder body = ErrorResponse.builder()
                .status(BAD_REQUEST.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now());

        if (ex.getField() != null) {
            body.errors(Map.of(ex.getField(), ex.getMessage()));
        }

        return ResponseEntity.badRequest().body(body.build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        Sentry.captureException(ex);
        ErrorResponse error = ErrorResponse.builder()
                .status(INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(CONFLICT.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(CONFLICT).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        ErrorResponse.ErrorResponseBuilder body = ErrorResponse.builder()
                .status(CONFLICT.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now());

        if (ex.getField() != null) {
            body.errors(Map.of(ex.getField(), ex.getMessage()));
        }

        return ResponseEntity.status(CONFLICT).body(body.build());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(NOT_FOUND.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(NOT_FOUND).body(error);
    }

    @ExceptionHandler(RegistrationDisabledException.class)
    public ResponseEntity<ErrorResponse> handleRegistrationDisabled(RegistrationDisabledException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(FORBIDDEN.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(FORBIDDEN).body(error);
    }

    @ExceptionHandler(LoginDisabledException.class)
    public ResponseEntity<ErrorResponse> handleLoginDisabled(LoginDisabledException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(FORBIDDEN.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(FORBIDDEN).body(error);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(BAD_REQUEST.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(EmailNotVerifiedException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(FORBIDDEN.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(FORBIDDEN).body(error);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(LOCKED.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(LOCKED).body(error);
    }
}
