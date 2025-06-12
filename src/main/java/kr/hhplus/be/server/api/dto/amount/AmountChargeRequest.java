package kr.hhplus.be.server.api.dto.amount;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmountChargeRequest {
    
    @NotNull(message = "충전 금액은 필수입니다")
    @DecimalMin(value = "0.01", message = "충전 금액은 0보다 커야 합니다")
    private BigDecimal amount;
}