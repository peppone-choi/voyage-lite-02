package kr.hhplus.be.server.reservation.interfaces.web.dto;

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
    
    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;
    
    @NotNull(message = "Seat number is required")
    @Min(value = 1, message = "Seat number must be at least 1")
    @Max(value = 50, message = "Seat number must be at most 50")
    private Integer seatNumber;
}