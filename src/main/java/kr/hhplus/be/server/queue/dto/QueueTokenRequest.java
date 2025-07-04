package kr.hhplus.be.server.queue.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QueueTokenRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
}