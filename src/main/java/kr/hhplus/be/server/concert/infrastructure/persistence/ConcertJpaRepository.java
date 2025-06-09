package kr.hhplus.be.server.concert.infrastructure.persistence;

import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertJpaRepository implements ConcertRepository {
    
    private final kr.hhplus.be.server.concert.repository.ConcertRepository jpaConcertRepository;
    
    @Override
    public Optional<Concert> findById(Long id) {
        return jpaConcertRepository.findById(id);
    }
}