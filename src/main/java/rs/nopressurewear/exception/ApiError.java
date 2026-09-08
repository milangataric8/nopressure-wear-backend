package rs.nopressurewear.exception;

import rs.nopressurewear.constants.ErrorCode;

import java.util.Map;

/**
 * The one shape every error response uses — from {@link GlobalExceptionHandler} and from
 * security filters alike (e.g. {@code PasswordChangeRequiredFilter}), so they can't drift apart.
 * See {@code backend-message-i18n-spec.md} section 3.3.
 * <p>
 * Deliberately carries no literal message text: {@code code} is for programmatic branching,
 * {@code messageKey} is an i18n key for the top-level toast/banner, and {@code fieldErrors} maps
 * field name to i18n key for form-level display. Both are resolved client-side by
 * {@code react-i18next} so the same backend response reads correctly in any UI language.
 */
public record ApiError(int status, String code, String messageKey, Map<String, String> fieldErrors) {

    public ApiError(int status, ErrorCode code, String messageKey) {
        this(status, code.name(), messageKey, null);
    }

    public ApiError(int status, ErrorCode code, String messageKey, Map<String, String> fieldErrors) {
        this(status, code.name(), messageKey, fieldErrors);
    }
}
