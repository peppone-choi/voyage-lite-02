package kr.hhplus.be.server.domain.queue;

import kr.hhplus.be.server.domain.queue.QueueToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueTokenTest {

    @Test
    @DisplayName("대기열 토큰을 생성한다")
    void createQueueToken() {
        // given
        String userId = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString();

        // when
        QueueToken queueToken = QueueToken.builder()
                .token(token)
                .userId(userId)
                .position(1)
                .status(QueueToken.Status.WAITING)
                .createdAt(LocalDateTime.now())
                .build();

        // then
        assertThat(queueToken.getToken()).isEqualTo(token);
        assertThat(queueToken.getUserId()).isEqualTo(userId);
        assertThat(queueToken.getPosition()).isEqualTo(1);
        assertThat(queueToken.getStatus()).isEqualTo(QueueToken.Status.WAITING);
        assertThat(queueToken.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("대기 중인 토큰을 활성화한다")
    void activateWaitingToken() {
        // given
        QueueToken queueToken = QueueToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(UUID.randomUUID().toString())
                .position(5)
                .status(QueueToken.Status.WAITING)
                .createdAt(LocalDateTime.now())
                .build();

        // when
        queueToken.activate();

        // then
        assertThat(queueToken.getStatus()).isEqualTo(QueueToken.Status.ACTIVE);
        assertThat(queueToken.getPosition()).isEqualTo(0);
        assertThat(queueToken.getActivatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 활성화된 토큰은 다시 활성화할 수 없다")
    void cannotActivateAlreadyActiveToken() {
        // given
        QueueToken activeToken = QueueToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(UUID.randomUUID().toString())
                .position(0)
                .status(QueueToken.Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .activatedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> activeToken.activate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Token is already active");
    }

    @Test
    @DisplayName("토큰을 만료시킨다")
    void expireToken() {
        // given
        QueueToken activeToken = QueueToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(UUID.randomUUID().toString())
                .position(0)
                .status(QueueToken.Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .activatedAt(LocalDateTime.now())
                .build();

        // when
        activeToken.expire();

        // then
        assertThat(activeToken.getStatus()).isEqualTo(QueueToken.Status.EXPIRED);
        assertThat(activeToken.getExpiredAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 만료된 토큰은 다시 만료시킬 수 없다")
    void cannotExpireAlreadyExpiredToken() {
        // given
        QueueToken expiredToken = QueueToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(UUID.randomUUID().toString())
                .status(QueueToken.Status.EXPIRED)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> expiredToken.expire())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Token is already expired");
    }

    @Test
    @DisplayName("대기 중인 토큰만 만료시킬 수 없다")
    void cannotExpireWaitingToken() {
        // given
        QueueToken waitingToken = QueueToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(UUID.randomUUID().toString())
                .position(5)
                .status(QueueToken.Status.WAITING)
                .createdAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> waitingToken.expire())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot expire waiting token");
    }

    @Test
    @DisplayName("토큰이 활성 상태인지 확인한다")
    void isActive() {
        // given
        QueueToken activeToken = QueueToken.builder()
                .status(QueueToken.Status.ACTIVE)
                .build();

        QueueToken waitingToken = QueueToken.builder()
                .status(QueueToken.Status.WAITING)
                .build();

        // when & then
        assertThat(activeToken.isActive()).isTrue();
        assertThat(waitingToken.isActive()).isFalse();
    }

    @Test
    @DisplayName("토큰이 만료되었는지 확인한다")
    void isExpired() {
        // given
        QueueToken expiredToken = QueueToken.builder()
                .status(QueueToken.Status.EXPIRED)
                .build();

        QueueToken activeToken = QueueToken.builder()
                .status(QueueToken.Status.ACTIVE)
                .build();

        // when & then
        assertThat(expiredToken.isExpired()).isTrue();
        assertThat(activeToken.isExpired()).isFalse();
    }

    @Test
    @DisplayName("활성화된 토큰의 남은 시간을 계산한다")
    void calculateRemainingTime() {
        // given
        LocalDateTime activatedAt = LocalDateTime.now().minusMinutes(3);
        QueueToken activeToken = QueueToken.builder()
                .status(QueueToken.Status.ACTIVE)
                .activatedAt(activatedAt)
                .build();

        // when
        long remainingSeconds = activeToken.getRemainingActiveTimeSeconds();

        // then
        assertThat(remainingSeconds).isBetween(110L, 130L); // 약 2분 남음 (5분 - 3분)
    }
}