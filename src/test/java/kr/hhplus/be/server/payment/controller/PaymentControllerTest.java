package kr.hhplus.be.server.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.payment.dto.PaymentRequest;
import kr.hhplus.be.server.payment.dto.PaymentResponse;
import kr.hhplus.be.server.payment.service.PaymentService;
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
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private QueueService queueService;

    @Test
    @DisplayName("예약한 좌석을 결제할 수 있다")
    void payForReservation() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        PaymentRequest request = PaymentRequest.builder()
                .reservationId(1L)
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(100L)
                .userId(userId)
                .reservationId(1L)
                .amount(BigDecimal.valueOf(150000))
                .status("COMPLETED")
                .paidAt(LocalDateTime.now())
                .concertTitle("아이유 콘서트")
                .performanceDate(LocalDateTime.now().plusDays(7))
                .seatNumber(10)
                .build();

        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(paymentService.processPayment(eq(userId), eq(1L))).willReturn(response);
        doNothing().when(queueService).expireToken(token);

        // when & then
        mockMvc.perform(post("/api/payments")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(100))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.reservationId").value(1))
                .andExpect(jsonPath("$.amount").value(150000))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("잔액이 부족하면 결제할 수 없다")
    void cannotPayWithInsufficientBalance() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        PaymentRequest request = PaymentRequest.builder()
                .reservationId(1L)
                .build();

        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(paymentService.processPayment(eq(userId), eq(1L)))
                .willThrow(new IllegalStateException("Insufficient balance"));

        // when & then
        mockMvc.perform(post("/api/payments")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 결제된 예약은 다시 결제할 수 없다")
    void cannotPayAlreadyPaidReservation() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        PaymentRequest request = PaymentRequest.builder()
                .reservationId(1L)
                .build();

        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(paymentService.processPayment(eq(userId), eq(1L)))
                .willThrow(new IllegalStateException("Reservation is already paid"));

        // when & then
        mockMvc.perform(post("/api/payments")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("만료된 예약은 결제할 수 없다")
    void cannotPayExpiredReservation() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        PaymentRequest request = PaymentRequest.builder()
                .reservationId(1L)
                .build();

        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(paymentService.processPayment(eq(userId), eq(1L)))
                .willThrow(new IllegalStateException("Reservation is expired"));

        // when & then
        mockMvc.perform(post("/api/payments")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 사용자의 예약은 결제할 수 없다")
    void cannotPayOtherUsersReservation() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        PaymentRequest request = PaymentRequest.builder()
                .reservationId(1L)
                .build();

        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(paymentService.processPayment(eq(userId), eq(1L)))
                .willThrow(new IllegalArgumentException("Reservation does not belong to user"));

        // when & then
        mockMvc.perform(post("/api/payments")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 예약은 결제할 수 없다")
    void cannotPayNonExistentReservation() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        PaymentRequest request = PaymentRequest.builder()
                .reservationId(999L)
                .build();

        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(paymentService.processPayment(eq(userId), eq(999L)))
                .willThrow(new IllegalArgumentException("Reservation not found"));

        // when & then
        mockMvc.perform(post("/api/payments")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("활성화되지 않은 토큰으로는 결제할 수 없다")
    void cannotPayWithInactiveToken() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        
        PaymentRequest request = PaymentRequest.builder()
                .reservationId(1L)
                .build();

        given(queueService.validateAndGetUserId(token))
                .willThrow(new IllegalStateException("Token is not active"));

        // when & then
        mockMvc.perform(post("/api/payments")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}