package kr.hhplus.be.server.amount.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "amounts", indexes = {
        @Index(name = "idx_user_id", columnList = "userId", unique = true)
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Amount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String userId;
    
    @Column(nullable = false)
    private BigDecimal balance;
    
    private static final BigDecimal MAX_BALANCE = BigDecimal.valueOf(100000000); // 1억원
    
    public static Amount createWithBalance(String userId, BigDecimal balance) {
        return Amount.builder()
                .userId(userId)
                .balance(balance)
                .build();
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