package kr.hhplus.be.server.infrastructure.persistance.payment;

import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentJpaRepository implements PaymentRepository {
    
    private final SpringPaymentJpa springPaymentJpa;
    
    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = toEntity(payment);
        PaymentEntity savedEntity = springPaymentJpa.save(entity);
        payment.assignId(savedEntity.getId());
        return payment;
    }
    
    @Override
    public Optional<Payment> findById(Long id) {
        return springPaymentJpa.findById(id)
                .map(this::toDomainModel);
    }
    
    @Override
    public Optional<Payment> findByReservationId(Long reservationId) {
        return springPaymentJpa.findByReservationId(reservationId)
                .map(this::toDomainModel);
    }
    
    @Override
    public List<Payment> findByUserIdAndStatus(String userId, Payment.Status status) {
        return springPaymentJpa.findByUserIdAndStatus(userId, status)
                .stream()
                .map(this::toDomainModel)
                .toList();
    }
    
    @Override
    public List<Payment> findCompletedPaymentsByUserIdAndPeriod(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        return springPaymentJpa.findCompletedPaymentsByUserIdAndPeriod(userId, startDate, endDate)
                .stream()
                .map(this::toDomainModel)
                .toList();
    }
    
    @Override
    public boolean existsActivePaymentByReservationId(Long reservationId) {
        return springPaymentJpa.existsActivePaymentByReservationId(reservationId);
    }
    
    
    private PaymentEntity toEntity(Payment payment) {
        return PaymentEntity.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .reservationId(payment.getReservationId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .failedAt(payment.getFailedAt())
                .failureReason(payment.getFailureReason())
                .cancelledAt(payment.getCancelledAt())
                .cancelReason(payment.getCancelReason())
                .build();
    }
    
    private Payment toDomainModel(PaymentEntity entity) {
        Payment payment = Payment.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .reservationId(entity.getReservationId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .paidAt(entity.getPaidAt())
                .failedAt(entity.getFailedAt())
                .failureReason(entity.getFailureReason())
                .cancelledAt(entity.getCancelledAt())
                .cancelReason(entity.getCancelReason())
                .build();
        return payment;
    }
}