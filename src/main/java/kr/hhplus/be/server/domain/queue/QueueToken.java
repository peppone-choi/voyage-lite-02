package kr.hhplus.be.server.domain.queue;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "queue_tokens", indexes = {
        @Index(name = "idx_token", columnList = "token"),
        @Index(name = "idx_user_id_status", columnList = "userId,status"),
        @Index(name = "idx_status_created_at", columnList = "status,createdAt")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @Column(nullable = false)
    private String userId;
    
    @Setter
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
        WAITING,
        ACTIVE,
        EXPIRED
    }
    
    private static final int ACTIVE_DURATION_MINUTES = 5;
    
    public void activate() {
        if (this.status == Status.ACTIVE) {
            throw new IllegalStateException("Token is already active");
        }
        if (this.status == Status.EXPIRED) {
            throw new IllegalStateException("만료된 토큰은 활성화할 수 없습니다");
        }
        
        this.status = Status.ACTIVE;
        this.position = 0;
        this.activatedAt = LocalDateTime.now();
    }
    
    public void expire() {
        if (this.status == Status.EXPIRED) {
            throw new IllegalStateException("Token is already expired");
        }
        if (this.status == Status.WAITING) {
            throw new IllegalStateException("Cannot expire waiting token");
        }
        
        this.status = Status.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return this.status == Status.ACTIVE;
    }
    
    public boolean isExpired() {
        return this.status == Status.EXPIRED;
    }
    
    public boolean isWaiting() {
        return this.status == Status.WAITING;
    }
    
    public long getRemainingActiveTimeSeconds() {
        if (!isActive() || activatedAt == null) {
            return 0;
        }
        
        LocalDateTime expirationTime = activatedAt.plusMinutes(ACTIVE_DURATION_MINUTES);
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(expirationTime)) {
            return 0;
        }
        
        return ChronoUnit.SECONDS.between(now, expirationTime);
    }
    
    public boolean shouldAutoExpire() {
        return isActive() && getRemainingActiveTimeSeconds() == 0;
    }
}