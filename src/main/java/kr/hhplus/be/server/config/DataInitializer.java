package kr.hhplus.be.server.config;

import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.schedule.domain.Schedule;
import kr.hhplus.be.server.schedule.repository.ScheduleRepository;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile({"local", "test"})
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ConcertRepository concertRepository;
    private final ScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Initializing sample data...");
        
        // Create concerts
        Concert concert1 = Concert.builder()
                .title("아이유 콘서트 - The Golden Hour")
                .artist("아이유")
                .venue("서울 올림픽공원 체조경기장")
                .description("아이유의 정규 6집 발매 기념 콘서트")
                .build();
        
        Concert concert2 = Concert.builder()
                .title("BTS 월드투어 - Yet To Come")
                .artist("BTS")
                .venue("고척스카이돔")
                .description("BTS 10주년 기념 월드투어")
                .build();
        
        Concert concert3 = Concert.builder()
                .title("뉴진스 팬미팅 - Bunnies Camp")
                .artist("뉴진스")
                .venue("잠실실내체육관")
                .description("뉴진스 첫 번째 팬미팅")
                .build();
        
        concertRepository.saveAll(List.of(concert1, concert2, concert3));
        
        // Create schedules and seats
        createSchedulesAndSeats(concert1, 3);
        createSchedulesAndSeats(concert2, 2);
        createSchedulesAndSeats(concert3, 1);
        
        log.info("Sample data initialization completed");
    }

    private void createSchedulesAndSeats(Concert concert, int scheduleCount) {
        LocalDate startDate = LocalDate.now().plusDays(7);
        
        for (int i = 0; i < scheduleCount; i++) {
            LocalDate performanceDate = startDate.plusDays(i * 7);
            LocalDateTime performanceTime = performanceDate.atTime(LocalTime.of(19, 0));
            
            Schedule schedule = Schedule.builder()
                    .concertId(concert.getId())
                    .performanceDate(performanceDate)
                    .performanceTime(performanceTime)
                    .totalSeats(50)
                    .availableSeats(50)
                    .build();
            
            Schedule savedSchedule = scheduleRepository.save(schedule);
            
            // Create seats (1-50)
            List<Seat> seats = new ArrayList<>();
            for (int seatNumber = 1; seatNumber <= 50; seatNumber++) {
                String grade;
                BigDecimal price;
                
                if (seatNumber <= 10) {
                    grade = "VIP";
                    price = BigDecimal.valueOf(150000);
                } else if (seatNumber <= 30) {
                    grade = "R";
                    price = BigDecimal.valueOf(100000);
                } else {
                    grade = "S";
                    price = BigDecimal.valueOf(80000);
                }
                
                Seat seat = Seat.builder()
                        .scheduleId(savedSchedule.getId())
                        .seatNumber(seatNumber)
                        .grade(grade)
                        .price(price)
                        .status(Seat.Status.AVAILABLE)
                        .build();
                
                seats.add(seat);
            }
            
            seatRepository.saveAll(seats);
        }
    }
}