package kr.hhplus.be.server.reservation.service;

import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.dto.ReservationRequest;
import kr.hhplus.be.server.reservation.dto.ReservationResponse;
import kr.hhplus.be.server.reservation.repository.ReservationRepository;
import kr.hhplus.be.server.schedule.domain.Schedule;
import kr.hhplus.be.server.schedule.repository.ScheduleRepository;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public ReservationResponse reserveSeat(String userId, ReservationRequest request) {
        // Validate schedule with lock to prevent concurrent updates
        Schedule schedule = scheduleRepository.findByIdWithLock(request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다"));
        
        if (schedule.isPast()) {
            throw new IllegalStateException("지난 일정은 예약할 수 없습니다");
        }
        
        if (schedule.isSoldOut()) {
            throw new IllegalStateException("매진된 일정입니다");
        }
        
        // Check if user already has reservation for this schedule
        List<Reservation.Status> activeStatuses = Arrays.asList(
            Reservation.Status.TEMPORARY_RESERVED,
            Reservation.Status.CONFIRMED
        );
        
        boolean hasExistingReservation = reservationRepository
                .existsByUserIdAndScheduleIdAndStatusIn(userId, request.getScheduleId(), activeStatuses);
        
        if (hasExistingReservation) {
            throw new IllegalStateException("이미 해당 일정에 예약이 있습니다");
        }
        
        // Lock and check seat
        Seat seat = seatRepository
                .findByScheduleIdAndSeatNumberWithLock(request.getScheduleId(), request.getSeatNumber())
                .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다"));
        
        if (!seat.isAvailable()) {
            throw new IllegalStateException("예약 가능한 좌석이 아닙니다");
        }
        
        // Reserve seat
        seat.temporaryReserve(userId);
        seatRepository.save(seat);
        
        // Update schedule available seats
        schedule.reserveSeat();
        scheduleRepository.save(schedule);
        
        // Create reservation
        Reservation reservation = Reservation.builder()
                .userId(userId)
                .scheduleId(request.getScheduleId())
                .seatId(seat.getId())
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(LocalDateTime.now())
                .build();
        
        Reservation savedReservation = reservationRepository.save(reservation);
        
        log.info("Created temporary reservation: {} for user: {}", savedReservation.getId(), userId);
        
        return convertToResponse(savedReservation, seat);
    }

    @Scheduled(fixedDelay = 60000) // Every minute
    @Transactional
    public void releaseExpiredReservations() {
        try {
            LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(5);
            List<Reservation> expiredReservations = reservationRepository
                    .findExpiredTemporaryReservations(Reservation.Status.TEMPORARY_RESERVED, expirationTime);
            
            for (Reservation reservation : expiredReservations) {
                // Release seat
                seatRepository.findById(reservation.getSeatId())
                        .ifPresent(seat -> {
                            seat.releaseReservation();
                            seatRepository.save(seat);
                            
                            // Update schedule
                            scheduleRepository.findById(reservation.getScheduleId())
                                    .ifPresent(schedule -> {
                                        schedule.cancelSeatReservation();
                                        scheduleRepository.save(schedule);
                                    });
                        });
                
                // Expire reservation
                reservation.expire();
                log.info("Expired reservation: {} for user: {}", reservation.getId(), reservation.getUserId());
            }
            
            if (!expiredReservations.isEmpty()) {
                reservationRepository.saveAll(expiredReservations);
            }
            
        } catch (Exception e) {
            log.error("Error releasing expired reservations", e);
        }
    }

    private ReservationResponse convertToResponse(Reservation reservation, Seat seat) {
        return ReservationResponse.builder()
                .reservationId(reservation.getId())
                .userId(reservation.getUserId())
                .scheduleId(reservation.getScheduleId())
                .seatNumber(seat.getSeatNumber())
                .seatGrade(seat.getGrade())
                .price(seat.getPrice())
                .status(reservation.getStatus().name())
                .reservedAt(reservation.getReservedAt())
                .expiresAt(reservation.getExpirationTime())
                .confirmedAt(reservation.getConfirmedAt())
                .build();
    }
}