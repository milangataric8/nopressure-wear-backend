package rs.nopressurewear.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.nopressurewear.model.FailedEmail;

import java.time.LocalDateTime;
import java.util.List;

public interface FailedEmailRepository extends JpaRepository<FailedEmail, Long> {

    /**
     * The emails due for a retry attempt right now. Capped at 50 per run so one large
     * backlog can't monopolise the scheduler.
     */
    List<FailedEmail> findTop50ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            String status, LocalDateTime before);

    List<FailedEmail> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);

    @Modifying
    @Query("DELETE FROM FailedEmail f WHERE f.status = 'SENT' AND f.sentAt < :cutoff")
    void deleteSentBefore(@Param("cutoff") LocalDateTime cutoff);
}
