package kr.hhplus.be.server.application.amount;

import kr.hhplus.be.server.domain.amount.model.Amount;
import kr.hhplus.be.server.domain.amount.AmountHistory;
import kr.hhplus.be.server.api.dto.amount.AmountResponse;
import kr.hhplus.be.server.domain.amount.AmountHistoryRepository;
import kr.hhplus.be.server.domain.amount.AmountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmountService {

    private final AmountRepository amountRepository;
    private final AmountHistoryRepository amountHistoryRepository;
    
    private static final BigDecimal MAX_CHARGE_AMOUNT = BigDecimal.valueOf(10000000); // 10 million

    @Transactional
    @Retryable(value = OptimisticLockingFailureException.class, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 100))
    public AmountResponse charge(String userId, BigDecimal chargeAmount) {
        validateChargeAmount(chargeAmount);
        
        // Get or create amount with lock
        Amount amount = amountRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    Amount newAmount = Amount.createWithBalance(userId, BigDecimal.ZERO);
                    return amountRepository.save(newAmount);
                });
        
        // Charge amount
        amount.charge(chargeAmount);
        Amount savedAmount = amountRepository.save(amount);
        
        // Record history
        AmountHistory history = AmountHistory.builder()
                .userId(userId)
                .amount(chargeAmount)
                .type(AmountHistory.Type.CHARGE)
                .balanceAfter(savedAmount.getBalance())
                .createdAt(LocalDateTime.now())
                .description("잔액 충전")
                .build();
        
        AmountHistory savedHistory = amountHistoryRepository.save(history);
        
        log.info("사용자 {}에게 {} 충전, 새 잔액: {}", 
                userId, chargeAmount, savedAmount.getBalance());
        
        return convertToResponse(savedAmount, chargeAmount, savedHistory.getCreatedAt());
    }

    @Transactional
    @Retryable(value = OptimisticLockingFailureException.class, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 100))
    public void use(String userId, BigDecimal useAmount) {
        Amount amount = amountRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 잔액 정보를 찾을 수 없습니다"));
        
        // Use amount
        amount.use(useAmount);
        Amount savedAmount = amountRepository.save(amount);
        
        // Record history
        AmountHistory history = AmountHistory.builder()
                .userId(userId)
                .amount(useAmount)
                .type(AmountHistory.Type.USE)
                .balanceAfter(savedAmount.getBalance())
                .createdAt(LocalDateTime.now())
                .description("예약 결제")
                .build();
        
        amountHistoryRepository.save(history);
        
        log.info("사용자 {}가 {} 사용, 새 잔액: {}", 
                userId, useAmount, savedAmount.getBalance());
    }

    @Transactional(readOnly = true)
    public AmountResponse getBalance(String userId) {
        Optional<Amount> amountOpt = amountRepository.findByUserId(userId);
        
        if (amountOpt.isEmpty()) {
            return AmountResponse.builder()
                    .userId(userId)
                    .balance(BigDecimal.ZERO)
                    .build();
        }
        
        Amount amount = amountOpt.get();
        
        // Get last charge info
        Optional<AmountHistory> lastCharge = amountHistoryRepository
                .findLastByUserIdAndType(userId, AmountHistory.Type.CHARGE);
        
        BigDecimal lastChargedAmount = lastCharge.map(AmountHistory::getAmount).orElse(null);
        LocalDateTime lastChargedAt = lastCharge.map(AmountHistory::getCreatedAt).orElse(null);
        
        return convertToResponse(amount, lastChargedAmount, lastChargedAt);
    }

    private void validateChargeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다");
        }
        
        if (amount.compareTo(MAX_CHARGE_AMOUNT) > 0) {
            throw new IllegalArgumentException("최대 충전 한도를 초과했습니다");
        }
    }

    private AmountResponse convertToResponse(Amount amount, BigDecimal lastChargedAmount, LocalDateTime lastChargedAt) {
        return AmountResponse.builder()
                .userId(amount.getUserId())
                .balance(amount.getBalance())
                .lastChargedAmount(lastChargedAmount)
                .lastChargedAt(lastChargedAt)
                .build();
    }
}