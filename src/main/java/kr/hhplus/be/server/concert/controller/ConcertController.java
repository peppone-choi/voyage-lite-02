package kr.hhplus.be.server.concert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.concert.dto.ConcertResponse;
import kr.hhplus.be.server.concert.service.ConcertService;
import kr.hhplus.be.server.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "콘서트 API", description = "콘서트 정보 및 예약 가능 날짜 조회 API")
@SecurityRequirement(name = "Queue-Token")
@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;
    private final QueueService queueService;

    @Operation(summary = "콘서트 목록 조회", description = "예약 가능한 콘서트 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ConcertResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    @GetMapping
    public ResponseEntity<List<ConcertResponse>> getConcerts(
            @Parameter(description = "대기열 토큰", required = true)
            @RequestHeader("Queue-Token") String token) {
        if (!queueService.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        List<ConcertResponse> concerts = concertService.getAllConcerts();
        return ResponseEntity.ok(concerts);
    }

    @Operation(summary = "예약 가능 날짜 조회", description = "특정 콘서트의 예약 가능한 날짜 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "콘서트를 찾을 수 없음")
    })
    @GetMapping("/{concertId}/available-dates")
    public ResponseEntity<Map<String, Object>> getAvailableDates(
            @Parameter(description = "콘서트 ID", required = true)
            @PathVariable Long concertId,
            @Parameter(description = "대기열 토큰", required = true)
            @RequestHeader("Queue-Token") String token) {
        if (!queueService.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        List<LocalDate> availableDates = concertService.getAvailableDates(concertId);
        Map<String, Object> response = Map.of(
                "concertId", concertId,
                "availableDates", availableDates
        );
        return ResponseEntity.ok(response);
    }
}