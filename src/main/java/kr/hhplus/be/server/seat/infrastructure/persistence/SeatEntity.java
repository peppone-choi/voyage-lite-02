package kr.hhplus.be.server.seat.infrastructure.persistence;

import jakarta.persistence.*;
import kr.hhplus.be.server.seat.domain.model.Seat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_schedule_seat", columnList = "scheduleId,seatNumber", unique = true),
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatEntity {
    
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
    private Seat.Status status;
    
    private String reservedBy;
    
    private LocalDateTime reservedAt;
}