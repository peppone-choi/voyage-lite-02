package kr.hhplus.be.server.seat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.queue.service.QueueService;
import kr.hhplus.be.server.seat.dto.SeatResponse;
import kr.hhplus.be.server.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "좌석 API", description = "좌석 조회 API")
@SecurityRequirement(name = "Queue-Token")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;
    private final QueueService queueService;

    @Operation(summary = "예약 가능 좌석 조회", description = "특정 일정의 예약 가능한 좌석 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SeatResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음")
    })
    @GetMapping("/{scheduleId}/seats")
    public ResponseEntity<Map<String, Object>> getAvailableSeats(
            @Parameter(description = "일정 ID", required = true)
            @PathVariable Long scheduleId,
            @Parameter(description = "공연 날짜")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "대기열 토큰", required = true)
            @RequestHeader("Queue-Token") String token) {
        if (!queueService.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        List<SeatResponse> availableSeats = seatService.getAvailableSeats(scheduleId);
        Map<String, Object> response = Map.of(
                "scheduleId", scheduleId,
                "date", date != null ? date.toString() : "",
                "availableSeats", availableSeats
        );
        return ResponseEntity.ok(response);
    }
}