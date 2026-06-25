package rs.nopressurewear.exception;

public class LoginDisabledException extends RuntimeException {
    public LoginDisabledException(String message) {
        super(message);
    }
}
