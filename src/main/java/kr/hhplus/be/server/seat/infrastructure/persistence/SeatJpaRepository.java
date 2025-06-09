package kr.hhplus.be.server.seat.infrastructure.persistence;

import kr.hhplus.be.server.seat.domain.SeatRepository;
import kr.hhplus.be.server.seat.domain.model.Seat;
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
        SeatEntity entity = toEntity(seat);
        SeatEntity savedEntity = springSeatJpa.save(entity);
        seat.assignId(savedEntity.getId());
        return seat;
    }
    
    @Override
    public void saveAll(List<Seat> seats) {
        List<SeatEntity> entities = seats.stream()
                .map(this::toEntity)
                .toList();
        List<SeatEntity> savedEntities = springSeatJpa.saveAll(entities);
        
        // Assign IDs back to domain models
        for (int i = 0; i < seats.size(); i++) {
            seats.get(i).assignId(savedEntities.get(i).getId());
        }
    }
    
    @Override
    public Optional<Seat> findById(Long id) {
        return springSeatJpa.findById(id)
                .map(this::toDomainModel);
    }
    
    @Override
    public Optional<Seat> findByScheduleIdAndSeatNumberWithLock(Long scheduleId, Integer seatNumber) {
        return springSeatJpa.findByScheduleIdAndSeatNumberWithLock(scheduleId, seatNumber)
                .map(this::toDomainModel);
    }
    
    @Override
    public List<Seat> findByScheduleId(Long scheduleId) {
        return springSeatJpa.findByScheduleId(scheduleId)
                .stream()
                .map(this::toDomainModel)
                .toList();
    }
    
    @Override
    public List<Seat> findAvailableSeatsByScheduleId(Long scheduleId) {
        return springSeatJpa.findAvailableSeatsByScheduleId(scheduleId)
                .stream()
                .map(this::toDomainModel)
                .toList();
    }
    
    @Override
    public List<Seat> findExpiredTemporaryReservations(LocalDateTime expirationTime) {
        return springSeatJpa.findExpiredTemporaryReservations(expirationTime)
                .stream()
                .map(this::toDomainModel)
                .toList();
    }
    
    
    private SeatEntity toEntity(Seat seat) {
        return SeatEntity.builder()
                .id(seat.getId())
                .scheduleId(seat.getScheduleId())
                .seatNumber(seat.getSeatNumber())
                .grade(seat.getGrade())
                .price(seat.getPrice())
                .status(seat.getStatus())
                .reservedBy(seat.getReservedBy())
                .reservedAt(seat.getReservedAt())
                .build();
    }
    
    private Seat toDomainModel(SeatEntity entity) {
        return Seat.builder()
                .id(entity.getId())
                .scheduleId(entity.getScheduleId())
                .seatNumber(entity.getSeatNumber())
                .grade(entity.getGrade())
                .price(entity.getPrice())
                .status(entity.getStatus())
                .reservedBy(entity.getReservedBy())
                .reservedAt(entity.getReservedAt())
                .build();
    }
}