package kr.hhplus.be.server.schedule.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
public class ScheduleEntity {
    
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
}