package kr.hhplus.be.server.reservation.application;

import java.time.LocalDateTime;
import java.util.List;
import kr.hhplus.be.server.reservation.domain.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.schedule.repository.ScheduleRepository;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

public class ReservationReleaseService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ScheduleRepository scheduleRepository;

    public ReservationReleaseService(ReservationRepository reservationRepository, SeatRepository seatRepository, ScheduleRepository scheduleRepository) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * 주기적으로 만료된 임시 예약을 해제하고 좌석을 반환합니다.
     * 만료된 예약은 5분이 지난 임시 예약입니다.
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(5);
        List<Reservation> expiredReservations = reservationRepository
                    .findExpiredTemporaryReservations(Reservation.Status.TEMPORARY_RESERVED, expirationTime);
            
        for (Reservation reservation : expiredReservations) {
            seatRepository.findById(reservation.getSeatId())
                .ifPresent(seat -> {
                    seat.releaseReservation();

                    seatRepository.save(seat);

                    scheduleRepository.findById(reservation.getScheduleId())
                        .ifPresent(schedule -> {
                            schedule.cancelSeatReservation();
                            scheduleRepository.save(schedule);
                        });
                });

            reservation.expire();
        }
        if (!expiredReservations.isEmpty()) {
            reservationRepository.saveAll(expiredReservations);
        }
    }
}