package kr.hhplus.be.server.infrastructure.persistance.user;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String userId;
    
    @Column(nullable = false)
    private String name;
    
    public static UserEntity fromDomain(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .name(user.getName())
                .build();
    }
    
    public User toDomain() {
        return User.builder()
                .id(this.id)
                .userId(this.userId)
                .name(this.name)
                .build();
    }
}