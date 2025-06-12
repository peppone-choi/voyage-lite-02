package kr.hhplus.be.server.infrastructure.persistance.queue;

import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QueueTokenJpaRepository implements QueueTokenRepository {
    
    private final SpringQueueTokenJpa springQueueTokenJpa;
    
    @Override
    public Optional<QueueToken> findByToken(String token) {
        return springQueueTokenJpa.findByToken(token)
                .map(QueueTokenEntity::toDomain);
    }
    
    @Override
    public Optional<QueueToken> findByUserIdAndStatusIn(String userId, List<QueueToken.Status> statuses) {
        List<QueueTokenEntity.Status> entityStatuses = statuses.stream()
                .map(status -> QueueTokenEntity.Status.valueOf(status.name()))
                .collect(Collectors.toList());
        return springQueueTokenJpa.findByUserIdAndStatusIn(userId, entityStatuses)
                .map(QueueTokenEntity::toDomain);
    }
    
    @Override
    public Long countByStatusAndCreatedAtBefore(QueueToken.Status status, LocalDateTime createdAt) {
        QueueTokenEntity.Status entityStatus = QueueTokenEntity.Status.valueOf(status.name());
        return springQueueTokenJpa.countByStatusAndCreatedAtBefore(entityStatus, createdAt);
    }
    
    @Override
    public Long countByStatus(QueueToken.Status status) {
        QueueTokenEntity.Status entityStatus = QueueTokenEntity.Status.valueOf(status.name());
        return springQueueTokenJpa.countByStatus(entityStatus);
    }
    
    @Override
    public List<QueueToken> findTopNByStatusOrderByCreatedAt(QueueToken.Status status, int limit) {
        QueueTokenEntity.Status entityStatus = QueueTokenEntity.Status.valueOf(status.name());
        return springQueueTokenJpa.findTopNByStatusOrderByCreatedAt(entityStatus, limit)
                .stream()
                .map(QueueTokenEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<QueueToken> findExpiredActiveTokens(LocalDateTime expirationTime) {
        return springQueueTokenJpa.findExpiredActiveTokens(expirationTime)
                .stream()
                .map(QueueTokenEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<QueueToken> findWaitingTokensToActivate(int limit) {
        return springQueueTokenJpa.findWaitingTokensToActivate(limit)
                .stream()
                .map(QueueTokenEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public QueueToken save(QueueToken queueToken) {
        QueueTokenEntity entity = QueueTokenEntity.fromDomain(queueToken);
        QueueTokenEntity savedEntity = springQueueTokenJpa.save(entity);
        return savedEntity.toDomain();
    }
    
    @Override
    public List<QueueToken> saveAll(List<QueueToken> queueTokens) {
        List<QueueTokenEntity> entities = queueTokens.stream()
                .map(QueueTokenEntity::fromDomain)
                .collect(Collectors.toList());
        return springQueueTokenJpa.saveAll(entities).stream()
                .map(QueueTokenEntity::toDomain)
                .collect(Collectors.toList());
    }
}