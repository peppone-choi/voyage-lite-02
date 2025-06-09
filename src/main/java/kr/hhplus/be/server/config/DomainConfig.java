package kr.hhplus.be.server.config;

import kr.hhplus.be.server.amount.domain.AmountRepository;
import kr.hhplus.be.server.payment.domain.PaymentRepository;
import kr.hhplus.be.server.seat.domain.SeatRepository;
import kr.hhplus.be.server.schedule.domain.ScheduleRepository;
import kr.hhplus.be.server.concert.domain.ConcertRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 클린 아키텍처를 위한 도메인 Repository 설정
 * 기존 JPA Repository와 새로운 도메인 Repository가 공존하는 동안 사용
 */
@Configuration
public class DomainConfig {
    
    // 새로운 도메인 Repository들이 @Primary로 우선 사용되도록 설정
    // 점진적으로 기존 코드를 마이그레이션할 수 있음
}