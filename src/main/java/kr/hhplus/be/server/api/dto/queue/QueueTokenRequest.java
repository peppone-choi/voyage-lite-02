package kr.hhplus.be.server.api.dto.queue;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QueueTokenRequest {
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
}