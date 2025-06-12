package kr.hhplus.be.server.infrastructure.persistance.amount;

import kr.hhplus.be.server.domain.amount.AmountRepository;
import kr.hhplus.be.server.domain.amount.model.Amount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AmountJpaRepository implements AmountRepository {
    
    private final SpringAmountJpa springAmountJpa;
    
    @Override
    public Amount save(Amount amount) {
        AmountEntity entity = toEntity(amount);
        AmountEntity savedEntity = springAmountJpa.save(entity);
        
        // 도메인 모델에 저장된 엔티티 정보 반영
        return Amount.builder()
                .id(savedEntity.getId())
                .userId(savedEntity.getUserId())
                .balance(savedEntity.getBalance())
                .version(savedEntity.getVersion())
                .build();
    }
    
    @Override
    public Optional<Amount> findByUserId(String userId) {
        return springAmountJpa.findByUserId(userId)
                .map(this::toDomainModel);
    }
    
    @Override
    public Optional<Amount> findByUserIdWithLock(String userId) {
        return springAmountJpa.findByUserIdWithLock(userId)
                .map(this::toDomainModel);
    }
    
    
    private AmountEntity toEntity(Amount amount) {
        return AmountEntity.builder()
                .id(amount.getId())
                .userId(amount.getUserId())
                .balance(amount.getBalance())
                .version(amount.getVersion())
                .build();
    }
    
    private Amount toDomainModel(AmountEntity entity) {
        return Amount.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .balance(entity.getBalance())
                .version(entity.getVersion())
                .build();
    }
}