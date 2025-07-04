package kr.hhplus.be.server.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.hhplus.be.server.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueTokenInterceptor implements HandlerInterceptor {

    private final QueueService queueService;
    
    private static final String QUEUE_TOKEN_HEADER = "Queue-Token";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Skip token validation for certain endpoints
        String requestURI = request.getRequestURI();
        if (shouldSkipTokenValidation(requestURI)) {
            return true;
        }
        
        // Get token from header
        String token = request.getHeader(QUEUE_TOKEN_HEADER);
        if (token == null || token.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Queue token is required");
            return false;
        }
        
        // Validate token for active endpoints
        if (requiresActiveToken(requestURI)) {
            try {
                String userId = queueService.validateAndGetUserId(token);
                request.setAttribute("userId", userId);
            } catch (Exception e) {
                log.warn("Token validation failed: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or inactive token");
                return false;
            }
        }
        
        return true;
    }

    private boolean shouldSkipTokenValidation(String requestURI) {
        return requestURI.equals("/api/queue/token") ||
               requestURI.startsWith("/swagger-ui") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.equals("/api/queue/status") ||
               requestURI.startsWith("/actuator");
    }

    private boolean requiresActiveToken(String requestURI) {
        return requestURI.startsWith("/api/reservations") ||
               requestURI.startsWith("/api/payments");
    }
}