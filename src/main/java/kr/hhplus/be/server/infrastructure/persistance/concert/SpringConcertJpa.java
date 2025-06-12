package kr.hhplus.be.server.infrastructure.persistance.concert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringConcertJpa extends JpaRepository<ConcertEntity, Long> {
    List<ConcertEntity> findAllByOrderByIdAsc();
}