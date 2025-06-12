package kr.hhplus.be.server.domain.amount;

import kr.hhplus.be.server.domain.amount.model.Amount;

import java.util.Optional;

public interface AmountRepository {
    
    Amount save(Amount amount);
    
    Optional<Amount> findByUserId(String userId);
    
    Optional<Amount> findByUserIdWithLock(String userId);
}