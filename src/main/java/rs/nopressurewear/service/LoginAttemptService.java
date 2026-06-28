package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.UserRepository;

import java.time.LocalDateTime;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;

    @Value("${security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${security.lock-duration-minutes:15}")
    private int lockMinutes;

    @Transactional(propagation = REQUIRES_NEW)
    public void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxAttempts) {
            user.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
        }
        userRepository.save(user);
    }
}
