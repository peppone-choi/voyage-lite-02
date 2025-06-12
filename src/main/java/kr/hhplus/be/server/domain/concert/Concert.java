package kr.hhplus.be.server.domain.concert;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "concerts")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Concert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String artist;
    
    @Column(nullable = false)
    private String venue;
    
    @Column(length = 1000)
    private String description;
    
    public void updateVenue(String venue) {
        this.venue = venue;
    }
}