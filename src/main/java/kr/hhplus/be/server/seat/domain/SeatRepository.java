package kr.hhplus.be.server.seat.domain;

import kr.hhplus.be.server.seat.domain.model.Seat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository {
    
    Seat save(Seat seat);
    
    void saveAll(List<Seat> seats);
    
    Optional<Seat> findById(Long id);
    
    Optional<Seat> findByScheduleIdAndSeatNumberWithLock(Long scheduleId, Integer seatNumber);
    
    List<Seat> findByScheduleId(Long scheduleId);
    
    List<Seat> findAvailableSeatsByScheduleId(Long scheduleId);
    
    List<Seat> findExpiredTemporaryReservations(LocalDateTime expirationTime);
}