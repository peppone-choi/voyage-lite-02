package kr.hhplus.be.server.user.service;

import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(String userId, String name) {
        return userRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .userId(userId)
                            .name(name)
                            .build();
                    return userRepository.save(newUser);
                });
    }
}