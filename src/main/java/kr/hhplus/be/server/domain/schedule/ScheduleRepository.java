package kr.hhplus.be.server.domain.schedule;

import kr.hhplus.be.server.domain.schedule.model.Schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {
    
    Schedule save(Schedule schedule);
    
    Optional<Schedule> findById(Long id);
    
    Optional<Schedule> findByIdWithLock(Long id);
    
    List<Schedule> findAvailableSchedulesByConcertId(Long concertId, LocalDateTime now);
    
    List<LocalDate> findAvailableDatesByConcertId(Long concertId, LocalDateTime now);
    
    List<Schedule> findByConcertIdAndPerformanceTimeAfter(Long concertId, LocalDateTime now);
}