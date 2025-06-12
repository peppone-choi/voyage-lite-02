package kr.hhplus.be.server.infrastructure.persistance.payment;

import kr.hhplus.be.server.domain.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringPaymentJpa extends JpaRepository<PaymentEntity, Long> {
    
    Optional<PaymentEntity> findByReservationId(Long reservationId);
    
    List<PaymentEntity> findByUserIdAndStatus(String userId, Payment.Status status);
    
    @Query("SELECT p FROM PaymentEntity p WHERE p.userId = :userId " +
           "AND p.status = 'COMPLETED' " +
           "AND p.paidAt BETWEEN :startDate AND :endDate")
    List<PaymentEntity> findCompletedPaymentsByUserIdAndPeriod(@Param("userId") String userId,
                                                             @Param("startDate") LocalDateTime startDate,
                                                             @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PaymentEntity p " +
           "WHERE p.reservationId = :reservationId " +
           "AND p.status IN ('COMPLETED', 'PENDING')")
    boolean existsActivePaymentByReservationId(@Param("reservationId") Long reservationId);
}