package kr.hhplus.be.server.schedule.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.schedule.domain.ScheduleRepository;
import kr.hhplus.be.server.schedule.domain.model.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ScheduleJpaRepository implements ScheduleRepository {
    
    private final SpringScheduleJpa springScheduleJpa;
    
    @Override
    public Schedule save(Schedule schedule) {
        ScheduleEntity entity = toEntity(schedule);
        ScheduleEntity savedEntity = springScheduleJpa.save(entity);
        schedule.assignId(savedEntity.getId());
        return schedule;
    }
    
    @Override
    public Optional<Schedule> findById(Long id) {
        return springScheduleJpa.findById(id)
                .map(this::toDomainModel);
    }
    
    @Override
    public Optional<Schedule> findByIdWithLock(Long id) {
        return springScheduleJpa.findByIdWithLock(id)
                .map(this::toDomainModel);
    }
    
    @Override
    public List<Schedule> findAvailableSchedulesByConcertId(Long concertId, LocalDateTime now) {
        return springScheduleJpa.findAvailableSchedulesByConcertId(concertId, now)
                .stream()
                .map(this::toDomainModel)
                .toList();
    }
    
    @Override
    public List<LocalDate> findAvailableDatesByConcertId(Long concertId, LocalDateTime now) {
        return springScheduleJpa.findAvailableDatesByConcertId(concertId, now);
    }
    
    @Override
    public List<Schedule> findByConcertIdAndPerformanceTimeAfter(Long concertId, LocalDateTime now) {
        return springScheduleJpa.findByConcertIdAndPerformanceTimeAfter(concertId, now)
                .stream()
                .map(this::toDomainModel)
                .toList();
    }
    
    @Repository
    interface SpringScheduleJpa extends JpaRepository<ScheduleEntity, Long> {
        
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT s FROM ScheduleEntity s WHERE s.id = :id")
        Optional<ScheduleEntity> findByIdWithLock(@Param("id") Long id);
        
        @Query("SELECT s FROM ScheduleEntity s WHERE s.concertId = :concertId " +
               "AND s.performanceTime > :now AND s.availableSeats > 0 " +
               "ORDER BY s.performanceDate ASC")
        List<ScheduleEntity> findAvailableSchedulesByConcertId(@Param("concertId") Long concertId, 
                                                         @Param("now") LocalDateTime now);
        
        @Query("SELECT DISTINCT s.performanceDate FROM ScheduleEntity s " +
               "WHERE s.concertId = :concertId AND s.performanceTime > :now " +
               "AND s.availableSeats > 0 ORDER BY s.performanceDate ASC")
        List<LocalDate> findAvailableDatesByConcertId(@Param("concertId") Long concertId, 
                                                       @Param("now") LocalDateTime now);
        
        List<ScheduleEntity> findByConcertIdAndPerformanceTimeAfter(Long concertId, LocalDateTime now);
    }
    
    private ScheduleEntity toEntity(Schedule schedule) {
        return ScheduleEntity.builder()
                .id(schedule.getId())
                .concertId(schedule.getConcertId())
                .performanceDate(schedule.getPerformanceDate())
                .performanceTime(schedule.getPerformanceTime())
                .totalSeats(schedule.getTotalSeats())
                .availableSeats(schedule.getAvailableSeats())
                .build();
    }
    
    private Schedule toDomainModel(ScheduleEntity entity) {
        Schedule schedule = Schedule.builder()
                .id(entity.getId())
                .concertId(entity.getConcertId())
                .performanceDate(entity.getPerformanceDate())
                .performanceTime(entity.getPerformanceTime())
                .totalSeats(entity.getTotalSeats())
                .availableSeats(entity.getAvailableSeats())
                .build();
        return schedule;
    }
}