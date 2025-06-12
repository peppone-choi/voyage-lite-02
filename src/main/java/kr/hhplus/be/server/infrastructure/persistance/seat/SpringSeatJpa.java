package kr.hhplus.be.server.infrastructure.persistance.seat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringSeatJpa extends JpaRepository<SeatEntity, Long> {
    
    List<SeatEntity> findByScheduleIdAndStatus(Long scheduleId, SeatEntity.Status status);
    
    List<SeatEntity> findByScheduleId(Long scheduleId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatEntity s WHERE s.scheduleId = :scheduleId AND s.seatNumber = :seatNumber")
    Optional<SeatEntity> findByScheduleIdAndSeatNumberWithLock(@Param("scheduleId") Long scheduleId, 
                                                        @Param("seatNumber") Integer seatNumber);
    
    @Query("SELECT s FROM SeatEntity s WHERE s.status = :status AND s.reservedAt < :expirationTime")
    List<SeatEntity> findExpiredTemporaryReservations(@Param("status") SeatEntity.Status status, 
                                                @Param("expirationTime") LocalDateTime expirationTime);
    
    @Query("SELECT s FROM SeatEntity s WHERE s.scheduleId = :scheduleId " +
           "AND s.status = kr.hhplus.be.server.infrastructure.persistance.seat.SeatEntity$Status.AVAILABLE " +
           "ORDER BY s.seatNumber ASC")
    List<SeatEntity> findAvailableSeatsByScheduleId(@Param("scheduleId") Long scheduleId);
    
    int countByScheduleIdAndStatus(Long scheduleId, SeatEntity.Status status);
}