package kr.hhplus.be.server.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.application.queue.QueueService;
import kr.hhplus.be.server.api.dto.schedule.ScheduleResponse;
import kr.hhplus.be.server.application.schedule.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "일정 API", description = "콘서트 일정 조회 API")
@SecurityRequirement(name = "Queue-Token")
@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final QueueService queueService;

    @Operation(summary = "예약 가능 일정 조회", description = "특정 콘서트의 예약 가능한 일정 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ScheduleResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "콘서트를 찾을 수 없음")
    })
    @GetMapping("/{concertId}/schedules")
    public ResponseEntity<List<ScheduleResponse>> getAvailableSchedules(
            @Parameter(description = "콘서트 ID", required = true)
            @PathVariable Long concertId,
            @Parameter(description = "대기열 토큰", required = true)
            @RequestHeader("Queue-Token") String token) {
        if (!queueService.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        List<ScheduleResponse> schedules = scheduleService.getAvailableSchedules(concertId);
        return ResponseEntity.ok(schedules);
    }
}