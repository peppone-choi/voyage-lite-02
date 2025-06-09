package kr.hhplus.be.server.concert.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.concert.dto.ConcertResponse;
import kr.hhplus.be.server.concert.service.ConcertService;
import kr.hhplus.be.server.queue.service.QueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConcertController.class)
class ConcertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConcertService concertService;

    @MockBean
    private QueueService queueService;

    @Test
    @DisplayName("예약 가능한 날짜 목록을 조회할 수 있다")
    void getAvailableDates() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        Long concertId = 1L;
        
        List<LocalDate> availableDates = Arrays.asList(
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(14),
                LocalDate.now().plusDays(21)
        );

        given(queueService.validateToken(token)).willReturn(true);
        given(concertService.getAvailableDates(concertId)).willReturn(availableDates);

        // when & then
        mockMvc.perform(get("/api/concerts/{concertId}/available-dates", concertId)
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concertId").value(concertId))
                .andExpect(jsonPath("$.availableDates").isArray())
                .andExpect(jsonPath("$.availableDates.length()").value(3));
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 예약 가능 날짜 조회시 에러가 발생한다")
    void getAvailableDatesWithInvalidToken() throws Exception {
        // given
        String invalidToken = "invalid-token";
        Long concertId = 1L;

        given(queueService.validateToken(invalidToken)).willReturn(false);

        // when & then
        mockMvc.perform(get("/api/concerts/{concertId}/available-dates", concertId)
                        .header("Queue-Token", invalidToken))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("콘서트 목록을 조회할 수 있다")
    void getConcertList() throws Exception {
        // given
        String token = UUID.randomUUID().toString();
        
        List<ConcertResponse> concerts = Arrays.asList(
                ConcertResponse.builder()
                        .concertId(1L)
                        .title("아이유 콘서트")
                        .artist("아이유")
                        .venue("서울 올림픽공원")
                        .build(),
                ConcertResponse.builder()
                        .concertId(2L)
                        .title("BTS 월드투어")
                        .artist("BTS")
                        .venue("고척스카이돔")
                        .build()
        );

        given(queueService.validateToken(token)).willReturn(true);
        given(concertService.getAllConcerts()).willReturn(concerts);

        // when & then
        mockMvc.perform(get("/api/concerts")
                        .header("Queue-Token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("아이유 콘서트"))
                .andExpect(jsonPath("$[1].title").value("BTS 월드투어"));
    }
}