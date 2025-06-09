package kr.hhplus.be.server.seat.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.seat.domain.SeatRepository;
import kr.hhplus.be.server.seat.domain.model.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    @Repository
    interface SpringSeatJpa extends JpaRepository<SeatEntity, Long> {
        
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