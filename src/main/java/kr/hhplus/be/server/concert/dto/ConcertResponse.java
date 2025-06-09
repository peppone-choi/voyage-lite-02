package kr.hhplus.be.server.concert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcertResponse {
    private Long concertId;
    private String title;
    private String artist;
    private String venue;
    private String description;
}