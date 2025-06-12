package kr.hhplus.be.server.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.api.dto.queue.QueueTokenRequest;
import kr.hhplus.be.server.api.dto.queue.QueueTokenResponse;
import kr.hhplus.be.server.application.queue.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "대기열 API", description = "대기열 토큰 발급 및 조회 API")
@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @Operation(summary = "대기열 토큰 발급", description = "서비스 이용을 위한 대기열 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 발급 성공",
                    content = @Content(schema = @Schema(implementation = QueueTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/token")
    public ResponseEntity<QueueTokenResponse> issueToken(
            @Valid @RequestBody QueueTokenRequest request) {
        QueueTokenResponse response = queueService.issueToken(request.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "대기열 상태 조회", description = "토큰을 사용하여 현재 대기열 상태를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = QueueTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "토큰을 찾을 수 없음")
    })
    @GetMapping("/status")
    public ResponseEntity<QueueTokenResponse> getQueueStatus(
            @Parameter(description = "대기열 토큰", required = true)
            @RequestHeader("Queue-Token") String token) {
        QueueTokenResponse response = queueService.getQueueStatus(token);
        return ResponseEntity.ok(response);
    }
}