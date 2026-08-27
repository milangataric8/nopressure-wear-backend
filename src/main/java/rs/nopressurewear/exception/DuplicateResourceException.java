package rs.nopressurewear.exception;

public class DuplicateResourceException extends RuntimeException {

    private final String field;

    public DuplicateResourceException(String message) {
        this(message, null);
    }

    public DuplicateResourceException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
