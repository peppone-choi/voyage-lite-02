package kr.hhplus.be.server.infrastructure.persistance.concert;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.concert.Concert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "concerts")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcertEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String artist;
    
    @Column(nullable = false)
    private String venue;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    public static ConcertEntity fromDomain(Concert concert) {
        return ConcertEntity.builder()
                .id(concert.getId())
                .title(concert.getTitle())
                .artist(concert.getArtist())
                .venue(concert.getVenue())
                .description(concert.getDescription())
                .build();
    }
    
    public Concert toDomain() {
        return Concert.builder()
                .id(this.id)
                .title(this.title)
                .artist(this.artist)
                .venue(this.venue)
                .description(this.description)
                .build();
    }
}