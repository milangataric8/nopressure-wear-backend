package rs.nopressurewear.exception;

/**
 * The request is well-formed and the caller is authorized, but performing it would violate
 * a business invariant (e.g. demoting the last active SUPER_ADMIN). Maps to {@code 409}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
