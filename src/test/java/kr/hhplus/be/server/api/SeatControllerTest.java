package kr.hhplus.be.server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.api.SeatController;
import kr.hhplus.be.server.application.queue.QueueService;
import kr.hhplus.be.server.api.dto.seat.SeatResponse;
import kr.hhplus.be.server.application.seat.SeatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeatController.class)
class SeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SeatService seatService;

    @MockBean
    private QueueService queueService;

    @Test
    @DisplayName("특정 날짜의 예약 가능한 좌석 목록을 조회할 수 있다")
    void getAvailableSeats() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        Long scheduleId = 1L;
        LocalDate date = LocalDate.now().plusDays(7);
        
        List<SeatResponse> availableSeats = Arrays.asList(
                SeatResponse.builder()
                        .seatId(1L)
                        .seatNumber(1)
                        .grade("VIP")
                        .price(BigDecimal.valueOf(150000))
                        .status("AVAILABLE")
                        .build(),
                SeatResponse.builder()
                        .seatId(2L)
                        .seatNumber(2)
                        .grade("VIP")
                        .price(BigDecimal.valueOf(150000))
                        .status("AVAILABLE")
                        .build(),
                SeatResponse.builder()
                        .seatId(10L)
                        .seatNumber(10)
                        .grade("R")
                        .price(BigDecimal.valueOf(100000))
                        .status("AVAILABLE")
                        .build()
        );

        given(queueService.validateToken(token)).willReturn(true);
        given(seatService.getAvailableSeats(scheduleId)).willReturn(availableSeats);

        // when & then
        mockMvc.perform(get("/api/schedules/{scheduleId}/seats", scheduleId)
                        .header("Queue-Token", token)
                        .param("date", date.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.date").value(date.toString()))
                .andExpect(jsonPath("$.availableSeats").isArray())
                .andExpect(jsonPath("$.availableSeats.length()").value(3))
                .andExpect(jsonPath("$.availableSeats[0].seatNumber").value(1))
                .andExpect(jsonPath("$.availableSeats[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("좌석 번호 1~50까지만 조회된다")
    void getSeatNumbersInValidRange() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        Long scheduleId = 1L;
        
        List<SeatResponse> seats = Arrays.asList(
                SeatResponse.builder().seatId(1L).seatNumber(1).grade("VIP").price(BigDecimal.valueOf(150000)).status("AVAILABLE").build(),
                SeatResponse.builder().seatId(25L).seatNumber(25).grade("R").price(BigDecimal.valueOf(100000)).status("AVAILABLE").build(),
                SeatResponse.builder().seatId(50L).seatNumber(50).grade("S").price(BigDecimal.valueOf(80000)).status("AVAILABLE").build()
        );

        given(queueService.validateToken(token)).willReturn(true);
        given(seatService.getAvailableSeats(scheduleId)).willReturn(seats);

        // when & then
        mockMvc.perform(get("/api/schedules/{scheduleId}/seats", scheduleId)
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats[0].seatNumber").value(1))
                .andExpect(jsonPath("$.availableSeats[1].seatNumber").value(25))
                .andExpect(jsonPath("$.availableSeats[2].seatNumber").value(50));
    }

    @Test
    @DisplayName("예약된 좌석은 조회되지 않는다")
    void reservedSeatsNotShown() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        Long scheduleId = 1L;
        
        // 10개 좌석 중 3개만 예약 가능
        List<SeatResponse> availableSeats = Arrays.asList(
                SeatResponse.builder().seatId(3L).seatNumber(3).grade("VIP").price(BigDecimal.valueOf(150000)).status("AVAILABLE").build(),
                SeatResponse.builder().seatId(7L).seatNumber(7).grade("R").price(BigDecimal.valueOf(100000)).status("AVAILABLE").build(),
                SeatResponse.builder().seatId(9L).seatNumber(9).grade("R").price(BigDecimal.valueOf(100000)).status("AVAILABLE").build()
        );

        given(queueService.validateToken(token)).willReturn(true);
        given(seatService.getAvailableSeats(scheduleId)).willReturn(availableSeats);

        // when & then
        mockMvc.perform(get("/api/schedules/{scheduleId}/seats", scheduleId)
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats.length()").value(3))
                .andExpect(jsonPath("$.availableSeats[?(@.seatNumber == 3)]").exists())
                .andExpect(jsonPath("$.availableSeats[?(@.seatNumber == 7)]").exists())
                .andExpect(jsonPath("$.availableSeats[?(@.seatNumber == 9)]").exists());
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 좌석 조회시 에러가 발생한다")
    void getSeatsWithInvalidToken() throws Exception {
        // given
        String invalidToken = "invalid-token";
        Long scheduleId = 1L;

        given(queueService.validateToken(invalidToken)).willReturn(false);

        // when & then
        mockMvc.perform(get("/api/schedules/{scheduleId}/seats", scheduleId)
                        .header("Queue-Token", invalidToken))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}