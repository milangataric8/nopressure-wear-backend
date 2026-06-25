package rs.nopressurewear.exception;

public class RegistrationDisabledException extends RuntimeException {
    public RegistrationDisabledException(String message) {
        super(message);
    }
}
