package kr.hhplus.be.server.infrastructure.persistance.schedule;

import kr.hhplus.be.server.domain.schedule.ScheduleRepository;
import kr.hhplus.be.server.domain.schedule.model.Schedule;
import lombok.RequiredArgsConstructor;
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