package kr.hhplus.be.server.seat.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatTest {

    @Test
    @DisplayName("좌석을 생성한다")
    void createSeat() {
        // given & when
        Seat seat = Seat.builder()
                .scheduleId(1L)
                .seatNumber(1)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.AVAILABLE)
                .build();

        // then
        assertThat(seat.getScheduleId()).isEqualTo(1L);
        assertThat(seat.getSeatNumber()).isEqualTo(1);
        assertThat(seat.getGrade()).isEqualTo("VIP");
        assertThat(seat.getPrice()).isEqualTo(BigDecimal.valueOf(150000));
        assertThat(seat.getStatus()).isEqualTo(Seat.Status.AVAILABLE);
    }

    @Test
    @DisplayName("좌석 번호는 1~50 범위여야 한다")
    void seatNumberValidation() {
        // given & when & then
        assertThatThrownBy(() -> Seat.builder()
                .scheduleId(1L)
                .seatNumber(0)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.AVAILABLE)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Seat number must be between 1 and 50");

        assertThatThrownBy(() -> Seat.builder()
                .scheduleId(1L)
                .seatNumber(51)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.AVAILABLE)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Seat number must be between 1 and 50");
    }

    @Test
    @DisplayName("좌석을 임시 예약한다")
    void temporaryReserve() {
        // given
        Seat seat = Seat.builder()
                .scheduleId(1L)
                .seatNumber(1)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.AVAILABLE)
                .build();

        String userId = "user123";

        // when
        seat.temporaryReserve(userId);

        // then
        assertThat(seat.getStatus()).isEqualTo(Seat.Status.TEMPORARY_RESERVED);
        assertThat(seat.getReservedBy()).isEqualTo(userId);
        assertThat(seat.getReservedAt()).isNotNull();
        assertThat(seat.getReservedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("이미 예약된 좌석은 임시 예약할 수 없다")
    void cannotReserveAlreadyReservedSeat() {
        // given
        Seat reservedSeat = Seat.builder()
                .scheduleId(1L)
                .seatNumber(1)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.RESERVED)
                .reservedBy("user123")
                .reservedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> reservedSeat.temporaryReserve("user456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Seat is not available for reservation");
    }

    @Test
    @DisplayName("임시 예약을 확정한다")
    void confirmReservation() {
        // given
        Seat temporaryReservedSeat = Seat.builder()
                .scheduleId(1L)
                .seatNumber(1)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.TEMPORARY_RESERVED)
                .reservedBy("user123")
                .reservedAt(LocalDateTime.now())
                .build();

        // when
        temporaryReservedSeat.confirmReservation();

        // then
        assertThat(temporaryReservedSeat.getStatus()).isEqualTo(Seat.Status.RESERVED);
        assertThat(temporaryReservedSeat.getReservedBy()).isEqualTo("user123");
    }

    @Test
    @DisplayName("임시 예약이 아닌 좌석은 확정할 수 없다")
    void cannotConfirmNonTemporaryReservedSeat() {
        // given
        Seat availableSeat = Seat.builder()
                .scheduleId(1L)
                .seatNumber(1)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.AVAILABLE)
                .build();

        // when & then
        assertThatThrownBy(() -> availableSeat.confirmReservation())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Seat is not temporarily reserved");
    }

    @Test
    @DisplayName("임시 예약을 해제한다")
    void releaseTemporaryReservation() {
        // given
        Seat temporaryReservedSeat = Seat.builder()
                .scheduleId(1L)
                .seatNumber(1)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.TEMPORARY_RESERVED)
                .reservedBy("user123")
                .reservedAt(LocalDateTime.now())
                .build();

        // when
        temporaryReservedSeat.releaseReservation();

        // then
        assertThat(temporaryReservedSeat.getStatus()).isEqualTo(Seat.Status.AVAILABLE);
        assertThat(temporaryReservedSeat.getReservedBy()).isNull();
        assertThat(temporaryReservedSeat.getReservedAt()).isNull();
    }

    @Test
    @DisplayName("임시 예약이 만료되었는지 확인한다")
    void isTemporaryReservationExpired() {
        // given
        LocalDateTime now = LocalDateTime.now();
        
        Seat expiredReservation = Seat.builder()
                .scheduleId(1L)
                .seatNumber(1)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.TEMPORARY_RESERVED)
                .reservedBy("user123")
                .reservedAt(now.minusMinutes(6)) // 6분 전
                .build();

        Seat validReservation = Seat.builder()
                .scheduleId(1L)
                .seatNumber(2)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.TEMPORARY_RESERVED)
                .reservedBy("user456")
                .reservedAt(now.minusMinutes(3)) // 3분 전
                .build();

        // when & then
        assertThat(expiredReservation.isTemporaryReservationExpired()).isTrue();
        assertThat(validReservation.isTemporaryReservationExpired()).isFalse();
    }

    @Test
    @DisplayName("좌석이 특정 사용자에 의해 예약되었는지 확인한다")
    void isReservedBy() {
        // given
        Seat seat = Seat.builder()
                .scheduleId(1L)
                .seatNumber(1)
                .grade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status(Seat.Status.TEMPORARY_RESERVED)
                .reservedBy("user123")
                .reservedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThat(seat.isReservedBy("user123")).isTrue();
        assertThat(seat.isReservedBy("user456")).isFalse();
    }

    @Test
    @DisplayName("좌석이 예약 가능한지 확인한다")
    void isAvailable() {
        // given
        Seat availableSeat = Seat.builder()
                .status(Seat.Status.AVAILABLE)
                .build();

        Seat reservedSeat = Seat.builder()
                .status(Seat.Status.RESERVED)
                .build();

        Seat temporaryReservedSeat = Seat.builder()
                .status(Seat.Status.TEMPORARY_RESERVED)
                .build();

        // when & then
        assertThat(availableSeat.isAvailable()).isTrue();
        assertThat(reservedSeat.isAvailable()).isFalse();
        assertThat(temporaryReservedSeat.isAvailable()).isFalse();
    }
}