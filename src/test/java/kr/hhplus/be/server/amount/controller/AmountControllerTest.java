package kr.hhplus.be.server.amount.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.amount.dto.AmountChargeRequest;
import kr.hhplus.be.server.amount.dto.AmountResponse;
import kr.hhplus.be.server.amount.service.AmountService;
import kr.hhplus.be.server.queue.service.QueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AmountController.class)
class AmountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AmountService amountService;

    @MockBean
    private QueueService queueService;

    @Test
    @DisplayName("잔액을 충전할 수 있다")
    void chargeAmount() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        AmountChargeRequest request = AmountChargeRequest.builder()
                .amount(BigDecimal.valueOf(50000))
                .build();

        AmountResponse response = AmountResponse.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(150000))
                .lastChargedAmount(BigDecimal.valueOf(50000))
                .lastChargedAt(LocalDateTime.now())
                .build();

        given(queueService.getUserIdFromToken(token)).willReturn(userId);
        given(amountService.charge(eq(userId), any(BigDecimal.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/amounts/charge")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.balance").value(150000))
                .andExpect(jsonPath("$.lastChargedAmount").value(50000));
    }

    @Test
    @DisplayName("잔액을 조회할 수 있다")
    void getBalance() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        AmountResponse response = AmountResponse.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(100000))
                .lastChargedAmount(BigDecimal.valueOf(100000))
                .lastChargedAt(LocalDateTime.now().minusDays(1))
                .build();

        given(queueService.getUserIdFromToken(token)).willReturn(userId);
        given(amountService.getBalance(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/amounts/balance")
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.balance").value(100000));
    }

    @Test
    @DisplayName("음수 금액은 충전할 수 없다")
    void cannotChargeNegativeAmount() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        
        AmountChargeRequest request = AmountChargeRequest.builder()
                .amount(BigDecimal.valueOf(-10000))
                .build();

        given(queueService.getUserIdFromToken(token)).willReturn("user123");
        given(amountService.charge("user123", BigDecimal.valueOf(-10000)))
                .willThrow(new IllegalArgumentException("충전 금액은 0보다 커야 합니다"));

        // when & then
        mockMvc.perform(post("/api/amounts/charge")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("0원은 충전할 수 없다")
    void cannotChargeZeroAmount() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        
        AmountChargeRequest request = AmountChargeRequest.builder()
                .amount(BigDecimal.ZERO)
                .build();

        given(queueService.getUserIdFromToken(token)).willReturn("user123");
        given(amountService.charge("user123", BigDecimal.ZERO))
                .willThrow(new IllegalArgumentException("충전 금액은 0보다 커야 합니다"));

        // when & then
        mockMvc.perform(post("/api/amounts/charge")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("최대 충전 한도를 초과할 수 없다")
    void cannotExceedMaxChargeLimit() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        AmountChargeRequest request = AmountChargeRequest.builder()
                .amount(BigDecimal.valueOf(10000000)) // 1천만원
                .build();

        given(queueService.getUserIdFromToken(token)).willReturn(userId);
        given(amountService.charge(eq(userId), any(BigDecimal.class)))
                .willThrow(new IllegalArgumentException("Amount exceeds maximum charge limit"));

        // when & then
        mockMvc.perform(post("/api/amounts/charge")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로는 잔액 조회를 할 수 없다")
    void cannotGetBalanceWithInvalidToken() throws Exception {
        // given
        String invalidToken = "invalid-token";
        
        given(queueService.getUserIdFromToken(invalidToken))
                .willThrow(new IllegalArgumentException("Invalid token"));

        // when & then
        mockMvc.perform(get("/api/amounts/balance")
                        .header("Queue-Token", invalidToken))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}