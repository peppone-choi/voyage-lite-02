package kr.hhplus.be.server.payment.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class Payment {
    
    private Long id;
    private final String userId;
    private final Long reservationId;
    private final BigDecimal amount;
    private Status status;
    private final LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime failedAt;
    private String failureReason;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    
    public enum Status {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    public static Payment create(String userId, Long reservationId, BigDecimal amount) {
        validateAmount(amount);
        return Payment.builder()
                .userId(userId)
                .reservationId(reservationId)
                .amount(amount)
                .status(Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    public void assignId(Long id) {
        this.id = id;
    }
    
    private static void validateAmount(BigDecimal amount) {
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
    
    public boolean isRefundable() {
        if (this.status != Status.COMPLETED || this.paidAt == null) {
            return false;
        }
        return this.paidAt.plusDays(7).isAfter(LocalDateTime.now());
    }
}