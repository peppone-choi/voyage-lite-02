package kr.hhplus.be.server.infrastructure.config;

import kr.hhplus.be.server.infrastructure.interceptor.QueueTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final QueueTokenInterceptor queueTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(queueTokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/queue/token",
                        "/api/queue/status",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}