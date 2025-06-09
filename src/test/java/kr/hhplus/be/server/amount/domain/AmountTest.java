package kr.hhplus.be.server.amount.domain;

import kr.hhplus.be.server.amount.domain.model.Amount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmountTest {

    @Test
    @DisplayName("잔액을 생성한다")
    void createAmount() {
        // given & when
        Amount amount = Amount.createWithBalance("user123", BigDecimal.valueOf(100000));

        // then
        assertThat(amount.getUserId()).isEqualTo("user123");
        assertThat(amount.getBalance()).isEqualTo(BigDecimal.valueOf(100000));
    }

    @Test
    @DisplayName("잔액을 충전한다")
    void charge() {
        // given
        Amount amount = Amount.createWithBalance("user123", BigDecimal.valueOf(50000));

        // when
        amount.charge(BigDecimal.valueOf(30000));

        // then
        assertThat(amount.getBalance()).isEqualTo(BigDecimal.valueOf(80000));
    }

    @Test
    @DisplayName("잔액을 사용한다")
    void use() {
        // given
        Amount amount = Amount.createWithBalance("user123", BigDecimal.valueOf(100000));

        // when
        amount.use(BigDecimal.valueOf(30000));

        // then
        assertThat(amount.getBalance()).isEqualTo(BigDecimal.valueOf(70000));
    }

    @Test
    @DisplayName("잔액이 부족하면 사용할 수 없다")
    void cannotUseWhenInsufficientBalance() {
        // given
        Amount amount = Amount.createWithBalance("user123", BigDecimal.valueOf(50000));

        // when & then
        assertThatThrownBy(() -> amount.use(BigDecimal.valueOf(60000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잔액이 부족합니다");
    }

    @Test
    @DisplayName("음수 금액은 충전할 수 없다")
    void cannotChargeNegativeAmount() {
        // given
        Amount amount = Amount.createWithBalance("user123", BigDecimal.valueOf(50000));

        // when & then
        assertThatThrownBy(() -> amount.charge(BigDecimal.valueOf(-10000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("음수 금액은 사용할 수 없다")
    void cannotUseNegativeAmount() {
        // given
        Amount amount = Amount.createWithBalance("user123", BigDecimal.valueOf(50000));

        // when & then
        assertThatThrownBy(() -> amount.use(BigDecimal.valueOf(-10000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("0원은 충전할 수 없다")
    void cannotChargeZeroAmount() {
        // given
        Amount amount = Amount.createWithBalance("user123", BigDecimal.valueOf(50000));

        // when & then
        assertThatThrownBy(() -> amount.charge(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("0원은 사용할 수 없다")
    void cannotUseZeroAmount() {
        // given
        Amount amount = Amount.createWithBalance("user123", BigDecimal.valueOf(50000));

        // when & then
        assertThatThrownBy(() -> amount.use(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("잔액이 충분한지 확인한다")
    void hasEnoughBalance() {
        // given
        Amount amount = Amount.createWithBalance("user123", BigDecimal.valueOf(100000));

        // when & then
        assertThat(amount.hasEnoughBalance(BigDecimal.valueOf(50000))).isTrue();
        assertThat(amount.hasEnoughBalance(BigDecimal.valueOf(100000))).isTrue();
        assertThat(amount.hasEnoughBalance(BigDecimal.valueOf(100001))).isFalse();
    }

    @Test
    @DisplayName("최대 잔액 한도를 초과할 수 없다")
    void cannotExceedMaxBalance() {
        // given
        Amount amount = Amount.createWithBalance("user123", BigDecimal.valueOf(90000000));

        // when & then
        assertThatThrownBy(() -> amount.charge(BigDecimal.valueOf(20000000))) // 2천만원 추가
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잔액이 최대 한도를 초과합니다");
    }
}