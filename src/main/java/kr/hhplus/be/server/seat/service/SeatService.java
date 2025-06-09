package kr.hhplus.be.server.seat.service;

import kr.hhplus.be.server.schedule.repository.ScheduleRepository;
import kr.hhplus.be.server.seat.domain.model.Seat;
import kr.hhplus.be.server.seat.dto.SeatResponse;
import kr.hhplus.be.server.seat.domain.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public List<SeatResponse> getAvailableSeats(Long scheduleId) {
        // Verify schedule exists
        scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다"));
        
        List<Seat> availableSeats = seatRepository
                .findAvailableSeatsByScheduleId(scheduleId);
        
        return availableSeats.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 60000) // Every minute
    @Transactional
    public void releaseExpiredTemporaryReservations() {
        try {
            LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(5);
            List<Seat> expiredSeats = seatRepository
                    .findExpiredTemporaryReservations(expirationTime);
            
            for (Seat seat : expiredSeats) {
                seat.releaseReservation();
                log.info("Released expired temporary reservation for seat: {} in schedule: {}", 
                        seat.getSeatNumber(), seat.getScheduleId());
                
                // Update available seats count in schedule
                scheduleRepository.findById(seat.getScheduleId())
                        .ifPresent(schedule -> {
                            schedule.cancelSeatReservation();
                            scheduleRepository.save(schedule);
                        });
            }
            
            if (!expiredSeats.isEmpty()) {
                seatRepository.saveAll(expiredSeats);
            }
            
        } catch (Exception e) {
            log.error("Error releasing expired temporary reservations", e);
        }
    }

    private SeatResponse convertToResponse(Seat seat) {
        return SeatResponse.builder()
                .seatId(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .grade(seat.getGrade())
                .price(seat.getPrice())
                .status(seat.getStatus().name())
                .build();
    }
}