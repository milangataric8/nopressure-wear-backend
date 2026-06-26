package rs.nopressurewear.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.nopressurewear.model.EmailVerificationToken;
import rs.nopressurewear.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidatePreviousTokens(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
