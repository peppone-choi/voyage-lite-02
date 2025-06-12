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
        amount.assignId(savedEntity.getId());
        return amount;
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
                .build();
    }
    
    private Amount toDomainModel(AmountEntity entity) {
        Amount amount = Amount.createWithBalance(entity.getUserId(), entity.getBalance());
        amount.assignId(entity.getId());
        return amount;
    }
}