package kr.hhplus.be.server.domain.queue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueTokenRepository {
    
    Optional<QueueToken> findByToken(String token);
    
    Optional<QueueToken> findByUserIdAndStatusIn(String userId, List<QueueToken.Status> statuses);
    
    Long countByStatusAndCreatedAtBefore(QueueToken.Status status, LocalDateTime createdAt);
    
    Long countByStatus(QueueToken.Status status);
    
    List<QueueToken> findTopNByStatusOrderByCreatedAt(QueueToken.Status status, int limit);
    
    List<QueueToken> findExpiredActiveTokens(LocalDateTime expirationTime);
    
    List<QueueToken> findWaitingTokensToActivate(int limit);
    
    QueueToken save(QueueToken queueToken);
    
    List<QueueToken> saveAll(List<QueueToken> queueTokens);
}