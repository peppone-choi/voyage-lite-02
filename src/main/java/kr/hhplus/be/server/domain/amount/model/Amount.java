package kr.hhplus.be.server.domain.amount.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class Amount {
    
    private Long id;
    private final String userId;
    private BigDecimal balance;
    
    private static final BigDecimal MAX_BALANCE = BigDecimal.valueOf(100000000); // 1억원
    
    public static Amount create(String userId) {
        return Amount.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();
    }
    
    public static Amount createWithBalance(String userId, BigDecimal balance) {
        return Amount.builder()
                .userId(userId)
                .balance(balance)
                .build();
    }
    
    public void assignId(Long id) {
        this.id = id;
    }
    
    public void charge(BigDecimal amount) {
        validateChargeAmount(amount);
        BigDecimal newBalance = this.balance.add(amount);
        if (newBalance.compareTo(MAX_BALANCE) > 0) {
            throw new IllegalStateException("잔액이 최대 한도를 초과합니다");
        }
        this.balance = newBalance;
    }
    
    public void use(BigDecimal amount) {
        validateUseAmount(amount);
        if (!hasEnoughBalance(amount)) {
            throw new IllegalStateException("잔액이 부족합니다");
        }
        this.balance = this.balance.subtract(amount);
    }
    
    public boolean hasEnoughBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }
    
    private void validateChargeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다");
        }
    }
    
    private void validateUseAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다");
        }
    }
}