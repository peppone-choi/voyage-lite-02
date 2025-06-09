package kr.hhplus.be.server.reservation.application;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import kr.hhplus.be.server.reservation.domain.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.interfaces.web.dto.ReservationRequest;
import kr.hhplus.be.server.schedule.domain.Schedule;
import kr.hhplus.be.server.schedule.repository.ScheduleRepository;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import org.springframework.transaction.annotation.Transactional;

public class ReservationCreateService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ScheduleRepository scheduleRepository;

    public ReservationCreateService(ReservationRepository reservationRepository, SeatRepository seatRepository, ScheduleRepository scheduleRepository) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * 좌석 예약을 처리합니다.
     *
     * @param userId 사용자 ID
     * @param request 예약 요청 정보
     * @return 예약 정보
     */
    @Transactional
    public Reservation reserveSeat(String userId, ReservationRequest request) {

        Schedule schedule = scheduleRepository.findByIdWithLock(request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다"));
        
        if (schedule.isPast()) {
            throw new IllegalStateException("지난 일정은 예약할 수 없습니다");
        }
        
        if (schedule.isSoldOut()) {
            throw new IllegalStateException("매진된 일정입니다");
        }

        List<Reservation.Status> activeStatuses = Arrays.asList(
            Reservation.Status.TEMPORARY_RESERVED,
            Reservation.Status.CONFIRMED
        );
        
        boolean hasExistingReservation = reservationRepository
                .existsByUserIdAndScheduleIdAndStatusIn(userId, request.getScheduleId(), activeStatuses);
        
        if (hasExistingReservation) {
            throw new IllegalStateException("이미 해당 일정에 예약이 있습니다");
        }

        Seat seat = seatRepository
                .findByScheduleIdAndSeatNumberWithLock(request.getScheduleId(), request.getSeatNumber())
                .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다"));
        
        if (!seat.isAvailable()) {
            throw new IllegalStateException("예약 가능한 좌석이 아닙니다");
        }

        seat.temporaryReserve(userId);
        seatRepository.save(seat);

        schedule.reserveSeat();
        scheduleRepository.save(schedule);

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .scheduleId(request.getScheduleId())
                .seatId(seat.getId())
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(LocalDateTime.now())
                .build();
        
        return reservationRepository.save(reservation);
    }
}