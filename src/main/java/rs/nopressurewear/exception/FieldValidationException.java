package rs.nopressurewear.exception;

/**
 * Business-rule validation failure that maps to a single form field, so the
 * frontend can attach the message to its input just like a Bean Validation error.
 * Use for rules that can't be expressed with annotations — invalid coupon,
 * insufficient stock, wrong current password.
 */
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String message) {
        this(message, null);
    }

    public FieldValidationException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
