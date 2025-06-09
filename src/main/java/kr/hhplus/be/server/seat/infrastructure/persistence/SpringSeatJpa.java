package kr.hhplus.be.server.seat.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringSeatJpa extends JpaRepository<SeatEntity, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatEntity s WHERE s.scheduleId = :scheduleId AND s.seatNumber = :seatNumber")
    Optional<SeatEntity> findByScheduleIdAndSeatNumberWithLock(@Param("scheduleId") Long scheduleId,
                                                              @Param("seatNumber") Integer seatNumber);
    
    List<SeatEntity> findByScheduleId(Long scheduleId);
    
    @Query("SELECT s FROM SeatEntity s WHERE s.scheduleId = :scheduleId " +
           "AND s.status = kr.hhplus.be.server.seat.domain.model.Seat$Status.AVAILABLE " +
           "ORDER BY s.seatNumber")
    List<SeatEntity> findAvailableSeatsByScheduleId(@Param("scheduleId") Long scheduleId);
    
    @Query("SELECT s FROM SeatEntity s WHERE s.status = kr.hhplus.be.server.seat.domain.model.Seat$Status.TEMPORARY_RESERVED " +
           "AND s.reservedAt < :expirationTime")
    List<SeatEntity> findExpiredTemporaryReservations(@Param("expirationTime") LocalDateTime expirationTime);
}