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
import rs.nopressurewear.constants.ErrorCode;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;
import static rs.nopressurewear.constants.ErrorCode.ACCESS_DENIED;
import static rs.nopressurewear.constants.ErrorCode.ACCOUNT_LOCKED;
import static rs.nopressurewear.constants.ErrorCode.EMAIL_ALREADY_TAKEN;
import static rs.nopressurewear.constants.ErrorCode.EMAIL_NOT_VERIFIED;
import static rs.nopressurewear.constants.ErrorCode.INVALID_CREDENTIALS;
import static rs.nopressurewear.constants.ErrorCode.INVALID_TOKEN;
import static rs.nopressurewear.constants.ErrorCode.LOGIN_DISABLED;
import static rs.nopressurewear.constants.ErrorCode.REGISTRATION_DISABLED;
import static rs.nopressurewear.constants.ErrorCode.RESOURCE_NOT_FOUND;
import static rs.nopressurewear.constants.ErrorCode.VALIDATION_FAILED;

/**
 * Every handler below returns {@link ApiError} — never a literal message, only a {@code code} for
 * programmatic branching and i18n keys the frontend resolves via {@code react-i18next}. See
 * {@code backend-message-i18n-spec.md} section 3.3.
 * <p>
 * Exceptions that don't carry a per-field message (e.g. {@link ResourceNotFoundException},
 * {@link ConflictException}) map to one fixed {@code messageKey} per exception type regardless of
 * {@code ex.getMessage()} — that text stays English, on purpose, as a developer/log-facing detail
 * (spec section 5), never forwarded to the client. Only {@link FieldValidationException} and
 * {@link DuplicateResourceException} forward their message, because it lands in {@code fieldErrors}
 * as an i18n key the caller chose deliberately.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(UNAUTHORIZED)
                .body(new ApiError(UNAUTHORIZED.value(),
                        INVALID_CREDENTIALS,
                        "errors.invalidCredentials"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(FORBIDDEN)
                .body(new ApiError(FORBIDDEN.value(),
                        ACCESS_DENIED,
                        "errors.accessDenied"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // keep the first message per field (a field can carry multiple constraints)
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ApiError(BAD_REQUEST.value(),
                        VALIDATION_FAILED,
                        "errors.validationFailed", fieldErrors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> fieldErrors = Map.of(ex.getName(), "errors.invalidParameter");
        return ResponseEntity.badRequest()
                .body(new ApiError(BAD_REQUEST.value(),
                        VALIDATION_FAILED,
                        "errors.invalidParameter", fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String path = v.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.putIfAbsent(field, v.getMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ApiError(BAD_REQUEST.value(),
                        VALIDATION_FAILED,
                        "errors.validationFailed", fieldErrors));
    }

    /** {@code ex.getMessage()} is an i18n key by convention (e.g. {@code "validation.newPassword.sameAsCurrent"}) —
     * callers of {@link FieldValidationException} are responsible for passing a key, never a sentence. */
    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ApiError> handleFieldValidation(FieldValidationException ex) {
        String key = ex.getMessage();
        Map<String, String> fieldErrors = ex.getField() != null ? Map.of(ex.getField(), key) : null;
        return ResponseEntity.badRequest()
                .body(new ApiError(BAD_REQUEST.value(),
                        VALIDATION_FAILED,
                        key,
                        fieldErrors));
    }

    /** Same key-not-sentence contract as {@link FieldValidationException}; {@code field == "email"} is common
     * enough (registration, staff creation, profile update) to get its own {@link ErrorCode}. */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicateResource(DuplicateResourceException ex) {
        String key = ex.getMessage();
        ErrorCode code = "email".equals(ex.getField())
                ? EMAIL_ALREADY_TAKEN
                : ErrorCode.CONFLICT;
        Map<String, String> fieldErrors = ex.getField() != null ? Map.of(ex.getField(), key) : null;
        return ResponseEntity.status(CONFLICT)
                .body(new ApiError(CONFLICT.value(),
                        code,
                        key,
                        fieldErrors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(NOT_FOUND)
                .body(new ApiError(NOT_FOUND.value(),
                        RESOURCE_NOT_FOUND,
                        "errors.resourceNotFound"));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict() {
        return ResponseEntity.status(CONFLICT)
                .body(new ApiError(CONFLICT.value(),
                        ErrorCode.CONFLICT,
                        "errors.conflict"));
    }

    @ExceptionHandler(RegistrationDisabledException.class)
    public ResponseEntity<ApiError> handleRegistrationDisabled(RegistrationDisabledException ex) {
        return ResponseEntity.status(FORBIDDEN)
                .body(new ApiError(FORBIDDEN.value(),
                        REGISTRATION_DISABLED,
                        "errors.registrationDisabled"));
    }

    @ExceptionHandler(LoginDisabledException.class)
    public ResponseEntity<ApiError> handleLoginDisabled(LoginDisabledException ex) {
        return ResponseEntity.status(FORBIDDEN)
                .body(new ApiError(FORBIDDEN.value(),
                        LOGIN_DISABLED,
                        "errors.loginDisabled"));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiError> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError(BAD_REQUEST.value(),
                        INVALID_TOKEN,
                        "errors.invalidToken"));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiError> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity.status(FORBIDDEN)
                .body(new ApiError(FORBIDDEN.value(),
                        EMAIL_NOT_VERIFIED,
                        "errors.emailNotVerified"));
    }

    /** The lockout duration used to be interpolated into the message text; it no longer is (spec's own
     * frontend example message is static) — the frontend shows a fixed "account locked" message. */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiError> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity.status(LOCKED)
                .body(new ApiError(LOCKED.value(),
                        ACCOUNT_LOCKED,
                        "errors.accountLocked"));
    }

    /** Catch-all for a plain/unclassified {@link RuntimeException} thrown somewhere in the service layer
     * (e.g. an expired-token check that predates {@link InvalidTokenException}). {@code ex.getMessage()} is
     * developer-facing only now — never forwarded — so callers aren't required to pass a key here. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError(BAD_REQUEST.value(),
                        ErrorCode.BAD_REQUEST,
                        "errors.badRequest"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        Sentry.captureException(ex);
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(new ApiError(INTERNAL_SERVER_ERROR.value(),
                        ErrorCode.INTERNAL_ERROR,
                        "errors.unexpected"));
    }
}
