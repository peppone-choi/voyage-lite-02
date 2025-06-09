package kr.hhplus.be.server.payment.domain;

import kr.hhplus.be.server.payment.domain.model.Payment;
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
        Payment payment = Payment.create("user123", 1L, BigDecimal.valueOf(150000));

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
        Payment payment = Payment.create("user123", 1L, BigDecimal.valueOf(150000));

        // when
        payment.complete();

        // then
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.COMPLETED);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getPaidAt()).isAfterOrEqualTo(payment.getCreatedAt());
    }

    @Test
    @DisplayName("이미 완료된 결제는 다시 완료할 수 없다")
    void cannotCompleteAlreadyCompletedPayment() {
        // given
        Payment completedPayment = Payment.create("user123", 1L, BigDecimal.valueOf(150000));
        completedPayment.complete();

        // when & then
        assertThatThrownBy(() -> completedPayment.complete())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 완료된 결제입니다");
    }

    @Test
    @DisplayName("결제를 실패 처리한다")
    void failPayment() {
        // given
        Payment payment = Payment.create("user123", 1L, BigDecimal.valueOf(150000));

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
        Payment completedPayment = Payment.create("user123", 1L, BigDecimal.valueOf(150000));
        completedPayment.complete();

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
        Payment pendingPayment = Payment.create("user123", 1L, BigDecimal.valueOf(150000));

        // when & then
        assertThatThrownBy(() -> pendingPayment.cancel("Cancel reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("완료된 결제만 취소할 수 있습니다");
    }

    @Test
    @DisplayName("이미 취소된 결제는 다시 취소할 수 없다")
    void cannotCancelAlreadyCancelledPayment() {
        // given
        Payment cancelledPayment = Payment.create("user123", 1L, BigDecimal.valueOf(150000));
        cancelledPayment.complete();
        cancelledPayment.cancel("Test cancel");

        // when & then
        assertThatThrownBy(() -> cancelledPayment.cancel("Another cancel"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 취소된 결제입니다");
    }

    @Test
    @DisplayName("결제가 완료되었는지 확인한다")
    void isCompleted() {
        // given
        Payment completedPayment = Payment.create("user123", 1L, BigDecimal.valueOf(150000));
        completedPayment.complete();

        Payment pendingPayment = Payment.create("user123", 1L, BigDecimal.valueOf(150000));

        Payment failedPayment = Payment.create("user123", 1L, BigDecimal.valueOf(150000));
        failedPayment.fail("Test failure");

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
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status(Payment.Status.COMPLETED)
                .createdAt(now)
                .paidAt(now.minusHours(12))
                .build();

        Payment oldPayment = Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status(Payment.Status.COMPLETED)
                .createdAt(now)
                .paidAt(now.minusDays(8))
                .build();

        Payment cancelledPayment = Payment.builder()
                .userId("user123")
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status(Payment.Status.CANCELLED)
                .createdAt(now)
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
        assertThatThrownBy(() -> Payment.create("user123", 1L, BigDecimal.valueOf(-10000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("0원으로 결제를 생성할 수 없다")
    void cannotCreatePaymentWithZeroAmount() {
        // when & then
        assertThatThrownBy(() -> Payment.create("user123", 1L, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액은 0보다 커야 합니다");
    }
}