package kr.hhplus.be.server.schedule.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class Schedule {
    
    private Long id;
    private final Long concertId;
    private final LocalDate performanceDate;
    private final LocalDateTime performanceTime;
    private final Integer totalSeats;
    private Integer availableSeats;
    
    public static Schedule create(Long concertId, LocalDate performanceDate, LocalDateTime performanceTime, Integer totalSeats) {
        return Schedule.builder()
                .concertId(concertId)
                .performanceDate(performanceDate)
                .performanceTime(performanceTime)
                .totalSeats(totalSeats)
                .availableSeats(totalSeats)
                .build();
    }
    
    public void assignId(Long id) {
        this.id = id;
    }
    
    public void reserveSeat() {
        if (availableSeats <= 0) {
            throw new IllegalStateException("예약 가능한 좌석이 없습니다");
        }
        this.availableSeats--;
    }
    
    public void cancelSeatReservation() {
        if (availableSeats >= totalSeats) {
            throw new IllegalStateException("전체 좌석 수를 초과할 수 없습니다");
        }
        this.availableSeats++;
    }
    
    public boolean isSoldOut() {
        return availableSeats == 0;
    }
    
    public boolean isPast() {
        return performanceTime.isBefore(LocalDateTime.now());
    }
    
    public boolean isAvailableForReservation() {
        return !isPast() && !isSoldOut();
    }
}