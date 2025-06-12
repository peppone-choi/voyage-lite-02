package kr.hhplus.be.server.infrastructure.persistance.user;

import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserJpaRepository implements UserRepository {
    
    private final SpringUserJpa springUserJpa;
    
    @Override
    public Optional<User> findById(Long id) {
        return springUserJpa.findById(id)
                .map(UserEntity::toDomain);
    }
    
    @Override
    public Optional<User> findByUserId(String userId) {
        return springUserJpa.findByUserId(userId)
                .map(UserEntity::toDomain);
    }
    
    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.fromDomain(user);
        UserEntity savedEntity = springUserJpa.save(entity);
        return savedEntity.toDomain();
    }
}