package kr.hhplus.be.server.api.dto.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    
    @NotNull(message = "스케줄 ID는 필수입니다")
    private Long scheduleId;
    
    @NotNull(message = "좌석 번호는 필수입니다")
    @Min(value = 1, message = "좌석 번호는 1 이상이어야 합니다")
    @Max(value = 50, message = "좌석 번호는 50 이하여야 합니다")
    private Integer seatNumber;
}