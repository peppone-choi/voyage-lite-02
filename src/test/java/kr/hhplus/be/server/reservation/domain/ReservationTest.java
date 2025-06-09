package kr.hhplus.be.server.reservation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    @DisplayName("예약을 생성한다")
    void createReservation() {
        // given & when
        Reservation reservation = Reservation.builder()
                .userId("user123")
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(LocalDateTime.now())
                .build();

        // then
        assertThat(reservation.getUserId()).isEqualTo("user123");
        assertThat(reservation.getScheduleId()).isEqualTo(1L);
        assertThat(reservation.getSeatId()).isEqualTo(10L);
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.TEMPORARY_RESERVED);
        assertThat(reservation.getReservedAt()).isNotNull();
    }

    @Test
    @DisplayName("임시 예약을 확정한다")
    void confirmReservation() {
        // given
        Reservation reservation = Reservation.builder()
                .userId("user123")
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(LocalDateTime.now())
                .build();

        Long paymentId = 100L;

        // when
        reservation.confirm(paymentId);

        // then
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.CONFIRMED);
        assertThat(reservation.getPaymentId()).isEqualTo(paymentId);
        assertThat(reservation.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("임시 예약이 아닌 상태에서는 확정할 수 없다")
    void cannotConfirmNonTemporaryReservation() {
        // given
        Reservation confirmedReservation = Reservation.builder()
                .userId("user123")
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.CONFIRMED)
                .reservedAt(LocalDateTime.now())
                .confirmedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> confirmedReservation.confirm(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only temporary reservations can be confirmed");
    }

    @Test
    @DisplayName("예약을 취소한다")
    void cancelReservation() {
        // given
        Reservation reservation = Reservation.builder()
                .userId("user123")
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.CONFIRMED)
                .reservedAt(LocalDateTime.now())
                .confirmedAt(LocalDateTime.now())
                .build();

        // when
        reservation.cancel();

        // then
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.CANCELLED);
        assertThat(reservation.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 취소된 예약은 다시 취소할 수 없다")
    void cannotCancelAlreadyCancelledReservation() {
        // given
        Reservation cancelledReservation = Reservation.builder()
                .userId("user123")
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.CANCELLED)
                .reservedAt(LocalDateTime.now())
                .cancelledAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> cancelledReservation.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Reservation is already cancelled");
    }

    @Test
    @DisplayName("예약을 만료시킨다")
    void expireReservation() {
        // given
        Reservation reservation = Reservation.builder()
                .userId("user123")
                .scheduleId(1L)
                .seatId(10L)
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        // when
        reservation.expire();

        // then
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.EXPIRED);
        assertThat(reservation.getExpiredAt()).isNotNull();
    }

    @Test
    @DisplayName("임시 예약이 만료되었는지 확인한다")
    void isExpired() {
        // given
        LocalDateTime now = LocalDateTime.now();
        
        Reservation expiredReservation = Reservation.builder()
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(now.minusMinutes(6))
                .build();

        Reservation validReservation = Reservation.builder()
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(now.minusMinutes(3))
                .build();

        Reservation confirmedReservation = Reservation.builder()
                .status(Reservation.Status.CONFIRMED)
                .reservedAt(now.minusMinutes(10))
                .build();

        // when & then
        assertThat(expiredReservation.isExpired()).isTrue();
        assertThat(validReservation.isExpired()).isFalse();
        assertThat(confirmedReservation.isExpired()).isFalse(); // 확정된 예약은 만료되지 않음
    }

    @Test
    @DisplayName("예약 만료 시간을 계산한다")
    void getExpirationTime() {
        // given
        LocalDateTime reservedAt = LocalDateTime.now();
        Reservation reservation = Reservation.builder()
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .reservedAt(reservedAt)
                .build();

        // when
        LocalDateTime expirationTime = reservation.getExpirationTime();

        // then
        assertThat(expirationTime).isEqualTo(reservedAt.plusMinutes(5));
    }

    @Test
    @DisplayName("예약이 활성 상태인지 확인한다")
    void isActive() {
        // given
        Reservation temporaryReservation = Reservation.builder()
                .status(Reservation.Status.TEMPORARY_RESERVED)
                .build();

        Reservation confirmedReservation = Reservation.builder()
                .status(Reservation.Status.CONFIRMED)
                .build();

        Reservation cancelledReservation = Reservation.builder()
                .status(Reservation.Status.CANCELLED)
                .build();

        Reservation expiredReservation = Reservation.builder()
                .status(Reservation.Status.EXPIRED)
                .build();

        // when & then
        assertThat(temporaryReservation.isActive()).isTrue();
        assertThat(confirmedReservation.isActive()).isTrue();
        assertThat(cancelledReservation.isActive()).isFalse();
        assertThat(expiredReservation.isActive()).isFalse();
    }
}