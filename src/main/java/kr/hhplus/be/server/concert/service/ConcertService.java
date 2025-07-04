package kr.hhplus.be.server.concert.service;

import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.dto.ConcertResponse;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public List<ConcertResponse> getAllConcerts() {
        List<Concert> concerts = concertRepository.findAllByOrderByIdAsc();
        
        return concerts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates(Long concertId) {
        // Verify concert exists
        concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("콘서트를 찾을 수 없습니다"));
        
        return scheduleRepository.findAvailableDatesByConcertId(concertId, LocalDateTime.now());
    }

    private ConcertResponse convertToResponse(Concert concert) {
        return ConcertResponse.builder()
                .concertId(concert.getId())
                .title(concert.getTitle())
                .artist(concert.getArtist())
                .venue(concert.getVenue())
                .description(concert.getDescription())
                .build();
    }
}