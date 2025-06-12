package kr.hhplus.be.server.infrastructure.persistance.amount;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "amounts", indexes = {
    @Index(name = "idx_user_id", columnList = "userId", unique = true)
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmountEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String userId;
    
    @Column(nullable = false)
    private BigDecimal balance;
    
    @Version
    private Long version;
}