package kr.hhplus.be.server.infrastructure.persistance.seat;

import kr.hhplus.be.server.domain.seat.SeatRepository;
import kr.hhplus.be.server.domain.seat.model.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SeatJpaRepository implements SeatRepository {
    
    private final SpringSeatJpa springSeatJpa;
    
    @Override
    public Seat save(Seat seat) {
        SeatEntity entity = SeatEntity.fromDomain(seat);
        SeatEntity savedEntity = springSeatJpa.save(entity);
        return savedEntity.toDomain();
    }
    
    @Override
    public void saveAll(List<Seat> seats) {
        List<SeatEntity> entities = seats.stream()
                .map(SeatEntity::fromDomain)
                .toList();
        springSeatJpa.saveAll(entities);
    }
    
    @Override
    public Optional<Seat> findById(Long id) {
        return springSeatJpa.findById(id)
                .map(SeatEntity::toDomain);
    }
    
    @Override
    public Optional<Seat> findByScheduleIdAndSeatNumberWithLock(Long scheduleId, Integer seatNumber) {
        return springSeatJpa.findByScheduleIdAndSeatNumberWithLock(scheduleId, seatNumber)
                .map(SeatEntity::toDomain);
    }
    
    @Override
    public Optional<Seat> findByScheduleIdAndSeatNumber(Long scheduleId, Integer seatNumber) {
        return springSeatJpa.findByScheduleIdAndSeatNumber(scheduleId, seatNumber)
                .map(SeatEntity::toDomain);
    }
    
    @Override
    public List<Seat> findByScheduleId(Long scheduleId) {
        return springSeatJpa.findByScheduleIdAndStatus(scheduleId, SeatEntity.Status.AVAILABLE)
                .stream()
                .map(SeatEntity::toDomain)
                .toList();
    }
    
    @Override
    public List<Seat> findAvailableSeatsByScheduleId(Long scheduleId) {
        return springSeatJpa.findAvailableSeatsByScheduleId(scheduleId)
                .stream()
                .map(SeatEntity::toDomain)
                .toList();
    }
    
    @Override
    public List<Seat> findExpiredTemporaryReservations(LocalDateTime expirationTime) {
        return springSeatJpa.findExpiredTemporaryReservations(SeatEntity.Status.TEMPORARY_RESERVED, expirationTime)
                .stream()
                .map(SeatEntity::toDomain)
                .toList();
    }
}