package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.interfaces.web.dto.ReservationRequest;
import kr.hhplus.be.server.reservation.interfaces.web.dto.ReservationResponse;
import kr.hhplus.be.server.reservation.domain.ReservationRepository;
import kr.hhplus.be.server.schedule.domain.Schedule;
import kr.hhplus.be.server.schedule.repository.ScheduleRepository;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

        schedule = Schedule.builder()
                .id(1L)
                .concertId(1L)
                .performanceDate(LocalDate.now().plusDays(7))
                .performanceTime(LocalDateTime.now().plusDays(7))
                .totalSeats(50)
                .availableSeats(30)
                .build();

        seat = Seat.builder()
                .id(10L)
                .scheduleId(1L)
                .seatNumber(10)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("좌석을 임시 예약한다")
    void reserveSeatTemporarily() {
        // given
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
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
        ReservationResponse response = reservationCreateService.reserveSeat(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getSeatNumber()).isEqualTo(10);
        assertThat(response.getStatus()).isEqualTo("TEMPORARY_RESERVED");
        assertThat(response.getExpiresAt()).isNotNull();

        verify(seatRepository).save(seat);
        assertThat(seat.getStatus()).isEqualTo(Seat.Status.TEMPORARY_RESERVED);
        assertThat(seat.getReservedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("이미 예약된 좌석은 예약할 수 없다")
    void cannotReserveAlreadyReservedSeat() {
        // given
        seat.temporaryReserve("otherUser");
        
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
        given(seatRepository.findByScheduleIdAndSeatNumberWithLock(1L, 10))
                .willReturn(Optional.of(seat));

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Seat is not available for reservation");
    }

    @Test
    @DisplayName("한 유저가 같은 스케줄에 중복 예약할 수 없다")
    void cannotDuplicateReservationForSameSchedule() {
        // given
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
        given(seatRepository.findByScheduleIdAndSeatNumberWithLock(1L, 10))
                .willReturn(Optional.of(seat));
        given(reservationRepository.existsByUserIdAndScheduleIdAndStatusIn(
                eq(userId), eq(1L), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User already has a reservation for this schedule");
    }

    @Test
    @DisplayName("존재하지 않는 스케줄은 예약할 수 없다")
    void cannotReserveNonExistentSchedule() {
        // given
        given(scheduleRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Schedule not found");
    }

    @Test
    @DisplayName("존재하지 않는 좌석은 예약할 수 없다")
    void cannotReserveNonExistentSeat() {
        // given
        given(scheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
        given(seatRepository.findByScheduleIdAndSeatNumberWithLock(1L, 10))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Seat not found");
    }

    @Test
    @DisplayName("지난 스케줄은 예약할 수 없다")
    void cannotReservePastSchedule() {
        // given
        Schedule pastSchedule = Schedule.builder()
                .id(1L)
                .performanceTime(LocalDateTime.now().minusDays(1))
                .build();

        given(scheduleRepository.findById(1L)).willReturn(Optional.of(pastSchedule));

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot reserve past schedule");
    }

    @Test
    @DisplayName("매진된 스케줄은 예약할 수 없다")
    void cannotReserveSoldOutSchedule() {
        // given
        Schedule soldOutSchedule = Schedule.builder()
                .id(1L)
                .performanceTime(LocalDateTime.now().plusDays(7))
                .availableSeats(0)
                .build();

        given(scheduleRepository.findById(1L)).willReturn(Optional.of(soldOutSchedule));

        // when & then
        assertThatThrownBy(() -> reservationCreateService.reserveSeat(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Schedule is sold out");
    }

    @Test
    @DisplayName("동시에 여러 사용자가 같은 좌석을 예약하려 할 때 한 명만 성공한다")
    void concurrentReservationOnlySingleSuccess() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        given(scheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
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
            throw new IllegalStateException("Seat is not available for reservation");
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
    @DisplayName("만료된 임시 예약을 해제한다")
    void releaseExpiredReservations() {
        // given
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(6);
        
        Seat expiredSeat1 = Seat.builder()
                .id(1L)
                .status(Seat.Status.TEMPORARY_RESERVED)
                .reservedAt(expiredTime)
                .build();

        Seat expiredSeat2 = Seat.builder()
                .id(2L)
                .status(Seat.Status.TEMPORARY_RESERVED)
                .reservedAt(expiredTime)
                .build();

        Reservation expiredReservation1 = Reservation.builder()
                .id(1L)
                .seatId(1L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(expiredTime)
                .build();

        Reservation expiredReservation2 = Reservation.builder()
                .id(2L)
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
        reservationCreateService.releaseExpiredReservations();

        // then
        verify(seatRepository, times(2)).save(any(Seat.class));
        verify(reservationRepository, times(2)).save(any(Reservation.class));
        
        assertThat(expiredReservation1.getStatus()).isEqualTo(Reservation.Status.EXPIRED);
        assertThat(expiredReservation2.getStatus()).isEqualTo(Reservation.Status.EXPIRED);
    }
}