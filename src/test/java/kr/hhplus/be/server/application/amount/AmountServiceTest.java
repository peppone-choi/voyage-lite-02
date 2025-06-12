package kr.hhplus.be.server.application.amount;

import kr.hhplus.be.server.application.amount.AmountService;
import kr.hhplus.be.server.domain.amount.model.Amount;
import kr.hhplus.be.server.domain.amount.AmountHistory;
import kr.hhplus.be.server.api.dto.amount.AmountResponse;
import kr.hhplus.be.server.domain.amount.AmountHistoryRepository;
import kr.hhplus.be.server.domain.amount.AmountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmountServiceTest {

    @Mock
    private AmountRepository amountRepository;

    @Mock
    private AmountHistoryRepository amountHistoryRepository;

    @InjectMocks
    private AmountService amountService;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = "user123";
    }

    @Test
    @DisplayName("잔액을 충전한다")
    void chargeAmount() {
        // given
        BigDecimal chargeAmount = BigDecimal.valueOf(50000);
        
        Amount existingAmount = Amount.createWithBalance(userId, BigDecimal.valueOf(100000));

        given(amountRepository.findByUserIdWithLock(userId))
                .willReturn(Optional.of(existingAmount));

        given(amountRepository.save(existingAmount))
                .willReturn(existingAmount);

        AmountHistory savedHistory = AmountHistory.builder()
                .id(1L)
                .userId(userId)
                .amount(chargeAmount)
                .type(AmountHistory.Type.CHARGE)
                .balanceAfter(BigDecimal.valueOf(150000))
                .createdAt(LocalDateTime.now())
                .build();

        given(amountHistoryRepository.save(any(AmountHistory.class)))
                .willReturn(savedHistory);

        // when
        AmountResponse response = amountService.charge(userId, chargeAmount);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getBalance()).isEqualTo(BigDecimal.valueOf(150000));
        assertThat(response.getLastChargedAmount()).isEqualTo(chargeAmount);

        verify(amountRepository).save(existingAmount);
        assertThat(existingAmount.getBalance()).isEqualTo(BigDecimal.valueOf(150000));
    }

    @Test
    @DisplayName("첫 충전시 새로운 잔액 계정을 생성한다")
    void createNewAmountOnFirstCharge() {
        // given
        BigDecimal chargeAmount = BigDecimal.valueOf(100000);
        
        given(amountRepository.findByUserIdWithLock(userId))
                .willReturn(Optional.empty());

        given(amountRepository.save(any(Amount.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AmountHistory savedHistory = AmountHistory.builder()
                .id(1L)
                .userId(userId)
                .amount(chargeAmount)
                .type(AmountHistory.Type.CHARGE)
                .balanceAfter(chargeAmount)
                .createdAt(LocalDateTime.now())
                .build();

        given(amountHistoryRepository.save(any(AmountHistory.class)))
                .willReturn(savedHistory);

        // when
        AmountResponse response = amountService.charge(userId, chargeAmount);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getBalance()).isEqualTo(chargeAmount);
        
        verify(amountRepository, times(2)).save(any(Amount.class));
    }

    @Test
    @DisplayName("잔액을 조회한다")
    void getBalance() {
        // given
        Amount amount = Amount.createWithBalance(userId, BigDecimal.valueOf(200000));

        given(amountRepository.findByUserId(userId))
                .willReturn(Optional.of(amount));

        // when
        AmountResponse response = amountService.getBalance(userId);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getBalance()).isEqualTo(BigDecimal.valueOf(200000));
    }

    @Test
    @DisplayName("잔액이 없는 사용자는 0원으로 조회된다")
    void getBalanceForNewUser() {
        // given
        given(amountRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        // when
        AmountResponse response = amountService.getBalance(userId);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getBalance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("잔액을 사용한다")
    void useAmount() {
        // given
        BigDecimal useAmount = BigDecimal.valueOf(30000);
        
        Amount amount = Amount.createWithBalance(userId, BigDecimal.valueOf(100000));

        given(amountRepository.findByUserIdWithLock(userId))
                .willReturn(Optional.of(amount));

        given(amountRepository.save(amount))
                .willReturn(amount);

        AmountHistory savedHistory = AmountHistory.builder()
                .id(1L)
                .userId(userId)
                .amount(useAmount)
                .type(AmountHistory.Type.USE)
                .balanceAfter(BigDecimal.valueOf(70000))
                .createdAt(LocalDateTime.now())
                .build();

        given(amountHistoryRepository.save(any(AmountHistory.class)))
                .willReturn(savedHistory);

        // when
        amountService.use(userId, useAmount);

        // then
        verify(amountRepository).save(amount);
        assertThat(amount.getBalance()).isEqualTo(BigDecimal.valueOf(70000));
    }

    @Test
    @DisplayName("잔액이 부족하면 사용할 수 없다")
    void cannotUseAmountWhenInsufficientBalance() {
        // given
        BigDecimal useAmount = BigDecimal.valueOf(150000);
        
        Amount amount = Amount.createWithBalance(userId, BigDecimal.valueOf(100000));

        given(amountRepository.findByUserIdWithLock(userId))
                .willReturn(Optional.of(amount));

        // when & then
        assertThatThrownBy(() -> amountService.use(userId, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잔액이 부족합니다");
    }

    @Test
    @DisplayName("음수 금액은 충전할 수 없다")
    void cannotChargeNegativeAmount() {
        // given
        BigDecimal negativeAmount = BigDecimal.valueOf(-10000);

        // when & then
        assertThatThrownBy(() -> amountService.charge(userId, negativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("0원은 충전할 수 없다")
    void cannotChargeZeroAmount() {
        // given
        BigDecimal zeroAmount = BigDecimal.ZERO;

        // when & then
        assertThatThrownBy(() -> amountService.charge(userId, zeroAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("최대 충전 한도를 초과할 수 없다")
    void cannotExceedMaxChargeLimit() {
        // given
        BigDecimal exceedAmount = BigDecimal.valueOf(10000001); // 1천만 1원

        // when & then
        assertThatThrownBy(() -> amountService.charge(userId, exceedAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 충전 한도를 초과했습니다");
    }

    @Test
    @DisplayName("동시에 여러 충전 요청이 와도 순차적으로 처리된다")
    void concurrentChargeProcessedSequentially() throws InterruptedException {
        // given
        int threadCount = 10;
        BigDecimal chargeAmount = BigDecimal.valueOf(10000);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        Amount initialAmount = Amount.createWithBalance(userId, BigDecimal.ZERO);

        given(amountRepository.findByUserIdWithLock(userId))
                .willReturn(Optional.of(initialAmount));

        given(amountRepository.save(any(Amount.class)))
                .willAnswer(invocation -> {
                    Amount amount = invocation.getArgument(0);
                    amount.charge(chargeAmount);
                    return amount;
                });

        given(amountHistoryRepository.save(any(AmountHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    amountService.charge(userId, chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 실패 케이스
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(threadCount);
        verify(amountRepository, times(threadCount)).save(any(Amount.class));
        verify(amountHistoryRepository, times(threadCount)).save(any(AmountHistory.class));
    }
}