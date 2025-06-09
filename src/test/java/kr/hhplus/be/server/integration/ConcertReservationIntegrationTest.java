package kr.hhplus.be.server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.amount.dto.AmountChargeRequest;
import kr.hhplus.be.server.payment.dto.PaymentRequest;
import kr.hhplus.be.server.queue.dto.QueueTokenRequest;
import kr.hhplus.be.server.reservation.interfaces.web.dto.ReservationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import kr.hhplus.be.server.TestcontainersConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Disabled("통합 테스트 - 테스트 데이터 설정 필요")
@DisplayName("콘서트 예약 통합 테스트")
class ConcertReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Disabled("통합 테스트 - 테스트 데이터 설정 필요")
    @DisplayName("콘서트 예약 전체 플로우 테스트")
    void concertReservationFullFlow() throws Exception {
        // 1. 대기열 토큰 발급
        String userId = UUID.randomUUID().toString();
        QueueTokenRequest tokenRequest = new QueueTokenRequest(userId);
        
        MvcResult tokenResult = mockMvc.perform(post("/api/queue/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andReturn();

        String token = objectMapper.readTree(tokenResult.getResponse().getContentAsString())
                .get("token").asText();

        // 2. 대기열 상태 확인
        mockMvc.perform(get("/api/queue/status")
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token))
                .andExpect(jsonPath("$.queuePosition").exists());

        // 3. 토큰이 활성화될 때까지 기다림 (테스트에서는 바로 활성화된다고 가정)
        // 실제로는 대기열 활성화 로직이 필요

        // 4. 콘서트 목록 조회
        mockMvc.perform(get("/api/concerts")
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk());

        // 5. 예약 가능 날짜 조회
        Long concertId = 1L;
        mockMvc.perform(get("/api/concerts/{concertId}/available-dates", concertId)
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concertId").value(concertId))
                .andExpect(jsonPath("$.availableDates").isArray());

        // 6. 예약 가능 일정 조회
        mockMvc.perform(get("/api/concerts/{concertId}/schedules", concertId)
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 7. 예약 가능 좌석 조회
        Long scheduleId = 1L;
        mockMvc.perform(get("/api/schedules/{scheduleId}/seats", scheduleId)
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.availableSeats").isArray());

        // 8. 잔액 충전
        AmountChargeRequest chargeRequest = AmountChargeRequest.builder()
                .amount(BigDecimal.valueOf(200000))
                .build();

        mockMvc.perform(post("/api/amounts/charge")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200000));

        // 9. 잔액 조회
        mockMvc.perform(get("/api/amounts/balance")
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200000));

        // 10. 좌석 예약
        ReservationRequest reservationRequest = ReservationRequest.builder()
                .scheduleId(scheduleId)
                .seatNumber(10)
                .build();

        MvcResult reservationResult = mockMvc.perform(post("/api/reservations")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservationRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").exists())
                .andExpect(jsonPath("$.status").value("TEMPORARY_RESERVED"))
                .andReturn();

        Long reservationId = objectMapper.readTree(reservationResult.getResponse().getContentAsString())
                .get("reservationId").asLong();

        // 11. 결제
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .reservationId(reservationId)
                .build();

        mockMvc.perform(post("/api/payments")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").exists());

        // 12. 결제 후 토큰이 만료되었는지 확인
        mockMvc.perform(get("/api/queue/status")
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}