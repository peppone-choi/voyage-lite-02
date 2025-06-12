package kr.hhplus.be.server.infrastructure.persistance.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringScheduleJpa extends JpaRepository<ScheduleEntity, Long> {
    
    @Query("SELECT s FROM ScheduleEntity s WHERE s.concertId = :concertId " +
           "AND s.performanceTime > :now AND s.availableSeats > 0 " +
           "ORDER BY s.performanceDate ASC")
    List<ScheduleEntity> findAvailableSchedulesByConcertId(@Param("concertId") Long concertId, 
                                                     @Param("now") LocalDateTime now);
    
    @Query("SELECT DISTINCT s.performanceDate FROM ScheduleEntity s " +
           "WHERE s.concertId = :concertId AND s.performanceTime > :now " +
           "AND s.availableSeats > 0 ORDER BY s.performanceDate ASC")
    List<java.time.LocalDate> findAvailableDatesByConcertId(@Param("concertId") Long concertId, 
                                                            @Param("now") LocalDateTime now);
    
    List<ScheduleEntity> findByConcertIdAndPerformanceTimeAfter(Long concertId, LocalDateTime now);
    
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT s FROM ScheduleEntity s WHERE s.id = :id")
    Optional<ScheduleEntity> findByIdWithLock(@Param("id") Long id);
}