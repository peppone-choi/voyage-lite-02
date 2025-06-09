package kr.hhplus.be.server.schedule.repository;

import kr.hhplus.be.server.schedule.domain.Schedule;
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
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    @Query("SELECT s FROM Schedule s WHERE s.concertId = :concertId " +
           "AND s.performanceTime > :now AND s.availableSeats > 0 " +
           "ORDER BY s.performanceDate ASC")
    List<Schedule> findAvailableSchedulesByConcertId(@Param("concertId") Long concertId, 
                                                     @Param("now") LocalDateTime now);
    
    @Query("SELECT DISTINCT s.performanceDate FROM Schedule s " +
           "WHERE s.concertId = :concertId AND s.performanceTime > :now " +
           "AND s.availableSeats > 0 ORDER BY s.performanceDate ASC")
    List<java.time.LocalDate> findAvailableDatesByConcertId(@Param("concertId") Long concertId, 
                                                            @Param("now") LocalDateTime now);
    
    List<Schedule> findByConcertIdAndPerformanceTimeAfter(Long concertId, LocalDateTime now);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Schedule s WHERE s.id = :id")
    Optional<Schedule> findByIdWithLock(@Param("id") Long id);
}