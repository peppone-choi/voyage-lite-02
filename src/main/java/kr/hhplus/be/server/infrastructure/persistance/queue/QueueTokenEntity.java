package kr.hhplus.be.server.infrastructure.persistance.queue;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.queue.QueueToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_tokens")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueTokenEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private Integer position;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime activatedAt;
    
    private LocalDateTime expiredAt;
    
    public enum Status {
        WAITING, ACTIVE, EXPIRED
    }
    
    public static QueueTokenEntity fromDomain(QueueToken queueToken) {
        return QueueTokenEntity.builder()
                .id(queueToken.getId())
                .token(queueToken.getToken())
                .userId(queueToken.getUserId())
                .position(queueToken.getPosition())
                .status(mapStatus(queueToken.getStatus()))
                .createdAt(queueToken.getCreatedAt())
                .activatedAt(queueToken.getActivatedAt())
                .expiredAt(queueToken.getExpiredAt())
                .build();
    }
    
    public QueueToken toDomain() {
        return QueueToken.builder()
                .id(this.id)
                .token(this.token)
                .userId(this.userId)
                .position(this.position)
                .status(mapStatus(this.status))
                .createdAt(this.createdAt)
                .activatedAt(this.activatedAt)
                .expiredAt(this.expiredAt)
                .build();
    }
    
    private static Status mapStatus(QueueToken.Status domainStatus) {
        return Status.valueOf(domainStatus.name());
    }
    
    private static QueueToken.Status mapStatus(Status entityStatus) {
        return QueueToken.Status.valueOf(entityStatus.name());
    }
}