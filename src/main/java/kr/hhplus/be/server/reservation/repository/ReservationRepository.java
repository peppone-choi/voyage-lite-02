package kr.hhplus.be.server.reservation.repository;

import kr.hhplus.be.server.reservation.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdWithLock(@Param("id") Long id);
    
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reservation r " +
           "WHERE r.userId = :userId AND r.scheduleId = :scheduleId AND r.status IN :statuses")
    boolean existsByUserIdAndScheduleIdAndStatusIn(@Param("userId") String userId, 
                                                   @Param("scheduleId") Long scheduleId,
                                                   @Param("statuses") List<Reservation.Status> statuses);
    
    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.reservedAt < :expirationTime")
    List<Reservation> findExpiredTemporaryReservations(@Param("status") Reservation.Status status,
                                                       @Param("expirationTime") LocalDateTime expirationTime);
    
    List<Reservation> findByUserIdAndStatus(String userId, Reservation.Status status);
    
    Optional<Reservation> findByUserIdAndSeatId(String userId, Long seatId);
    
    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId " +
           "AND r.status IN (kr.hhplus.be.server.reservation.domain.Reservation$Status.TEMPORARY_RESERVED, " +
           "kr.hhplus.be.server.reservation.domain.Reservation$Status.CONFIRMED) " +
           "ORDER BY r.reservedAt DESC")
    List<Reservation> findActiveReservationsByUserId(@Param("userId") String userId);
}