package kr.hhplus.be.server.domain.payment;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_reservation_id", columnList = "reservationId", unique = true),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, unique = true)
    private Long reservationId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
    
    private LocalDateTime failedAt;
    
    private LocalDateTime cancelledAt;
    
    private String failureReason;
    
    private String cancelReason;
    
    public enum Status {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    private static final int REFUND_DAYS_LIMIT = 7;
    
    @PrePersist
    @PreUpdate
    private void validateAmount() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다");
        }
    }
    
    public void complete() {
        if (this.status == Status.COMPLETED) {
            throw new IllegalStateException("이미 완료된 결제입니다");
        }
        this.status = Status.COMPLETED;
        this.paidAt = LocalDateTime.now();
    }
    
    public void fail(String reason) {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("대기 중인 결제만 실패 처리할 수 있습니다");
        }
        this.status = Status.FAILED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
    }
    
    public void cancel(String reason) {
        if (this.status != Status.COMPLETED) {
            throw new IllegalStateException("완료된 결제만 취소할 수 있습니다");
        }
        if (this.status == Status.CANCELLED) {
            throw new IllegalStateException("이미 취소된 결제입니다");
        }
        this.status = Status.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }
    
    public boolean isCompleted() {
        return this.status == Status.COMPLETED;
    }
    
    public boolean isRefundable() {
        if (this.status != Status.COMPLETED || paidAt == null) {
            return false;
        }
        long daysSincePaid = ChronoUnit.DAYS.between(paidAt, LocalDateTime.now());
        return daysSincePaid <= REFUND_DAYS_LIMIT;
    }
}