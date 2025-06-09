package kr.hhplus.be.server.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    @DisplayName("결제를 생성한다")
    void createPayment() {
        // given & when
        Payment payment = Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status(Payment.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // then
        assertThat(payment.getUserId()).isEqualTo("user123");
        assertThat(payment.getReservationId()).isEqualTo(1L);
        assertThat(payment.getAmount()).isEqualTo(BigDecimal.valueOf(150000));
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.PENDING);
        assertThat(payment.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("결제를 완료한다")
    void completePayment() {
        // given
        Payment payment = Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status(Payment.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // when
        payment.complete();

        // then
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.COMPLETED);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getPaidAt()).isAfter(payment.getCreatedAt());
    }

    @Test
    @DisplayName("이미 완료된 결제는 다시 완료할 수 없다")
    void cannotCompleteAlreadyCompletedPayment() {
        // given
        Payment completedPayment = Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status(Payment.Status.COMPLETED)
                .createdAt(LocalDateTime.now())
                .paidAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> completedPayment.complete())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment is already completed");
    }

    @Test
    @DisplayName("결제를 실패 처리한다")
    void failPayment() {
        // given
        Payment payment = Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status(Payment.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        String failureReason = "Insufficient balance";

        // when
        payment.fail(failureReason);

        // then
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo(failureReason);
        assertThat(payment.getFailedAt()).isNotNull();
    }

    @Test
    @DisplayName("결제를 취소한다")
    void cancelPayment() {
        // given
        Payment completedPayment = Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status(Payment.Status.COMPLETED)
                .createdAt(LocalDateTime.now())
                .paidAt(LocalDateTime.now())
                .build();

        String cancelReason = "User requested cancellation";

        // when
        completedPayment.cancel(cancelReason);

        // then
        assertThat(completedPayment.getStatus()).isEqualTo(Payment.Status.CANCELLED);
        assertThat(completedPayment.getCancelReason()).isEqualTo(cancelReason);
        assertThat(completedPayment.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("완료되지 않은 결제는 취소할 수 없다")
    void cannotCancelNonCompletedPayment() {
        // given
        Payment pendingPayment = Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status(Payment.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> pendingPayment.cancel("Cancel reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only completed payment can be cancelled");
    }

    @Test
    @DisplayName("이미 취소된 결제는 다시 취소할 수 없다")
    void cannotCancelAlreadyCancelledPayment() {
        // given
        Payment cancelledPayment = Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status(Payment.Status.CANCELLED)
                .createdAt(LocalDateTime.now())
                .cancelledAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> cancelledPayment.cancel("Another cancel"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment is already cancelled");
    }

    @Test
    @DisplayName("결제가 완료되었는지 확인한다")
    void isCompleted() {
        // given
        Payment completedPayment = Payment.builder()
                .status(Payment.Status.COMPLETED)
                .build();

        Payment pendingPayment = Payment.builder()
                .status(Payment.Status.PENDING)
                .build();

        Payment failedPayment = Payment.builder()
                .status(Payment.Status.FAILED)
                .build();

        // when & then
        assertThat(completedPayment.isCompleted()).isTrue();
        assertThat(pendingPayment.isCompleted()).isFalse();
        assertThat(failedPayment.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("결제가 환불 가능한지 확인한다")
    void isRefundable() {
        // given
        LocalDateTime now = LocalDateTime.now();
        
        Payment recentPayment = Payment.builder()
                .status(Payment.Status.COMPLETED)
                .paidAt(now.minusHours(12)) // 12시간 전 결제
                .build();

        Payment oldPayment = Payment.builder()
                .status(Payment.Status.COMPLETED)
                .paidAt(now.minusDays(8)) // 8일 전 결제
                .build();

        Payment cancelledPayment = Payment.builder()
                .status(Payment.Status.CANCELLED)
                .paidAt(now.minusHours(1))
                .build();

        // when & then
        assertThat(recentPayment.isRefundable()).isTrue();
        assertThat(oldPayment.isRefundable()).isFalse(); // 7일 이내만 환불 가능
        assertThat(cancelledPayment.isRefundable()).isFalse();
    }

    @Test
    @DisplayName("음수 금액으로 결제를 생성할 수 없다")
    void cannotCreatePaymentWithNegativeAmount() {
        // when & then
        assertThatThrownBy(() -> Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(-10000))
                .status(Payment.Status.PENDING)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment amount must be positive");
    }

    @Test
    @DisplayName("0원으로 결제를 생성할 수 없다")
    void cannotCreatePaymentWithZeroAmount() {
        // when & then
        assertThatThrownBy(() -> Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.ZERO)
                .status(Payment.Status.PENDING)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment amount must be positive");
    }
}