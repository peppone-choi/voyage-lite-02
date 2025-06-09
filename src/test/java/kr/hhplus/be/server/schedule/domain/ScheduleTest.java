package kr.hhplus.be.server.schedule.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleTest {

    @Test
    @DisplayName("스케줄을 생성한다")
    void createSchedule() {
        // given
        LocalDateTime performanceTime = LocalDateTime.now().plusDays(7).withHour(19).withMinute(0);

        // when
        Schedule schedule = Schedule.builder()
                .concertId(1L)
                .performanceDate(performanceTime.toLocalDate())
                .performanceTime(performanceTime)
                .totalSeats(50)
                .availableSeats(50)
                .build();

        // then
        assertThat(schedule.getConcertId()).isEqualTo(1L);
        assertThat(schedule.getPerformanceDate()).isEqualTo(performanceTime.toLocalDate());
        assertThat(schedule.getPerformanceTime()).isEqualTo(performanceTime);
        assertThat(schedule.getTotalSeats()).isEqualTo(50);
        assertThat(schedule.getAvailableSeats()).isEqualTo(50);
    }

    @Test
    @DisplayName("좌석이 예약되면 가용 좌석수가 감소한다")
    void decreaseAvailableSeats() {
        // given
        Schedule schedule = Schedule.builder()
                .concertId(1L)
                .performanceDate(LocalDate.now().plusDays(7))
                .performanceTime(LocalDateTime.now().plusDays(7).withHour(19).withMinute(0))
                .totalSeats(50)
                .availableSeats(50)
                .build();

        // when
        schedule.reserveSeat();

        // then
        assertThat(schedule.getAvailableSeats()).isEqualTo(49);
    }

    @Test
    @DisplayName("좌석 예약이 취소되면 가용 좌석수가 증가한다")
    void increaseAvailableSeats() {
        // given
        Schedule schedule = Schedule.builder()
                .concertId(1L)
                .performanceDate(LocalDate.now().plusDays(7))
                .performanceTime(LocalDateTime.now().plusDays(7).withHour(19).withMinute(0))
                .totalSeats(50)
                .availableSeats(45)
                .build();

        // when
        schedule.cancelSeatReservation();

        // then
        assertThat(schedule.getAvailableSeats()).isEqualTo(46);
    }

    @Test
    @DisplayName("가용 좌석이 없으면 매진 상태이다")
    void isSoldOut() {
        // given
        Schedule soldOutSchedule = Schedule.builder()
                .concertId(1L)
                .performanceDate(LocalDate.now().plusDays(7))
                .performanceTime(LocalDateTime.now().plusDays(7).withHour(19).withMinute(0))
                .totalSeats(50)
                .availableSeats(0)
                .build();

        Schedule availableSchedule = Schedule.builder()
                .concertId(1L)
                .performanceDate(LocalDate.now().plusDays(7))
                .performanceTime(LocalDateTime.now().plusDays(7).withHour(19).withMinute(0))
                .totalSeats(50)
                .availableSeats(1)
                .build();

        // when & then
        assertThat(soldOutSchedule.isSoldOut()).isTrue();
        assertThat(availableSchedule.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("매진된 스케줄은 좌석을 예약할 수 없다")
    void cannotReserveSoldOutSchedule() {
        // given
        Schedule soldOutSchedule = Schedule.builder()
                .concertId(1L)
                .performanceDate(LocalDate.now().plusDays(7))
                .performanceTime(LocalDateTime.now().plusDays(7).withHour(19).withMinute(0))
                .totalSeats(50)
                .availableSeats(0)
                .build();

        // when & then
        assertThatThrownBy(() -> soldOutSchedule.reserveSeat())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약 가능한 좌석이 없습니다");
    }

    @Test
    @DisplayName("지난 스케줄인지 확인한다")
    void isPastSchedule() {
        // given
        Schedule pastSchedule = Schedule.builder()
                .performanceTime(LocalDateTime.now().minusDays(1))
                .build();

        Schedule futureSchedule = Schedule.builder()
                .performanceTime(LocalDateTime.now().plusDays(1))
                .build();

        // when & then
        assertThat(pastSchedule.isPast()).isTrue();
        assertThat(futureSchedule.isPast()).isFalse();
    }

    @Test
    @DisplayName("예약 가능한 스케줄인지 확인한다")
    void isAvailableForReservation() {
        // given
        Schedule availableSchedule = Schedule.builder()
                .performanceTime(LocalDateTime.now().plusDays(7))
                .totalSeats(50)
                .availableSeats(10)
                .build();

        Schedule pastSchedule = Schedule.builder()
                .performanceTime(LocalDateTime.now().minusDays(1))
                .totalSeats(50)
                .availableSeats(10)
                .build();

        Schedule soldOutSchedule = Schedule.builder()
                .performanceTime(LocalDateTime.now().plusDays(7))
                .totalSeats(50)
                .availableSeats(0)
                .build();

        // when & then
        assertThat(availableSchedule.isAvailableForReservation()).isTrue();
        assertThat(pastSchedule.isAvailableForReservation()).isFalse();
        assertThat(soldOutSchedule.isAvailableForReservation()).isFalse();
    }
}