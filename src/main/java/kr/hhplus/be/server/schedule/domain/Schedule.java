package kr.hhplus.be.server.schedule.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedules", indexes = {
        @Index(name = "idx_concert_id", columnList = "concertId"),
        @Index(name = "idx_performance_date", columnList = "performanceDate")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long concertId;
    
    @Column(nullable = false)
    private LocalDate performanceDate;
    
    @Column(nullable = false)
    private LocalDateTime performanceTime;
    
    @Column(nullable = false)
    private Integer totalSeats;
    
    @Column(nullable = false)
    private Integer availableSeats;
    
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