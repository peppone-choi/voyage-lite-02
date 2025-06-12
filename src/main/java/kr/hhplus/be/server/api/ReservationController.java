package kr.hhplus.be.server.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.application.queue.QueueService;
import kr.hhplus.be.server.application.reservation.ReservationCreateService;
import kr.hhplus.be.server.domain.reservation.model.Reservation;
import kr.hhplus.be.server.api.dto.reservation.ReservationRequest;
import kr.hhplus.be.server.api.dto.reservation.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "예약 API", description = "좌석 예약 관련 API")
@SecurityRequirement(name = "Queue-Token")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationCreateService reservationCreateService;
    private final QueueService queueService;

    @Operation(summary = "좌석 예약", description = "좌석을 임시 예약합니다. 5분 이내에 결제를 완료해야 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "예약 성공",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 예약된 좌석, 유효하지 않은 좌석 번호 등)"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰 또는 활성화되지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "일정 또는 좌석을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> reserveSeat(
            @Parameter(description = "대기열 토큰", required = true)
            @RequestHeader("Queue-Token") String token,
            @Valid @RequestBody ReservationRequest request) {
        // 대기열 토큰 검증 및 사용자 ID 추출
        String userId = queueService.validateAndGetUserId(token);
        // 좌석 예약 처리
        Reservation reservation = reservationCreateService.reserveSeat(userId, request);
        // 예약 응답 생성
        ReservationResponse response = ReservationResponse.builder()
                .reservationId(reservation.getId())
                .userId(reservation.getUserId())
                .scheduleId(reservation.getScheduleId())
                .seatId(reservation.getSeatId())
                .price(null) // 가격은 예약 생성 시 계산되지 않으므로 null로 설정
                .status(reservation.getStatus().name())
                .reservedAt(reservation.getReservedAt())
                .expiresAt(reservation.getStatus() == Reservation.Status.TEMPORARY_RESERVED ? 
                        reservation.getExpirationTime() : null)
                .confirmedAt(reservation.getConfirmedAt())
                .build();

        // 예약 응답 반환
        return ResponseEntity.ok(response);
    }
}