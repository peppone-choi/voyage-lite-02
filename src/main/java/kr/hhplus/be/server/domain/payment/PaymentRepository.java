package kr.hhplus.be.server.domain.payment;

import kr.hhplus.be.server.domain.payment.model.Payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    
    Payment save(Payment payment);
    
    Optional<Payment> findById(Long id);
    
    Optional<Payment> findByReservationId(Long reservationId);
    
    List<Payment> findByUserIdAndStatus(String userId, Payment.Status status);
    
    List<Payment> findCompletedPaymentsByUserIdAndPeriod(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    boolean existsActivePaymentByReservationId(Long reservationId);
}