package kr.hhplus.be.server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.api.ScheduleController;
import kr.hhplus.be.server.application.queue.QueueService;
import kr.hhplus.be.server.api.dto.schedule.ScheduleResponse;
import kr.hhplus.be.server.application.schedule.ScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private QueueService queueService;

    @Test
    @DisplayName("콘서트의 예약 가능한 일정 목록을 조회할 수 있다")
    void getAvailableSchedules() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        Long concertId = 1L;
        
        List<ScheduleResponse> schedules = Arrays.asList(
                ScheduleResponse.builder()
                        .scheduleId(1L)
                        .concertId(concertId)
                        .performanceDate(LocalDate.now().plusDays(7))
                        .performanceTime(LocalDateTime.now().plusDays(7).withHour(19).withMinute(0))
                        .availableSeats(30)
                        .totalSeats(50)
                        .build(),
                ScheduleResponse.builder()
                        .scheduleId(2L)
                        .concertId(concertId)
                        .performanceDate(LocalDate.now().plusDays(14))
                        .performanceTime(LocalDateTime.now().plusDays(14).withHour(19).withMinute(0))
                        .availableSeats(45)
                        .totalSeats(50)
                        .build()
        );

        given(queueService.validateToken(token)).willReturn(true);
        given(scheduleService.getAvailableSchedules(concertId)).willReturn(schedules);

        // when & then
        mockMvc.perform(get("/api/concerts/{concertId}/schedules", concertId)
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].scheduleId").value(1))
                .andExpect(jsonPath("$[0].availableSeats").value(30))
                .andExpect(jsonPath("$[1].scheduleId").value(2))
                .andExpect(jsonPath("$[1].availableSeats").value(45));
    }

    @Test
    @DisplayName("매진된 일정은 조회되지 않는다")
    void soldOutSchedulesNotShown() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        Long concertId = 1L;
        
        // 일정은 있지만 예약 가능한 좌석이 있는 일정만 반환
        List<ScheduleResponse> availableSchedules = Arrays.asList(
                ScheduleResponse.builder()
                        .scheduleId(2L)
                        .concertId(concertId)
                        .performanceDate(LocalDate.now().plusDays(14))
                        .performanceTime(LocalDateTime.now().plusDays(14).withHour(19).withMinute(0))
                        .availableSeats(10)
                        .totalSeats(50)
                        .build()
        );

        given(queueService.validateToken(token)).willReturn(true);
        given(scheduleService.getAvailableSchedules(concertId)).willReturn(availableSchedules);

        // when & then
        mockMvc.perform(get("/api/concerts/{concertId}/schedules", concertId)
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].availableSeats").value(10));
    }

    @Test
    @DisplayName("지난 일정은 조회되지 않는다")
    void pastSchedulesNotShown() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        Long concertId = 1L;
        
        // 미래 일정만 반환
        List<ScheduleResponse> futureSchedules = Arrays.asList(
                ScheduleResponse.builder()
                        .scheduleId(3L)
                        .concertId(concertId)
                        .performanceDate(LocalDate.now().plusDays(30))
                        .performanceTime(LocalDateTime.now().plusDays(30).withHour(19).withMinute(0))
                        .availableSeats(50)
                        .totalSeats(50)
                        .build()
        );

        given(queueService.validateToken(token)).willReturn(true);
        given(scheduleService.getAvailableSchedules(concertId)).willReturn(futureSchedules);

        // when & then
        mockMvc.perform(get("/api/concerts/{concertId}/schedules", concertId)
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].scheduleId").value(3));
    }
}