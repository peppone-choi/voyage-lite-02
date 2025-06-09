package kr.hhplus.be.server.seat.repository;

import kr.hhplus.be.server.seat.domain.Seat;
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
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    List<Seat> findByScheduleIdAndStatus(Long scheduleId, Seat.Status status);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.scheduleId = :scheduleId AND s.seatNumber = :seatNumber")
    Optional<Seat> findByScheduleIdAndSeatNumberWithLock(@Param("scheduleId") Long scheduleId, 
                                                        @Param("seatNumber") Integer seatNumber);
    
    @Query("SELECT s FROM Seat s WHERE s.status = :status AND s.reservedAt < :expirationTime")
    List<Seat> findExpiredTemporaryReservations(@Param("status") Seat.Status status, 
                                                @Param("expirationTime") LocalDateTime expirationTime);
    
    @Query("SELECT s FROM Seat s WHERE s.scheduleId = :scheduleId " +
           "AND s.status = kr.hhplus.be.server.seat.domain.Seat$Status.AVAILABLE " +
           "ORDER BY s.seatNumber ASC")
    List<Seat> findAvailableSeatsByScheduleId(@Param("scheduleId") Long scheduleId);
    
    int countByScheduleIdAndStatus(Long scheduleId, Seat.Status status);
}