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
import kr.hhplus.be.server.api.dto.amount.AmountChargeRequest;
import kr.hhplus.be.server.api.dto.amount.AmountResponse;
import kr.hhplus.be.server.application.amount.AmountService;
import kr.hhplus.be.server.application.queue.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "잔액 API", description = "잔액 충전 및 조회 API")
@SecurityRequirement(name = "Queue-Token")
@RestController
@RequestMapping("/api/amounts")
@RequiredArgsConstructor
public class AmountController {

    private final AmountService amountService;
    private final QueueService queueService;

    @Operation(summary = "잔액 충전", description = "사용자의 잔액을 충전합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "충전 성공",
                    content = @Content(schema = @Schema(implementation = AmountResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (음수 금액, 한도 초과 등)")
    })
    @PostMapping("/charge")
    public ResponseEntity<AmountResponse> chargeAmount(
            @Parameter(description = "대기열 토큰", required = true)
            @RequestHeader("Queue-Token") String token,
            @Valid @RequestBody AmountChargeRequest request) {
        try {
            String userId = queueService.getUserIdFromToken(token);
            AmountResponse response = amountService.charge(userId, request.getAmount());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AmountResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 토큰")
    })
    @GetMapping("/balance")
    public ResponseEntity<AmountResponse> getBalance(
            @Parameter(description = "대기열 토큰", required = true)
            @RequestHeader("Queue-Token") String token) {
        try {
            String userId = queueService.getUserIdFromToken(token);
            AmountResponse response = amountService.getBalance(userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}