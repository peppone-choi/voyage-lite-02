package kr.hhplus.be.server.infrastructure.persistance.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kr.hhplus.be.server.domain.reservation.model.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
public class ReservationEntity {
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
    private Reservation.Status status;

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
}
