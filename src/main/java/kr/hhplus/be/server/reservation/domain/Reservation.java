package kr.hhplus.be.server.reservation.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_user_schedule", columnList = "userId,scheduleId"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_reserved_at", columnList = "reservedAt")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private Long scheduleId;
    
    @Column(nullable = false)
    private Long seatId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @Column(nullable = false)
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
            throw new IllegalStateException("임시 예약만 확정할 수 있습니다");
        }
        this.status = Status.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.paymentId = paymentId;
    }
    
    public void cancel() {
        if (this.status == Status.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약입니다");
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
}