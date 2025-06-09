package kr.hhplus.be.server.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.queue.service.QueueService;
import kr.hhplus.be.server.reservation.interfaces.web.dto.ReservationRequest;
import kr.hhplus.be.server.reservation.interfaces.web.dto.ReservationResponse;
import kr.hhplus.be.server.reservation.interfaces.web.ReservationController;
import kr.hhplus.be.server.reservation.application.ReservationCreateService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationCreateService reservationCreateService;

    @MockBean
    private QueueService queueService;

    @Test
    @DisplayName("좌석 예약을 요청할 수 있다")
    void reserveSeat() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        ReservationRequest request = ReservationRequest.builder()
                .scheduleId(1L)
                .seatNumber(10)
                .build();

        ReservationResponse response = ReservationResponse.builder()
                .reservationId(1L)
                .userId(userId)
                .scheduleId(1L)
                .seatNumber(10)
                .seatGrade("VIP")
                .price(BigDecimal.valueOf(150000))
                .status("TEMPORARY_RESERVED")
                .reservedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(reservationCreateService.reserveSeat(eq(userId), any(ReservationRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(1))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.seatNumber").value(10))
                .andExpect(jsonPath("$.status").value("TEMPORARY_RESERVED"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("이미 예약된 좌석은 예약할 수 없다")
    void cannotReserveAlreadyReservedSeat() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        ReservationRequest request = ReservationRequest.builder()
                .scheduleId(1L)
                .seatNumber(10)
                .build();

        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(reservationCreateService.reserveSeat(eq(userId), any(ReservationRequest.class)))
                .willThrow(new IllegalStateException("Seat is already reserved"));

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 좌석 번호로 예약 요청시 에러가 발생한다")
    void reserveInvalidSeatNumber() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        
        ReservationRequest request = ReservationRequest.builder()
                .scheduleId(1L)
                .seatNumber(51) // 1~50 범위 초과
                .build();

        given(queueService.validateAndGetUserId(token)).willReturn("user123");

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("활성화되지 않은 토큰으로 예약 요청시 에러가 발생한다")
    void cannotReserveWithInactiveToken() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        
        ReservationRequest request = ReservationRequest.builder()
                .scheduleId(1L)
                .seatNumber(10)
                .build();

        given(queueService.validateAndGetUserId(token))
                .willThrow(new IllegalStateException("Token is not active"));

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("한 유저가 같은 스케줄에 중복 예약할 수 없다")
    void cannotDuplicateReservationForSameSchedule() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        String userId = "user123";
        
        ReservationRequest request = ReservationRequest.builder()
                .scheduleId(1L)
                .seatNumber(20)
                .build();

        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(reservationCreateService.reserveSeat(eq(userId), any(ReservationRequest.class)))
                .willThrow(new IllegalStateException("User already has a reservation for this schedule"));

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}