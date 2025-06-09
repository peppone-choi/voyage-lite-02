package kr.hhplus.be.server.concert.domain;

import java.util.Optional;

public interface ConcertRepository {
    
    Optional<Concert> findById(Long id);
}