package kr.hhplus.be.server.domain.reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.hhplus.be.server.domain.reservation.model.Reservation;
import kr.hhplus.be.server.domain.reservation.model.Reservation.Status;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository  {

    Optional<Reservation> findByIdWithLock(Long id);

    boolean existsByUserIdAndScheduleIdAndStatusIn(String userId, Long scheduleId, List<Status> statuses);

    List<Reservation> findExpiredTemporaryReservations(Reservation.Status status, LocalDateTime expirationTime);

    List<Reservation> findByUserIdAndStatus(String userId, Reservation.Status status);

    Optional<Reservation> findByUserIdAndSeatId(String userId, Long seatId);

    List<Reservation> findActiveReservationsByUserId(String userId);

    Reservation save(Reservation reservation);

    void saveAll(List<Reservation> expiredReservations);
}