package kr.hhplus.be.server.amount.repository;

import kr.hhplus.be.server.amount.domain.Amount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AmountRepository extends JpaRepository<Amount, Long> {
    
    Optional<Amount> findByUserId(String userId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Amount a WHERE a.userId = :userId")
    Optional<Amount> findByUserIdWithLock(@Param("userId") String userId);
}