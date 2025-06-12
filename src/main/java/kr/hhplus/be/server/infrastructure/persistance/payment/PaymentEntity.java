package kr.hhplus.be.server.infrastructure.persistance.payment;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.payment.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_reservation_id", columnList = "reservationId"),
    @Index(name = "idx_user_id_status", columnList = "userId,status"),
    @Index(name = "idx_paid_at", columnList = "paidAt")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, unique = true)
    private Long reservationId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Payment.Status status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
    
    private LocalDateTime failedAt;
    
    private String failureReason;
    
    private LocalDateTime cancelledAt;
    
    private String cancelReason;
}