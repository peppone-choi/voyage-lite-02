package kr.hhplus.be.server.queue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.queue.controller.QueueController;
import kr.hhplus.be.server.queue.dto.QueueTokenRequest;
import kr.hhplus.be.server.queue.dto.QueueTokenResponse;
import kr.hhplus.be.server.queue.service.QueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueueController.class)
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QueueService queueService;

    @Test
    @DisplayName("유저 대기열 토큰을 발급받을 수 있다")
    void issueQueueToken() throws Exception {
        // given
        String userId = UUID.randomUUID().toString();
        QueueTokenRequest request = new QueueTokenRequest(userId);
        
        String token = UUID.randomUUID().toString();
        QueueTokenResponse response = QueueTokenResponse.builder()
                .token(token)
                .userId(userId)
                .queuePosition(10)
                .estimatedWaitTime(300) // 5 minutes in seconds
                .status("WAITING")
                .createdAt(LocalDateTime.now())
                .build();

        given(queueService.issueToken(any(String.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/queue/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.queuePosition").value(10))
                .andExpect(jsonPath("$.estimatedWaitTime").value(300))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("토큰으로 대기열 상태를 조회할 수 있다")
    void getQueueStatus() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        
        QueueTokenResponse response = QueueTokenResponse.builder()
                .token(token)
                .userId(userId)
                .queuePosition(5)
                .estimatedWaitTime(150) // 2.5 minutes
                .status("WAITING")
                .createdAt(LocalDateTime.now())
                .build();

        given(queueService.getQueueStatus(token)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/queue/status")
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token))
                .andExpect(jsonPath("$.queuePosition").value(5))
                .andExpect(jsonPath("$.estimatedWaitTime").value(150))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("활성화된 토큰은 대기 위치가 0이다")
    void getActiveTokenStatus() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        
        QueueTokenResponse response = QueueTokenResponse.builder()
                .token(token)
                .userId(userId)
                .queuePosition(0)
                .estimatedWaitTime(0)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        given(queueService.getQueueStatus(token)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/queue/status")
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queuePosition").value(0))
                .andExpect(jsonPath("$.estimatedWaitTime").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 조회시 에러가 발생한다")
    void getQueueStatusWithInvalidToken() throws Exception {
        // given
        String invalidToken = "invalid-token";
        
        given(queueService.getQueueStatus(invalidToken))
                .willThrow(new IllegalArgumentException("Invalid token"));

        // when & then
        mockMvc.perform(get("/api/queue/status")
                        .header("Queue-Token", invalidToken))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}