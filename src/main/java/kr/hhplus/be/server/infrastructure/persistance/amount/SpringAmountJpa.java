package kr.hhplus.be.server.infrastructure.persistance.amount;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringAmountJpa extends JpaRepository<AmountEntity, Long> {
    
    Optional<AmountEntity> findByUserId(String userId);
    
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT a FROM AmountEntity a WHERE a.userId = :userId")
    Optional<AmountEntity> findByUserIdWithLock(@Param("userId") String userId);
}