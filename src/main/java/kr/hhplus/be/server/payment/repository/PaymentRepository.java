package kr.hhplus.be.server.payment.repository;

import kr.hhplus.be.server.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByReservationId(Long reservationId);
    
    List<Payment> findByUserIdAndStatus(String userId, Payment.Status status);
    
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId " +
           "AND p.status = kr.hhplus.be.server.payment.domain.Payment$Status.COMPLETED " +
           "AND p.paidAt BETWEEN :startDate AND :endDate")
    List<Payment> findCompletedPaymentsByUserIdAndPeriod(@Param("userId") String userId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Payment p " +
           "WHERE p.reservationId = :reservationId " +
           "AND p.status IN (kr.hhplus.be.server.payment.domain.Payment$Status.COMPLETED, " +
           "kr.hhplus.be.server.payment.domain.Payment$Status.PENDING)")
    boolean existsActivePaymentByReservationId(@Param("reservationId") Long reservationId);
}