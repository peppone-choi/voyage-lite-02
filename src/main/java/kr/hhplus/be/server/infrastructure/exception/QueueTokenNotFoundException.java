package kr.hhplus.be.server.infrastructure.exception;

public class QueueTokenNotFoundException extends RuntimeException {
    
    public QueueTokenNotFoundException() {
        super("대기열 토큰을 찾을 수 없습니다");
    }
    
    public QueueTokenNotFoundException(String message) {
        super(message);
    }
    
    public QueueTokenNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}