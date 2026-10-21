package rs.nopressurewear.platform.error;

public class LoginDisabledException extends RuntimeException {
    public LoginDisabledException(String message) {
        super(message);
    }
}
