package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.interfaces.web.dto.ReservationRequest;
import kr.hhplus.be.server.reservation.interfaces.web.dto.ReservationResponse;
import kr.hhplus.be.server.reservation.domain.ReservationRepository;
import kr.hhplus.be.server.schedule.domain.model.Schedule;
import kr.hhplus.be.server.schedule.domain.ScheduleRepository;
import kr.hhplus.be.server.seat.domain.model.Seat;
import kr.hhplus.be.server.seat.domain.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationCreateServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private ReservationCreateService reservationCreateService;

    private String userId;
    private ReservationRequest request;
    private Schedule schedule;
    private Seat seat;

    @BeforeEach
    void setUp() {
        userId = "user123";
        request = ReservationRequest.builder()
                .scheduleId(1L)
                .seatNumber(10)
                .build();

        schedule = Schedule.create(1L, LocalDate.now().plusDays(7), LocalDateTime.now().plusDays(7), 50);

        seat = Seat.create(1L, 10, "VIP", BigDecimal.valueOf(150000));
        seat.assignId(10L);
    }

    @Test
    @DisplayName("좌석을 임시 예약한다")
    void reserveSeatTemporarily() {
        // given
        given(scheduleRepository.findByIdWithLock(1L)).willReturn(Optional.of(schedule));
        given(seatRepository.findByScheduleIdAndSeatNumberWithLock(1L, 10))
                .willReturn(Optional.of(seat));
        given(reservationRepository.existsByUserIdAndScheduleIdAndStatusIn(
                eq(userId), eq(1L), any())).willReturn(false);

        Reservation savedReservation = Reservation.builder()
                .id(1L)
                .userId(userId)
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(LocalDateTime.now())
                .build();

        given(reservationRepository.save(any(Reservation.class))).willReturn(savedReservation);

        // when
        Reservation response = reservationCreateService.reserveSeat(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getSeatId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(Reservation.Status.TEMPORARY_RESERVED);
        assertThat(response.getExpirationTime()).isNotNull();

        verify(seatRepository).save(seat);
        assertThat(seat.getStatus()).isEqualTo(Seat.Status.TEMPORARY_RESERVED);
        assertThat(seat.getReservedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("이미 예약된 좌석은 예약할 수 없다")
    void cannotReserveAlreadyReservedSeat() {
        // given
        seat.temporaryReserve("otherUser");
        
        given(scheduleRepository.findByIdWithLock(1L)).willReturn(Optional.of(schedule));
        given(seatRepository.findByScheduleIdAndSeatNumberWithLock(1L, 10))
                .willReturn(Optional.of(seat));

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약 가능한 좌석이 아닙니다");
    }

    @Test
    @DisplayName("한 유저가 같은 스케줄에 중복 예약할 수 없다")
    void cannotDuplicateReservationForSameSchedule() {
        // given
        given(scheduleRepository.findByIdWithLock(1L)).willReturn(Optional.of(schedule));
        given(reservationRepository.existsByUserIdAndScheduleIdAndStatusIn(
                eq(userId), eq(1L), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 해당 일정에 예약이 있습니다");
    }

    @Test
    @DisplayName("존재하지 않는 스케줄은 예약할 수 없다")
    void cannotReserveNonExistentSchedule() {
        // given
        given(scheduleRepository.findByIdWithLock(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("일정을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("존재하지 않는 좌석은 예약할 수 없다")
    void cannotReserveNonExistentSeat() {
        // given
        given(scheduleRepository.findByIdWithLock(1L)).willReturn(Optional.of(schedule));
        given(seatRepository.findByScheduleIdAndSeatNumberWithLock(1L, 10))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("좌석을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("지난 스케줄은 예약할 수 없다")
    void cannotReservePastSchedule() {
        // given
        Schedule pastSchedule = Schedule.create(1L, LocalDate.now().minusDays(1), LocalDateTime.now().minusDays(1), 50);

        given(scheduleRepository.findByIdWithLock(1L)).willReturn(Optional.of(pastSchedule));

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("지난 일정은 예약할 수 없습니다");
    }

    @Test
    @DisplayName("매진된 스케줄은 예약할 수 없다")
    void cannotReserveSoldOutSchedule() {
        // given
        Schedule soldOutSchedule = Schedule.builder()
                .id(1L)
                .concertId(1L)
                .performanceDate(LocalDate.now().plusDays(7))
                .performanceTime(LocalDateTime.now().plusDays(7))
                .totalSeats(50)
                .availableSeats(0)
                .build();

        given(scheduleRepository.findByIdWithLock(1L)).willReturn(Optional.of(soldOutSchedule));

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("매진된 일정입니다");
    }

    @Test
    @Disabled("동시성 테스트 수정 필요")
    @DisplayName("동시에 여러 사용자가 같은 좌석을 예약하려 할 때 한 명만 성공한다")
    void concurrentReservationOnlySingleSuccess() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        given(scheduleRepository.findByIdWithLock(1L)).willReturn(Optional.of(schedule));
        given(seatRepository.findByScheduleIdAndSeatNumberWithLock(1L, 10))
                .willReturn(Optional.of(seat));
        given(reservationRepository.existsByUserIdAndScheduleIdAndStatusIn(
                any(), eq(1L), any())).willReturn(false);

        // 첫 번째 호출만 성공하도록 설정
        doAnswer(invocation -> {
            Seat seatArg = invocation.getArgument(0);
            if (seatArg.getStatus() == Seat.Status.AVAILABLE) {
                seatArg.temporaryReserve("firstUser");
                return seatArg;
            }
            throw new IllegalStateException("예약 가능한 좌석이 아닙니다");
        }).when(seatRepository).save(any(Seat.class));

        // when
        for (int i = 0; i < threadCount; i++) {
            String testUserId = "user" + i;
            executorService.execute(() -> {
                try {
                    reservationCreateService.reserveSeat(testUserId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);
    }

    @Test
    @Disabled("releaseExpiredReservations 메서드 구현 필요")
    @DisplayName("만료된 임시 예약을 해제한다")
    void releaseExpiredReservations() {
        // given
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(6);
        
        Seat expiredSeat1 = Seat.create(1L, 1, "VIP", BigDecimal.valueOf(150000));
        expiredSeat1.assignId(1L);
        expiredSeat1.temporaryReserve("user1");

        Seat expiredSeat2 = Seat.create(1L, 2, "VIP", BigDecimal.valueOf(150000));
        expiredSeat2.assignId(2L);
        expiredSeat2.temporaryReserve("user2");

        Reservation expiredReservation1 = Reservation.builder()
                .id(1L)
                .userId("user1")
                .scheduleId(1L)
                .seatId(1L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(expiredTime)
                .build();

        Reservation expiredReservation2 = Reservation.builder()
                .id(2L)
                .userId("user2")
                .scheduleId(1L)
                .seatId(2L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(expiredTime)
                .build();

        given(reservationRepository.findExpiredTemporaryReservations(
                eq(Reservation.Status.TEMPORARY_RESERVED), any(LocalDateTime.class)))
                .willReturn(Arrays.asList(expiredReservation1, expiredReservation2));
        given(seatRepository.findById(1L)).willReturn(Optional.of(expiredSeat1));
        given(seatRepository.findById(2L)).willReturn(Optional.of(expiredSeat2));

        // when
        // TODO: releaseExpiredReservations 메서드가 구현되지 않음
        // reservationCreateService.releaseExpiredReservations();

        // then
        // verify(seatRepository, times(2)).save(any(Seat.class));
        // verify(reservationRepository, times(2)).save(any(Reservation.class));
        
        assertThat(expiredReservation1.getStatus()).isEqualTo(Reservation.Status.EXPIRED);
        assertThat(expiredReservation2.getStatus()).isEqualTo(Reservation.Status.EXPIRED);
    }
}