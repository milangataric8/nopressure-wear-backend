package rs.nopressurewear.platform.error;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import rs.nopressurewear.platform.api.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code backend-message-i18n-spec.md} section 3.3: every handler returns {@link ApiError} —
 * a stable {@code code}, an i18n {@code messageKey}, and (only for field-scoped failures)
 * {@code fieldErrors} — never a literal message. Deliberately not a {@code @WebMvcTest}: these
 * handler methods are plain functions of the exception, so calling them directly is both faster
 * and a more precise unit than asserting on serialized JSON.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void badCredentials_mapsToFixedKey_regardlessOfExceptionMessage() {
        ApiError error = handler.handleBadCredentials(new BadCredentialsException("Invalid email or password")).getBody();

        assertThat(error.status()).isEqualTo(401);
        assertThat(error.code()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.name());
        assertThat(error.messageKey()).isEqualTo("errors.invalidCredentials");
        assertThat(error.fieldErrors()).isNull();
    }

    @Test
    void accessDenied_mapsToFixedKey_regardlessOfExceptionMessage() {
        ApiError error = handler.handleAccessDenied(new AccessDeniedException("Nemate ovlašćenje za ovu akciju.")).getBody();

        assertThat(error.status()).isEqualTo(403);
        assertThat(error.code()).isEqualTo(ErrorCode.ACCESS_DENIED.name());
        assertThat(error.messageKey()).isEqualTo("errors.accessDenied");
    }

    @Test
    void resourceNotFound_dropsTheDeveloperFacingMessage_infavorOfAFixedKey() {
        ApiError error = handler.handleResourceNotFoundException(new ResourceNotFoundException("User not found")).getBody();

        assertThat(error.status()).isEqualTo(404);
        assertThat(error.code()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.name());
        assertThat(error.messageKey()).isEqualTo("errors.resourceNotFound");
    }

    @Test
    void fieldValidationException_forwardsItsMessageAsAnI18nKey_intoFieldErrors() {
        ApiError error = handler.handleFieldValidation(
                new FieldValidationException("validation.newPassword.sameAsCurrent", "newPassword")).getBody();

        assertThat(error.status()).isEqualTo(400);
        assertThat(error.messageKey()).isEqualTo("validation.newPassword.sameAsCurrent");
        assertThat(error.fieldErrors()).containsEntry("newPassword", "validation.newPassword.sameAsCurrent");
    }

    @Test
    void fieldValidationException_withoutAField_hasNoFieldErrors() {
        ApiError error = handler.handleFieldValidation(new FieldValidationException("errors.coupon.codeTaken")).getBody();

        assertThat(error.fieldErrors()).isNull();
        assertThat(error.messageKey()).isEqualTo("errors.coupon.codeTaken");
    }

    @Test
    void duplicateResource_onEmailField_getsTheDedicatedErrorCode() {
        ApiError error = handler.handleDuplicateResource(
                new DuplicateResourceException("validation.email.taken", "email")).getBody();

        assertThat(error.status()).isEqualTo(409);
        assertThat(error.code()).isEqualTo(ErrorCode.EMAIL_ALREADY_TAKEN.name());
        assertThat(error.fieldErrors()).containsEntry("email", "validation.email.taken");
    }

    @Test
    void duplicateResource_onAnyOtherField_fallsBackToGenericConflict() {
        ApiError error = handler.handleDuplicateResource(
                new DuplicateResourceException("errors.category.nameTaken")).getBody();

        assertThat(error.code()).isEqualTo(ErrorCode.CONFLICT.name());
        assertThat(error.fieldErrors()).isNull();
    }

    @Test
    void accountLocked_dropsTheInterpolatedMinutesText_infavorOfAStaticKey() {
        ApiError error = handler.handleAccountLocked(
                new AccountLockedException("Account locked. Try again in 7 minute(s).")).getBody();

        assertThat(error.status()).isEqualTo(423);
        assertThat(error.messageKey()).isEqualTo("errors.accountLocked");
    }

    @Test
    void genericRuntimeException_stillMapsToAKey_neverTheRawMessage() {
        ApiError error = handler.handleRuntimeException(new RuntimeException("Token has expired")).getBody();

        assertThat(error.status()).isEqualTo(400);
        assertThat(error.messageKey()).isEqualTo("errors.badRequest");
        assertThat(error.messageKey()).doesNotContain("Token has expired");
    }
}
