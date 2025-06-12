package kr.hhplus.be.server.domain.reservation.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Reservation {
    private Long id;

    private String userId;

    private Long scheduleId;

    private Long seatId;

    private Status status;

    private LocalDateTime reservedAt;
    
    private LocalDateTime confirmedAt;
    
    private LocalDateTime expiredAt;
    
    private LocalDateTime cancelledAt;
    
    private Long paymentId;
    
    public enum Status {
        TEMPORARY_RESERVED,
        CONFIRMED,
        EXPIRED,
        CANCELLED
    }
    
    private static final int TEMPORARY_RESERVATION_MINUTES = 5;
    
    public void confirm(Long paymentId) {
        if (this.status != Status.TEMPORARY_RESERVED) {
            throw new IllegalStateException("Only temporary reservations can be confirmed");
        }
        this.status = Status.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.paymentId = paymentId;
    }
    
    public void cancel() {
        if (this.status == Status.CANCELLED) {
            throw new IllegalStateException("Reservation is already cancelled");
        }
        this.status = Status.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
    
    public void expire() {
        if (this.status != Status.TEMPORARY_RESERVED) {
            throw new IllegalStateException("임시 예약만 만료시킬 수 있습니다");
        }
        this.status = Status.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        if (status != Status.TEMPORARY_RESERVED) {
            return false;
        }
        LocalDateTime expirationTime = reservedAt.plusMinutes(TEMPORARY_RESERVATION_MINUTES);
        return LocalDateTime.now().isAfter(expirationTime);
    }
    
    public LocalDateTime getExpirationTime() {
        return reservedAt.plusMinutes(TEMPORARY_RESERVATION_MINUTES);
    }
    
    public boolean isActive() {
        return status == Status.TEMPORARY_RESERVED || status == Status.CONFIRMED;
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("이미 ID가 할당된 예약입니다");
        }
        this.id = id;
    }
}