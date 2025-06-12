package kr.hhplus.be.server.application.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import kr.hhplus.be.server.api.dto.schedule.ScheduleResponse;
import kr.hhplus.be.server.domain.concert.ConcertRepository;
import kr.hhplus.be.server.domain.schedule.model.Schedule;
import kr.hhplus.be.server.domain.schedule.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ConcertRepository concertRepository;

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAvailableSchedules(Long concertId) {
        // Verify concert exists
        concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("콘서트를 찾을 수 없습니다"));
        
        List<Schedule> schedules = scheduleRepository
                .findAvailableSchedulesByConcertId(concertId, LocalDateTime.now());
        
        return schedules.stream()
                .filter(Schedule::isAvailableForReservation)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private ScheduleResponse convertToResponse(Schedule schedule) {
        return ScheduleResponse.builder()
                .scheduleId(schedule.getId())
                .concertId(schedule.getConcertId())
                .performanceDate(schedule.getPerformanceDate())
                .performanceTime(schedule.getPerformanceTime())
                .availableSeats(schedule.getAvailableSeats())
                .totalSeats(schedule.getTotalSeats())
                .build();
    }
}