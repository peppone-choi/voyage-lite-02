package kr.hhplus.be.server.amount.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.amount.domain.AmountRepository;
import kr.hhplus.be.server.amount.domain.model.Amount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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