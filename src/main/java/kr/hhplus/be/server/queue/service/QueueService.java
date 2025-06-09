package kr.hhplus.be.server.queue.service;

import kr.hhplus.be.server.queue.domain.QueueToken;
import kr.hhplus.be.server.queue.dto.QueueTokenResponse;
import kr.hhplus.be.server.queue.exception.QueueTokenNotFoundException;
import kr.hhplus.be.server.queue.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueTokenRepository queueTokenRepository;
    
    private static final int MAX_ACTIVE_USERS = 100;
    private static final int ACTIVATION_BATCH_SIZE = 10;
    private static final int ESTIMATED_WAIT_TIME_PER_POSITION = 30; // seconds

    @Transactional
    public QueueTokenResponse issueToken(String userId) {
        // Check if user already has an active token
        List<QueueToken.Status> activeStatuses = Arrays.asList(
            QueueToken.Status.WAITING, 
            QueueToken.Status.ACTIVE
        );
        
        var existingToken = queueTokenRepository.findByUserIdAndStatusIn(userId, activeStatuses);
        if (existingToken.isPresent()) {
            return convertToResponse(existingToken.get());
        }
        
        // Create new token
        QueueToken newToken = QueueToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .status(QueueToken.Status.WAITING)
                .createdAt(LocalDateTime.now())
                .build();
        
        QueueToken savedToken = queueTokenRepository.save(newToken);
        
        // Calculate position
        Long position = calculateQueuePosition(savedToken);
        savedToken.setPosition(position.intValue());
        
        return convertToResponse(savedToken);
    }

    @Transactional(readOnly = true)
    public QueueTokenResponse getQueueStatus(String token) {
        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow(() -> new QueueTokenNotFoundException("대기열 토큰을 찾을 수 없습니다"));
        
        if (queueToken.isExpired()) {
            throw new IllegalStateException("만료된 토큰입니다");
        }
        
        if (queueToken.isWaiting()) {
            // Update position for waiting tokens
            Long currentPosition = calculateQueuePosition(queueToken);
            queueToken.setPosition(currentPosition.intValue());
        }
        
        return convertToResponse(queueToken);
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        try {
            QueueToken queueToken = queueTokenRepository.findByToken(token)
                    .orElse(null);
            return queueToken != null && queueToken.isActive();
        } catch (Exception e) {
            log.error("Error validating token: {}", token, e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public String validateAndGetUserId(String token) {
        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("유효하지 않은 토큰입니다"));
        
        if (!queueToken.isActive()) {
            throw new IllegalStateException("활성화되지 않은 토큰입니다");
        }
        
        if (queueToken.shouldAutoExpire()) {
            throw new IllegalStateException("토큰이 만료되었습니다");
        }
        
        return queueToken.getUserId();
    }

    @Transactional(readOnly = true)
    public String getUserIdFromToken(String token) {
        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다"));
        
        return queueToken.getUserId();
    }

    @Transactional
    public void expireToken(String token) {
        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다"));
        
        queueToken.expire();
        queueTokenRepository.save(queueToken);
        log.info("Token expired: {}", token);
    }

    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    @Transactional
    public void activateWaitingTokens() {
        try {
            // First, expire old active tokens
            expireOldActiveTokens();
            
            // Calculate how many tokens we can activate
            Long activeCount = queueTokenRepository.countByStatus(QueueToken.Status.ACTIVE);
            int slotsAvailable = MAX_ACTIVE_USERS - activeCount.intValue();
            
            if (slotsAvailable <= 0) {
                return;
            }
            
            int tokensToActivate = Math.min(slotsAvailable, ACTIVATION_BATCH_SIZE);
            
            // Get waiting tokens to activate
            List<QueueToken> waitingTokens = queueTokenRepository
                    .findWaitingTokensToActivate(tokensToActivate);
            
            // Activate tokens
            for (QueueToken token : waitingTokens) {
                token.activate();
                log.info("Activated token for user: {}", token.getUserId());
            }
            
            queueTokenRepository.saveAll(waitingTokens);
            
        } catch (Exception e) {
            log.error("Error during token activation", e);
        }
    }

    private void expireOldActiveTokens() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(5);
        List<QueueToken> expiredTokens = queueTokenRepository
                .findExpiredActiveTokens(expirationTime);
        
        for (QueueToken token : expiredTokens) {
            token.expire();
            log.info("Expired old active token: {}", token.getToken());
        }
        
        if (!expiredTokens.isEmpty()) {
            queueTokenRepository.saveAll(expiredTokens);
        }
    }

    private Long calculateQueuePosition(QueueToken token) {
        if (token.isActive()) {
            return 0L;
        }
        
        return queueTokenRepository.countByStatusAndCreatedAtBefore(
            QueueToken.Status.WAITING, 
            token.getCreatedAt()
        ) + 1;
    }

    private QueueTokenResponse convertToResponse(QueueToken token) {
        int estimatedWaitTime = token.isActive() ? 0 : 
                token.getPosition() * ESTIMATED_WAIT_TIME_PER_POSITION;
        
        return QueueTokenResponse.builder()
                .token(token.getToken())
                .userId(token.getUserId())
                .queuePosition(token.getPosition())
                .estimatedWaitTime(estimatedWaitTime)
                .status(token.getStatus().name())
                .createdAt(token.getCreatedAt())
                .activatedAt(token.getActivatedAt())
                .expiredAt(token.getExpiredAt())
                .build();
    }
}