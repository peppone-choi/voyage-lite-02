package kr.hhplus.be.server.infrastructure.persistance.reservation;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.domain.reservation.model.Reservation;
import kr.hhplus.be.server.domain.reservation.model.Reservation.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringReservationJpa extends JpaRepository<ReservationEntity, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservationEntity r WHERE r.id = :id")
    Optional<ReservationEntity> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ReservationEntity r " +
        "WHERE r.userId = :userId AND r.scheduleId = :scheduleId AND r.status IN :statuses")
    boolean existsByUserIdAndScheduleIdAndStatusIn(@Param("userId") String userId,
        @Param("scheduleId") Long scheduleId,
        @Param("statuses") List<Status> statuses);

    @Query("SELECT r FROM ReservationEntity r WHERE r.status = :status AND r.reservedAt < "
        + ":expirationTime")
    List<ReservationEntity> findExpiredTemporaryReservations(@Param("status") Reservation.Status status,
        @Param("expirationTime") LocalDateTime expirationTime);

    List<ReservationEntity> findByUserIdAndStatus(String userId, Reservation.Status status);

    Optional<ReservationEntity> findByUserIdAndSeatId(String userId, Long seatId);

    @Query("SELECT r FROM ReservationEntity r WHERE r.userId = :userId " +
        "AND r.status IN ('TEMPORARY_RESERVED', 'CONFIRMED')")
    List<ReservationEntity> findActiveReservationsByUserId(@Param("userId") String userId);
}