package kr.hhplus.be.server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.queue.QueueService;
import kr.hhplus.be.server.api.dto.reservation.ReservationRequest;
import kr.hhplus.be.server.api.dto.reservation.ReservationResponse;
import kr.hhplus.be.server.api.ReservationController;
import kr.hhplus.be.server.application.reservation.ReservationCreateService;
import kr.hhplus.be.server.domain.reservation.model.Reservation;
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
import static org.mockito.Mockito.mock;
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
                .seatId(10L)
                .price(BigDecimal.valueOf(150000))
                .status("TEMPORARY_RESERVED")
                .reservedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        Reservation reservation = mock(Reservation.class);
        given(reservation.getId()).willReturn(1L);
        given(reservation.getUserId()).willReturn(userId);
        given(reservation.getScheduleId()).willReturn(1L);
        given(reservation.getSeatId()).willReturn(10L);
        given(reservation.getStatus()).willReturn(Reservation.Status.TEMPORARY_RESERVED);
        given(reservation.getReservedAt()).willReturn(LocalDateTime.now());
        given(reservation.getExpirationTime()).willReturn(LocalDateTime.now().plusMinutes(5));
        
        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(reservationCreateService.reserveSeat(eq(userId), any(ReservationRequest.class)))
                .willReturn(reservation);

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(1))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.seatId").value(10))
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
                .willThrow(new IllegalStateException("좌석이 이미 예약되었습니다"));

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

        String userId = "user123";
        given(queueService.validateAndGetUserId(token)).willReturn(userId);
        given(reservationCreateService.reserveSeat(eq(userId), any(ReservationRequest.class)))
                .willThrow(new IllegalArgumentException("좌석 번호는 1부터 50 사이여야 합니다"));

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
                .willThrow(new IllegalStateException("토큰이 활성화되지 않음"));

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
                .willThrow(new IllegalStateException("사용자가 이미 해당 스케줄에 예약을 가지고 있습니다"));

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .header("Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}