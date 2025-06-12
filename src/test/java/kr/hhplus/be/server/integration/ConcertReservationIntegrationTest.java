package kr.hhplus.be.server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.api.dto.amount.AmountChargeRequest;
import kr.hhplus.be.server.api.dto.payment.PaymentRequest;
import kr.hhplus.be.server.api.dto.queue.QueueTokenRequest;
import kr.hhplus.be.server.api.dto.reservation.ReservationRequest;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.ReservationRepository;
import kr.hhplus.be.server.domain.seat.SeatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import kr.hhplus.be.server.TestcontainersConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("콘서트 예약 통합 테스트")
class ConcertReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private QueueTokenRepository queueTokenRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private SeatRepository seatRepository;

    @Test
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
    
    @Test
    @DisplayName("만료된 임시예약은 다시 예약 가능해야 한다")
    void expiredTemporaryReservationCanBeReservedAgain() throws Exception {
        // 1. 첫 번째 유저 토큰 발급
        String userId1 = UUID.randomUUID().toString();
        QueueTokenRequest tokenRequest1 = new QueueTokenRequest(userId1);
        
        MvcResult tokenResult1 = mockMvc.perform(post("/api/queue/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String token1 = objectMapper.readTree(tokenResult1.getResponse().getContentAsString())
                .get("token").asText();
                
        // 토큰을 ACTIVE 상태로 변경
        var queueToken1 = queueTokenRepository.findByToken(token1).orElseThrow();
        queueToken1.activate();
        queueTokenRepository.save(queueToken1);

        // 2. 잔액 충전
        AmountChargeRequest chargeRequest = AmountChargeRequest.builder()
                .amount(BigDecimal.valueOf(200000))
                .build();

        mockMvc.perform(post("/api/amounts/charge")
                        .header("Queue-Token", token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeRequest)))
                .andExpect(status().isOk());

        // 3. 좌석 예약 (임시예약)
        Long scheduleId = 1L;
        int seatNumber = 15;
        ReservationRequest reservationRequest = ReservationRequest.builder()
                .scheduleId(scheduleId)
                .seatNumber(seatNumber)
                .build();

        MvcResult reservationResult = mockMvc.perform(post("/api/reservations")
                        .header("Queue-Token", token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TEMPORARY_RESERVED"))
                .andReturn();

        Long reservationId = objectMapper.readTree(reservationResult.getResponse().getContentAsString())
                .get("reservationId").asLong();
        
        // 4. 임시예약 만료 시뮬레이션 (예약 만료 시간 변경)
        var reservation = reservationRepository.findById(reservationId).orElseThrow();
        reservation.setExpired();
        reservationRepository.save(reservation);
        
        // 좌석 상태도 다시 AVAILABLE로 변경
        var seat = seatRepository.findByScheduleIdAndSeatNumber(scheduleId, seatNumber).orElseThrow();
        seat.release();
        seatRepository.save(seat);

        // 5. 두 번째 유저 토큰 발급
        String userId2 = UUID.randomUUID().toString();
        QueueTokenRequest tokenRequest2 = new QueueTokenRequest(userId2);
        
        MvcResult tokenResult2 = mockMvc.perform(post("/api/queue/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest2)))
                .andExpect(status().isOk())
                .andReturn();

        String token2 = objectMapper.readTree(tokenResult2.getResponse().getContentAsString())
                .get("token").asText();
                
        // 토큰을 ACTIVE 상태로 변경
        var queueToken2 = queueTokenRepository.findByToken(token2).orElseThrow();
        queueToken2.activate();
        queueTokenRepository.save(queueToken2);

        // 6. 두 번째 유저 잔액 충전
        mockMvc.perform(post("/api/amounts/charge")
                        .header("Queue-Token", token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeRequest)))
                .andExpect(status().isOk());

        // 7. 두 번째 유저가 같은 좌석 예약 시도 (성공해야 함)
        mockMvc.perform(post("/api/reservations")
                        .header("Queue-Token", token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TEMPORARY_RESERVED"))
                .andExpect(jsonPath("$.seatNumber").value(seatNumber));
    }
    
    @Test
    @DisplayName("다중 유저가 동시에 같은 좌석을 예약하면 한 명만 성공해야 한다")
    void concurrentSeatReservationOnlyOneSuccess() throws Exception {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        Long scheduleId = 1L;
        int seatNumber = 20;
        
        // 좌석이 사용 가능한 상태인지 확인
        var seat = seatRepository.findByScheduleIdAndSeatNumber(scheduleId, seatNumber).orElseThrow();
        seat.release();
        seatRepository.save(seat);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 각 스레드별로 다른 유저 생성
                    String userId = UUID.randomUUID().toString();
                    QueueTokenRequest tokenRequest = new QueueTokenRequest(userId);
                    
                    // 토큰 발급
                    MvcResult tokenResult = mockMvc.perform(post("/api/queue/token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(tokenRequest)))
                            .andExpect(status().isOk())
                            .andReturn();

                    String token = objectMapper.readTree(tokenResult.getResponse().getContentAsString())
                            .get("token").asText();
                    
                    // 토큰을 ACTIVE 상태로 변경
                    var queueToken = queueTokenRepository.findByToken(token).orElseThrow();
                    queueToken.activate();
                    queueTokenRepository.save(queueToken);

                    // 잔액 충전
                    AmountChargeRequest chargeRequest = AmountChargeRequest.builder()
                            .amount(BigDecimal.valueOf(200000))
                            .build();

                    mockMvc.perform(post("/api/amounts/charge")
                                    .header("Queue-Token", token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(chargeRequest)))
                            .andExpect(status().isOk());

                    // 동시에 같은 좌석 예약 시도
                    ReservationRequest reservationRequest = ReservationRequest.builder()
                            .scheduleId(scheduleId)
                            .seatNumber(seatNumber)
                            .build();

                    MvcResult result = mockMvc.perform(post("/api/reservations")
                                    .header("Queue-Token", token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(reservationRequest)))
                            .andReturn();
                    
                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // 검증: 오직 한 명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);
        
        // 좌석 상태 확인
        var finalSeat = seatRepository.findByScheduleIdAndSeatNumber(scheduleId, seatNumber).orElseThrow();
        assertThat(finalSeat.isOccupied()).isTrue();
    }
}