package kr.hhplus.be.server.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.payment.dto.PaymentRequest;
import kr.hhplus.be.server.payment.dto.PaymentResponse;
import kr.hhplus.be.server.payment.service.PaymentService;
import kr.hhplus.be.server.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "결제 API", description = "예약 결제 처리 API")
@SecurityRequirement(name = "Queue-Token")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final QueueService queueService;

    @Operation(summary = "예약 결제", description = "예약한 좌석을 결제합니다. 결제 완료시 좌석 소유권이 확정되고 대기열 토큰이 만료됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 성공",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잔액 부족, 이미 결제됨, 만료된 예약 등)"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰 또는 활성화되지 않은 토큰"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @Parameter(description = "대기열 토큰", required = true)
            @RequestHeader("Queue-Token") String token,
            @Valid @RequestBody PaymentRequest request) {
        try {
            String userId = queueService.validateAndGetUserId(token);
            PaymentResponse response = paymentService.processPayment(userId, request.getReservationId());
            
            // 결제 성공시 토큰 만료
            queueService.expireToken(token);
            
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