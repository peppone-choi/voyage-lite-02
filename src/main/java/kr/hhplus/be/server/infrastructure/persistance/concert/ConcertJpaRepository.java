package kr.hhplus.be.server.infrastructure.persistance.concert;

import kr.hhplus.be.server.domain.concert.Concert;
import kr.hhplus.be.server.domain.concert.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertJpaRepository implements ConcertRepository {
    
    private final SpringConcertJpa springConcertJpa;
    
    @Override
    public Optional<Concert> findById(Long id) {
        return springConcertJpa.findById(id)
                .map(ConcertEntity::toDomain);
    }
    
    @Override
    public List<Concert> findAllByOrderByIdAsc() {
        return springConcertJpa.findAllByOrderByIdAsc().stream()
                .map(ConcertEntity::toDomain)
                .toList();
    }
    
    @Override
    public List<Concert> saveAll(List<Concert> concerts) {
        List<ConcertEntity> entities = concerts.stream()
                .map(ConcertEntity::fromDomain)
                .toList();
        return springConcertJpa.saveAll(entities).stream()
                .map(ConcertEntity::toDomain)
                .toList();
    }
}