package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.application.amount.AmountService;
import kr.hhplus.be.server.application.payment.PaymentService;
import kr.hhplus.be.server.domain.concert.Concert;
import kr.hhplus.be.server.domain.concert.ConcertRepository;
import kr.hhplus.be.server.domain.payment.model.Payment;
import kr.hhplus.be.server.api.dto.payment.PaymentResponse;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.reservation.model.Reservation;
import kr.hhplus.be.server.domain.reservation.ReservationRepository;
import kr.hhplus.be.server.domain.schedule.model.Schedule;
import kr.hhplus.be.server.domain.schedule.ScheduleRepository;
import kr.hhplus.be.server.domain.seat.model.Seat;
import kr.hhplus.be.server.domain.seat.SeatRepository;
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

        seat = Seat.create(1L, 10, "VIP", BigDecimal.valueOf(150000));
        seat.assignId(10L);
        seat.temporaryReserve(userId);

        reservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(LocalDateTime.now().minusMinutes(3))
                .build();

        schedule = Schedule.create(1L, LocalDate.now().plusDays(7), LocalDateTime.now().plusDays(7), 50);
        schedule.assignId(1L);

        concert = Concert.builder()
                .id(1L)
                .title("아이유 콘서트")
                .artist("아이유")
                .venue("서울 올림픽공원")
                .description("콘서트 설명")
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
        given(paymentRepository.existsActivePaymentByReservationId(reservationId))
                .willReturn(false);

        given(paymentRepository.save(any(Payment.class)))
                .willAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    if (payment.getId() == null) {
                        payment.assignId(100L);
                    }
                    return payment;
                });
        
        doNothing().when(amountService).use(userId, seat.getPrice());

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
        // Verify that payment.complete() was called during save
        verify(paymentRepository, times(2)).save(any(Payment.class)); // Once for creation, once for completion

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
                .hasMessage("예약을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("다른 사용자의 예약은 결제할 수 없다")
    void cannotPayOtherUsersReservation() {
        // given
        reservation = Reservation.builder()
                .id(reservationId)
                .userId("otherUser")
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(LocalDateTime.now().minusMinutes(3))
                .build();

        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 예약은 사용자의 예약이 아닙니다");
    }

    @Test
    @DisplayName("이미 확정된 예약은 다시 결제할 수 없다")
    void cannotPayAlreadyConfirmedReservation() {
        // given
        reservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.CONFIRMED)
                .reservedAt(LocalDateTime.now().minusMinutes(3))
                .confirmedAt(LocalDateTime.now())
                .paymentId(100L)
                .build();

        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 결제된 예약입니다");
    }

    @Test
    @DisplayName("만료된 예약은 결제할 수 없다")
    void cannotPayExpiredReservation() {
        // given
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(10);
        reservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(expiredTime)
                .build();

        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("만료된 예약입니다");
    }

    @Test
    @DisplayName("취소된 예약은 결제할 수 없다")
    void cannotPayCancelledReservation() {
        // given
        reservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.CANCELLED)
                .reservedAt(LocalDateTime.now().minusMinutes(3))
                .cancelledAt(LocalDateTime.now())
                .build();

        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("취소된 예약입니다");
    }

    @Test
    @DisplayName("잔액이 부족하면 결제할 수 없다")
    void cannotPayWithInsufficientBalance() {
        // given
        given(reservationRepository.findByIdWithLock(reservationId))
                .willReturn(Optional.of(reservation));
        given(seatRepository.findById(10L))
                .willReturn(Optional.of(seat));
        
        // Mock payment repository to return a payment, then throw exception during amount service
        Payment pendingPayment = Payment.create(userId, reservationId, seat.getPrice());
        pendingPayment.assignId(100L);
        
        given(paymentRepository.save(any(Payment.class)))
                .willAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    if (payment.getId() == null) {
                        payment.assignId(100L);
                    }
                    return payment;
                });
        doThrow(new IllegalStateException("잔액이 부족합니다"))
                .when(amountService).use(userId, seat.getPrice());

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잔액이 부족합니다");

        // Payment should be saved once (for creation), then saved again (for failure)
        verify(paymentRepository, times(2)).save(any(Payment.class));
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

        // Mock to return a pending payment first
        Payment pendingPayment = Payment.create(userId, reservationId, seat.getPrice());
        pendingPayment.assignId(100L);

        given(paymentRepository.save(any(Payment.class)))
                .willAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    if (payment.getId() == null) {
                        payment.assignId(100L);
                    }
                    return payment;
                });
        
        // Mock existsActivePaymentByReservationId to return false for first call, true for subsequent calls
        AtomicInteger checkCount = new AtomicInteger(0);
        given(paymentRepository.existsActivePaymentByReservationId(reservationId))
                .willAnswer(invocation -> {
                    int count = checkCount.incrementAndGet();
                    return count > 1; // First call returns false, subsequent calls return true
                });

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

        // Mock payment repository to first return a payment, then throw on second save
        Payment pendingPayment = Payment.create(userId, reservationId, seat.getPrice());
        pendingPayment.assignId(100L);
        
        given(paymentRepository.save(any(Payment.class)))
                .willAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    if (payment.getId() == null) {
                        payment.assignId(100L);
                        return payment;
                    }
                    throw new RuntimeException("Database error");
                });

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(userId, reservationId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        // Verify that reservation and seat were not saved due to the exception
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(seatRepository, never()).save(any(Seat.class));
    }
}