package kr.hhplus.be.server.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * 클린 아키텍처를 위한 도메인 Repository 설정
 * 기존 JPA Repository와 새로운 도메인 Repository가 공존하는 동안 사용
 */
@Configuration
public class DomainConfig {
    
    // 새로운 도메인 Repository들이 @Primary로 우선 사용되도록 설정
    // 점진적으로 기존 코드를 마이그레이션할 수 있음
}