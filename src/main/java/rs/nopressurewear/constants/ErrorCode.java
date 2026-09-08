package rs.nopressurewear.constants;

/**
 * Machine-stable codes for the {@code code} field of {@code ApiError} — part of the API contract,
 * not the message text. The frontend branches on these; renaming one is a breaking change.
 * See {@code backend-message-i18n-spec.md} section 3.1.
 */
public enum ErrorCode {

    // password / auth
    PASSWORD_CHANGE_REQUIRED,
    PASSWORD_TOO_SHORT,
    PASSWORD_INVALID_FORMAT,
    PASSWORD_REQUIRED,
    PASSWORD_CONFIRMATION_REQUIRED,
    PASSWORDS_DO_NOT_MATCH,
    PASSWORD_SAME_AS_CURRENT,
    CURRENT_PASSWORD_REQUIRED,
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED,
    EMAIL_NOT_VERIFIED,
    REGISTRATION_DISABLED,
    LOGIN_DISABLED,
    INVALID_TOKEN,

    // identity fields
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    EMAIL_ALREADY_TAKEN,
    FIRST_NAME_REQUIRED,
    LAST_NAME_REQUIRED,

    // generic / fallback
    VALIDATION_FAILED,
    RESOURCE_NOT_FOUND,
    ACCESS_DENIED,
    CONFLICT,
    BAD_REQUEST,
    INTERNAL_ERROR
    // extend as encountered
}
