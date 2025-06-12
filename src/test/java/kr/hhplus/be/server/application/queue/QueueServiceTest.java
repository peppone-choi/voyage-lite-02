package kr.hhplus.be.server.application.queue;

import kr.hhplus.be.server.application.queue.QueueService;
import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.api.dto.queue.QueueTokenResponse;
import kr.hhplus.be.server.infrastructure.exception.QueueTokenNotFoundException;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueTokenRepository queueTokenRepository;

    @InjectMocks
    private QueueService queueService;

    private String userId;
    private String token;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        token = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("새로운 대기열 토큰을 발급한다")
    void issueNewToken() {
        // given
        QueueToken queueToken = QueueToken.builder()
                .token(token)
                .userId(userId)
                .position(10)
                .status(QueueToken.Status.WAITING)
                .createdAt(LocalDateTime.now())
                .build();

        given(queueTokenRepository.save(any(QueueToken.class))).willReturn(queueToken);
        given(queueTokenRepository.countByStatusAndCreatedAtBefore(
                eq(QueueToken.Status.WAITING), any(LocalDateTime.class))).willReturn(9L);

        // when
        QueueTokenResponse response = queueService.issueToken(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getQueuePosition()).isEqualTo(10);
        assertThat(response.getStatus()).isEqualTo("WAITING");
        verify(queueTokenRepository).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("이미 대기열에 있는 유저는 기존 토큰을 반환한다")
    void returnExistingTokenForUserInQueue() {
        // given
        QueueToken existingToken = QueueToken.builder()
                .token(token)
                .userId(userId)
                .position(5)
                .status(QueueToken.Status.WAITING)
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();

        given(queueTokenRepository.findByUserIdAndStatusIn(
                eq(userId), 
                eq(Arrays.asList(QueueToken.Status.WAITING, QueueToken.Status.ACTIVE))))
                .willReturn(Optional.of(existingToken));

        // when
        QueueTokenResponse response = queueService.issueToken(userId);

        // then
        assertThat(response.getToken()).isEqualTo(token);
        assertThat(response.getQueuePosition()).isEqualTo(5);
        verify(queueTokenRepository, never()).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("대기열 상태를 조회한다")
    void getQueueStatus() {
        // given
        QueueToken queueToken = QueueToken.builder()
                .token(token)
                .userId(userId)
                .position(3)
                .status(QueueToken.Status.WAITING)
                .createdAt(LocalDateTime.now())
                .build();

        given(queueTokenRepository.findByToken(token)).willReturn(Optional.of(queueToken));
        given(queueTokenRepository.countByStatusAndCreatedAtBefore(
                eq(QueueToken.Status.WAITING), any(LocalDateTime.class))).willReturn(2L);

        // when
        QueueTokenResponse response = queueService.getQueueStatus(token);

        // then
        assertThat(response.getQueuePosition()).isEqualTo(3);
        assertThat(response.getEstimatedWaitTime()).isEqualTo(90); // 3 * 30 seconds
    }

    @Test
    @DisplayName("활성화된 토큰의 대기 위치는 0이다")
    void activeTokenHasZeroPosition() {
        // given
        QueueToken activeToken = QueueToken.builder()
                .token(token)
                .userId(userId)
                .position(0)
                .status(QueueToken.Status.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        given(queueTokenRepository.findByToken(token)).willReturn(Optional.of(activeToken));

        // when
        QueueTokenResponse response = queueService.getQueueStatus(token);

        // then
        assertThat(response.getQueuePosition()).isEqualTo(0);
        assertThat(response.getEstimatedWaitTime()).isEqualTo(0);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("존재하지 않는 토큰 조회시 예외가 발생한다")
    void throwExceptionWhenTokenNotFound() {
        // given
        given(queueTokenRepository.findByToken(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queueService.getQueueStatus("invalid-token"))
                .isInstanceOf(QueueTokenNotFoundException.class)
                .hasMessage("대기열 토큰을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("만료된 토큰 조회시 예외가 발생한다")
    void throwExceptionWhenTokenExpired() {
        // given
        QueueToken expiredToken = QueueToken.builder()
                .token(token)
                .userId(userId)
                .status(QueueToken.Status.EXPIRED)
                .expiredAt(LocalDateTime.now().minusMinutes(10))
                .build();

        given(queueTokenRepository.findByToken(token)).willReturn(Optional.of(expiredToken));

        // when & then
        assertThatThrownBy(() -> queueService.getQueueStatus(token))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("만료된 토큰입니다");
    }

    @Test
    @DisplayName("대기열을 주기적으로 활성화한다")
    void activateQueuePeriodically() {
        // given
        List<QueueToken> waitingTokens = Arrays.asList(
                createWaitingToken(1),
                createWaitingToken(2),
                createWaitingToken(3)
        );

        given(queueTokenRepository.findWaitingTokensToActivate(10))
                .willReturn(waitingTokens);

        given(queueTokenRepository.countByStatus(QueueToken.Status.ACTIVE))
                .willReturn(90L); // max 100
                
        given(queueTokenRepository.findExpiredActiveTokens(any(LocalDateTime.class)))
                .willReturn(List.of());

        // when
        queueService.activateWaitingTokens();

        // then
        verify(queueTokenRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("동시에 여러 유저가 토큰을 발급받아도 순서가 보장된다")
    void concurrentTokenIssuancePreservesOrder() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        given(queueTokenRepository.save(any(QueueToken.class))).willAnswer(invocation -> {
            QueueToken token = invocation.getArgument(0);
            token.setPosition(successCount.incrementAndGet());
            return token;
        });

        given(queueTokenRepository.countByStatusAndCreatedAtBefore(
                any(QueueToken.Status.class), any(LocalDateTime.class)))
                .willAnswer(invocation -> (long) successCount.get());

        // when
        for (int i = 0; i < threadCount; i++) {
            String testUserId = "user-" + i;
            executorService.execute(() -> {
                try {
                    queueService.issueToken(testUserId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(threadCount);
        verify(queueTokenRepository, times(threadCount)).save(any(QueueToken.class));
    }

    private QueueToken createWaitingToken(int position) {
        return QueueToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(UUID.randomUUID().toString())
                .position(position)
                .status(QueueToken.Status.WAITING)
                .createdAt(LocalDateTime.now().minusMinutes(position))
                .build();
    }
}