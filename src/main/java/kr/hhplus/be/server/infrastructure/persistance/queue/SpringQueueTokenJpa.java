package kr.hhplus.be.server.infrastructure.persistance.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringQueueTokenJpa extends JpaRepository<QueueTokenEntity, Long> {
    
    Optional<QueueTokenEntity> findByToken(String token);
    
    @Query("SELECT qt FROM QueueTokenEntity qt WHERE qt.userId = :userId AND qt.status IN :statuses")
    Optional<QueueTokenEntity> findByUserIdAndStatusIn(@Param("userId") String userId, 
                                                  @Param("statuses") List<QueueTokenEntity.Status> statuses);
    
    @Query("SELECT COUNT(qt) FROM QueueTokenEntity qt WHERE qt.status = :status AND qt.createdAt < :createdAt")
    Long countByStatusAndCreatedAtBefore(@Param("status") QueueTokenEntity.Status status, 
                                        @Param("createdAt") LocalDateTime createdAt);
    
    Long countByStatus(QueueTokenEntity.Status status);
    
    @Query(value = "SELECT * FROM queue_tokens WHERE status = :#{#status.name()} ORDER BY created_at ASC LIMIT :limit", 
           nativeQuery = true)
    List<QueueTokenEntity> findTopNByStatusOrderByCreatedAt(@Param("status") QueueTokenEntity.Status status, 
                                                      @Param("limit") int limit);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT qt FROM QueueTokenEntity qt WHERE qt.status = kr.hhplus.be.server.infrastructure.persistance.queue.QueueTokenEntity$Status.ACTIVE " +
           "AND qt.activatedAt < :expirationTime")
    List<QueueTokenEntity> findExpiredActiveTokens(@Param("expirationTime") LocalDateTime expirationTime);
    
    @Query(value = "SELECT * FROM queue_tokens WHERE status = 'WAITING' ORDER BY created_at ASC LIMIT :limit", 
           nativeQuery = true)
    List<QueueTokenEntity> findWaitingTokensToActivate(@Param("limit") int limit);
}