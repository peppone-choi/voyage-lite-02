package kr.hhplus.be.server.queue.repository;

import kr.hhplus.be.server.queue.domain.QueueToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueTokenRepository extends JpaRepository<QueueToken, Long> {
    
    Optional<QueueToken> findByToken(String token);
    
    @Query("SELECT qt FROM QueueToken qt WHERE qt.userId = :userId AND qt.status IN :statuses")
    Optional<QueueToken> findByUserIdAndStatusIn(@Param("userId") String userId, 
                                                  @Param("statuses") List<QueueToken.Status> statuses);
    
    @Query("SELECT COUNT(qt) FROM QueueToken qt WHERE qt.status = :status AND qt.createdAt < :createdAt")
    Long countByStatusAndCreatedAtBefore(@Param("status") QueueToken.Status status, 
                                        @Param("createdAt") LocalDateTime createdAt);
    
    Long countByStatus(QueueToken.Status status);
    
    @Query(value = "SELECT * FROM queue_tokens WHERE status = :#{#status.name()} ORDER BY created_at ASC LIMIT :limit", 
           nativeQuery = true)
    List<QueueToken> findTopNByStatusOrderByCreatedAt(@Param("status") QueueToken.Status status, 
                                                      @Param("limit") int limit);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT qt FROM QueueToken qt WHERE qt.status = kr.hhplus.be.server.queue.domain.QueueToken$Status.ACTIVE " +
           "AND qt.activatedAt < :expirationTime")
    List<QueueToken> findExpiredActiveTokens(@Param("expirationTime") LocalDateTime expirationTime);
    
    @Query(value = "SELECT * FROM queue_tokens WHERE status = 'WAITING' ORDER BY created_at ASC LIMIT :limit", 
           nativeQuery = true)
    List<QueueToken> findWaitingTokensToActivate(@Param("limit") int limit);
}