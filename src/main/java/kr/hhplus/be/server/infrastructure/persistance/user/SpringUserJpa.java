package kr.hhplus.be.server.infrastructure.persistance.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringUserJpa extends JpaRepository<UserEntity, Long> {
    
    Optional<UserEntity> findByUserId(String userId);
}