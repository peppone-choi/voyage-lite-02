package kr.hhplus.be.server.amount.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "amount_histories", indexes = {
        @Index(name = "idx_user_id_created_at", columnList = "userId,createdAt")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmountHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;
    
    @Column(nullable = false)
    private BigDecimal balanceAfter;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private String description;
    
    public enum Type {
        CHARGE,
        USE,
        REFUND
    }
}