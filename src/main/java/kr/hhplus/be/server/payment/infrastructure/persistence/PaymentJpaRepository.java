package kr.hhplus.be.server.payment.infrastructure.persistence;

import kr.hhplus.be.server.payment.domain.PaymentRepository;
import kr.hhplus.be.server.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    @Repository
    interface SpringPaymentJpa extends JpaRepository<PaymentEntity, Long> {
        
        Optional<PaymentEntity> findByReservationId(Long reservationId);
        
        List<PaymentEntity> findByUserIdAndStatus(String userId, Payment.Status status);
        
        @Query("SELECT p FROM PaymentEntity p WHERE p.userId = :userId " +
               "AND p.status = kr.hhplus.be.server.payment.domain.model.Payment$Status.COMPLETED " +
               "AND p.paidAt BETWEEN :startDate AND :endDate")
        List<PaymentEntity> findCompletedPaymentsByUserIdAndPeriod(@Param("userId") String userId,
                                                                 @Param("startDate") LocalDateTime startDate,
                                                                 @Param("endDate") LocalDateTime endDate);
        
        @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PaymentEntity p " +
               "WHERE p.reservationId = :reservationId " +
               "AND p.status IN (kr.hhplus.be.server.payment.domain.model.Payment$Status.COMPLETED, " +
               "kr.hhplus.be.server.payment.domain.model.Payment$Status.PENDING)")
        boolean existsActivePaymentByReservationId(@Param("reservationId") Long reservationId);
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