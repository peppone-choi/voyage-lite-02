package kr.hhplus.be.server.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long seatId;
    private Integer seatNumber;
    private String grade;
    private BigDecimal price;
    private String status;
}