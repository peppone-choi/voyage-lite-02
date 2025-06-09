package kr.hhplus.be.server.amount.domain;

import kr.hhplus.be.server.amount.domain.model.Amount;

import java.util.Optional;

public interface AmountRepository {
    
    Amount save(Amount amount);
    
    Optional<Amount> findByUserId(String userId);
    
    Optional<Amount> findByUserIdWithLock(String userId);
}