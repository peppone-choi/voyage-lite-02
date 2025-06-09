package kr.hhplus.be.server.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.queue.service.QueueService;
import kr.hhplus.be.server.reservation.dto.ReservationRequest;
import kr.hhplus.be.server.reservation.dto.ReservationResponse;
import kr.hhplus.be.server.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "예약 API", description = "좌석 예약 관련 API")
@SecurityRequirement(name = "Queue-Token")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
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
        try {
            String userId = queueService.validateAndGetUserId(token);
            ReservationResponse response = reservationService.reserveSeat(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Token")) {
                return ResponseEntity.status(401).build();
            }
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}