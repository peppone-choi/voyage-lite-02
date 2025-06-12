package kr.hhplus.be.server.domain.amount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AmountHistoryRepository extends JpaRepository<AmountHistory, Long> {
    
    List<AmountHistory> findByUserIdOrderByCreatedAtDesc(String userId);
    
    @Query("SELECT ah FROM AmountHistory ah WHERE ah.userId = :userId " +
           "AND ah.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ah.createdAt DESC")
    List<AmountHistory> findByUserIdAndCreatedAtBetween(@Param("userId") String userId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query(value = "SELECT * FROM amount_histories WHERE user_id = :userId " +
           "AND type = :#{#type.name()} ORDER BY created_at DESC LIMIT 1", 
           nativeQuery = true)
    Optional<AmountHistory> findLastByUserIdAndType(@Param("userId") String userId,
                                                    @Param("type") AmountHistory.Type type);
}