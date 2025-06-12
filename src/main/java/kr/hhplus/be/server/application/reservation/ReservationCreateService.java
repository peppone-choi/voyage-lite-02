package kr.hhplus.be.server.application.reservation;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import kr.hhplus.be.server.domain.reservation.ReservationRepository;
import kr.hhplus.be.server.domain.reservation.model.Reservation;
import kr.hhplus.be.server.api.dto.reservation.ReservationRequest;
import kr.hhplus.be.server.domain.schedule.model.Schedule;
import kr.hhplus.be.server.domain.schedule.ScheduleRepository;
import kr.hhplus.be.server.domain.seat.model.Seat;
import kr.hhplus.be.server.domain.seat.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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

    /**
     * 만료된 임시 예약을 해제합니다.
     * 5분이 지난 임시 예약을 찾아서 만료 처리하고 좌석을 다시 예약 가능한 상태로 변경합니다.
     */
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(5);
        
        List<Reservation> expiredReservations = reservationRepository
                .findExpiredTemporaryReservations(Reservation.Status.TEMPORARY_RESERVED, expiredTime);
        
        for (Reservation reservation : expiredReservations) {
            // 좌석 예약 해제
            Seat seat = seatRepository.findById(reservation.getSeatId())
                    .orElseThrow(() -> new IllegalStateException("좌석을 찾을 수 없습니다"));
            
            seat.releaseReservation();
            seatRepository.save(seat);
            
            // 예약 상태를 만료로 변경
            reservation.expire();
            reservationRepository.save(reservation);
            
            // 스케줄의 가용 좌석 수 증가
            Schedule schedule = scheduleRepository.findById(reservation.getScheduleId())
                    .orElseThrow(() -> new IllegalStateException("일정을 찾을 수 없습니다"));
            
            schedule.cancelSeatReservation();
            scheduleRepository.save(schedule);
        }
    }
}