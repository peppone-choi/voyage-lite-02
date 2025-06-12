package kr.hhplus.be.server.domain.seat.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class Seat {
    
    private Long id;
    private final Long scheduleId;
    private final Integer seatNumber;
    private final String grade;
    private final BigDecimal price;
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
    
    public static Seat create(Long scheduleId, Integer seatNumber, String grade, BigDecimal price) {
        validateSeatNumber(seatNumber);
        return Seat.builder()
                .scheduleId(scheduleId)
                .seatNumber(seatNumber)
                .grade(grade)
                .price(price)
                .status(Status.AVAILABLE)
                .build();
    }
    
    public void assignId(Long id) {
        this.id = id;
    }
    
    private static void validateSeatNumber(Integer seatNumber) {
        if (seatNumber < MIN_SEAT_NUMBER || seatNumber > MAX_SEAT_NUMBER) {
            throw new IllegalArgumentException("좌석 번호는 " + MIN_SEAT_NUMBER + "부터 " + MAX_SEAT_NUMBER + " 사이여야 합니다");
        }
    }
    
    public void temporaryReserve(String userId) {
        if (!isAvailable()) {
            throw new IllegalStateException("좌석을 예약할 수 없습니다");
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