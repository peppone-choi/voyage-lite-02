package kr.hhplus.be.server.infrastructure.persistance.seat;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.seat.model.Seat;
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
    private Status status;
    
    private String reservedBy;
    
    private LocalDateTime reservedAt;
    
    @Version
    private Long version;
    
    public enum Status {
        AVAILABLE, TEMPORARY_RESERVED, RESERVED
    }
    
    public static SeatEntity fromDomain(Seat seat) {
        return SeatEntity.builder()
                .id(seat.getId())
                .scheduleId(seat.getScheduleId())
                .seatNumber(seat.getSeatNumber())
                .grade(seat.getGrade())
                .price(seat.getPrice())
                .status(mapDomainToEntity(seat.getStatus()))
                .reservedBy(seat.getReservedBy())
                .reservedAt(seat.getReservedAt())
                .version(seat.getVersion())
                .build();
    }
    
    public Seat toDomain() {
        return Seat.builder()
                .id(this.id)
                .scheduleId(this.scheduleId)
                .seatNumber(this.seatNumber)
                .grade(this.grade)
                .price(this.price)
                .status(mapEntityToDomain(this.status))
                .reservedBy(this.reservedBy)
                .reservedAt(this.reservedAt)
                .version(this.version)
                .build();
    }
    
    private static Status mapDomainToEntity(Seat.Status domainStatus) {
        return Status.valueOf(domainStatus.name());
    }
    
    private static Seat.Status mapEntityToDomain(Status entityStatus) {
        return Seat.Status.valueOf(entityStatus.name());
    }
}