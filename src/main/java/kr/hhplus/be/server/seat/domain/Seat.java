package kr.hhplus.be.server.seat.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "seats", indexes = {
        @Index(name = "idx_schedule_seat", columnList = "scheduleId,seatNumber", unique = true),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long scheduleId;
    
    @Column(nullable = false)
    private Integer seatNumber;
    
    @Column(nullable = false)
    private String grade;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    private String reservedBy;
    
    private LocalDateTime reservedAt;
    
    public enum Status {
        AVAILABLE,
        TEMPORARY_RESERVED,
        RESERVED
    }
    
    private static final int MIN_SEAT_NUMBER = 1;
    private static final int MAX_SEAT_NUMBER = 50;
    private static final int TEMPORARY_RESERVATION_MINUTES = 5;
    
    @PrePersist
    @PreUpdate
    private void validateSeatNumber() {
        if (seatNumber < MIN_SEAT_NUMBER || seatNumber > MAX_SEAT_NUMBER) {
            throw new IllegalArgumentException("좌석 번호는 " + MIN_SEAT_NUMBER + "부터 " + MAX_SEAT_NUMBER + " 사이여야 합니다");
        }
    }
    
    public void temporaryReserve(String userId) {
        if (!isAvailable()) {
            throw new IllegalStateException("예약 가능한 좌석이 아닙니다");
        }
        this.status = Status.TEMPORARY_RESERVED;
        this.reservedBy = userId;
        this.reservedAt = LocalDateTime.now();
    }
    
    public void confirmReservation() {
        if (this.status != Status.TEMPORARY_RESERVED) {
            throw new IllegalStateException("임시 예약된 좌석이 아닙니다");
        }
        this.status = Status.RESERVED;
    }
    
    public void releaseReservation() {
        this.status = Status.AVAILABLE;
        this.reservedBy = null;
        this.reservedAt = null;
    }
    
    public boolean isTemporaryReservationExpired() {
        if (status != Status.TEMPORARY_RESERVED || reservedAt == null) {
            return false;
        }
        LocalDateTime expirationTime = reservedAt.plusMinutes(TEMPORARY_RESERVATION_MINUTES);
        return LocalDateTime.now().isAfter(expirationTime);
    }
    
    public boolean isReservedBy(String userId) {
        return userId != null && userId.equals(this.reservedBy);
    }
    
    public boolean isAvailable() {
        return this.status == Status.AVAILABLE;
    }
}