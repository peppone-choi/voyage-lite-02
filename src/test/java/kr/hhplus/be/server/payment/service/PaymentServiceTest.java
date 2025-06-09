package kr.hhplus.be.server.payment.service;

import kr.hhplus.be.server.amount.service.AmountService;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.dto.PaymentResponse;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
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
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private AmountService amountService;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ConcertRepository concertRepository;

    @InjectMocks
    private PaymentService paymentService;

    private String userId;
    private Long reservationId;
    private Reservation reservation;
    private Seat seat;
    private Schedule schedule;
    private Concert concert;

    @BeforeEach
    void setUp() {
        userId = "user123";
        reservationId = 1L;

        seat = Seat.builder()
                .id(10L)
                .scheduleId(1L)
                .seatNumber(10)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.TEMPORARY_RESERVED)
                .reservedBy(userId)
                .build();

        reservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(LocalDateTime.now().minusMinutes(3))
                .build();

        schedule = Schedule.builder()
                .id(1L)
                .concertId(1L)
                .performanceDate(LocalDate.now().plusDays(7))
                .performanceTime(LocalDateTime.now().plusDays(7))
                .build();

        concert = Concert.builder()
                .id(1L)
                .title("아이유 콘서트")
                .artist("아이유")
                .venue("서울 올림픽공원")
                .build();
    }

    @Test
    @DisplayName("결제를 처리한다")
    void processPayment() {
        // given
        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));
        given(seatRepository.findById(10L))
                .willReturn(Optional.of(seat));
        given(scheduleRepository.findById(1L))
                .willReturn(Optional.of(schedule));
        given(concertRepository.findById(1L))
                .willReturn(Optional.of(concert));

        Payment savedPayment = Payment.builder()
                .id(100L)
                .userId(userId)
                .reservationId(reservationId)
                .amount(seat.getPrice())
                .status(Payment.Status.COMPLETED)
                .paidAt(LocalDateTime.now())
                .build();

        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // when
        PaymentResponse response = paymentService.processPayment(userId, reservationId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(100L);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(150000));
        assertThat(response.getStatus()).isEqualTo("COMPLETED");

        verify(amountService).use(userId, seat.getPrice());
        verify(reservationRepository).save(reservation);
        verify(seatRepository).save(seat);

        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.CONFIRMED);
        assertThat(seat.getStatus()).isEqualTo(Seat.Status.RESERVED);
    }

    @Test
    @DisplayName("존재하지 않는 예약은 결제할 수 없다")
    void cannotPayNonExistentReservation() {
        // given
        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reservation not found");
    }

    @Test
    @DisplayName("다른 사용자의 예약은 결제할 수 없다")
    void cannotPayOtherUsersReservation() {
        // given
        reservation = Reservation.builder()
                .id(reservationId)
                .userId("otherUser")
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .build();

        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reservation does not belong to user");
    }

    @Test
    @DisplayName("이미 확정된 예약은 다시 결제할 수 없다")
    void cannotPayAlreadyConfirmedReservation() {
        // given
        reservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .status(Reservation.Status.CONFIRMED)
                .build();

        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Reservation is already paid");
    }

    @Test
    @DisplayName("만료된 예약은 결제할 수 없다")
    void cannotPayExpiredReservation() {
        // given
        reservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(LocalDateTime.now().minusMinutes(10)) // 10분 전 예약
                .build();

        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Reservation is expired");
    }

    @Test
    @DisplayName("취소된 예약은 결제할 수 없다")
    void cannotPayCancelledReservation() {
        // given
        reservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .status(Reservation.Status.CANCELLED)
                .build();

        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Reservation is cancelled");
    }

    @Test
    @DisplayName("잔액이 부족하면 결제할 수 없다")
    void cannotPayWithInsufficientBalance() {
        // given
        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));
        given(seatRepository.findById(10L))
                .willReturn(Optional.of(seat));

        doThrow(new IllegalStateException("Insufficient balance"))
                .when(amountService).use(userId, seat.getPrice());

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient balance");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    @DisplayName("동시에 같은 예약에 대한 결제 요청시 한 번만 성공한다")
    void concurrentPaymentOnlySingleSuccess() throws InterruptedException {
        // given
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));
        given(seatRepository.findById(10L))
                .willReturn(Optional.of(seat));
        given(scheduleRepository.findById(1L))
                .willReturn(Optional.of(schedule));
        given(concertRepository.findById(1L))
                .willReturn(Optional.of(concert));

        // 첫 번째 호출만 성공
        doAnswer(invocation -> {
            if (reservation.getStatus() == Reservation.Status.TEMPORARY_RESERVED) {
                reservation.confirm(100L);
                return null;
            }
            throw new IllegalStateException("Reservation is already paid");
        }).when(reservationRepository).save(any(Reservation.class));

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    paymentService.processPayment(userId, reservationId);
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
    @DisplayName("결제 실패시 트랜잭션이 롤백된다")
    void rollbackOnPaymentFailure() {
        // given
        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));
        given(seatRepository.findById(10L))
                .willReturn(Optional.of(seat));

        doNothing().when(amountService).use(userId, seat.getPrice());
        
        // 결제 저장 시 예외 발생
        given(paymentRepository.save(any(Payment.class)))
                .willThrow(new RuntimeException("Database error"));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        // 예약과 좌석 상태가 변경되지 않아야 함
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.TEMPORARY_RESERVED);
        assertThat(seat.getStatus()).isEqualTo(Seat.Status.TEMPORARY_RESERVED);
    }
}