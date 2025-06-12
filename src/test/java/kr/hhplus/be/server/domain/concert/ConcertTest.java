package kr.hhplus.be.server.domain.concert;

import kr.hhplus.be.server.domain.concert.Concert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConcertTest {

    @Test
    @DisplayName("콘서트를 생성한다")
    void createConcert() {
        // given & when
        Concert concert = Concert.builder()
                .title("아이유 콘서트")
                .artist("아이유")
                .venue("서울 올림픽공원")
                .description("2024 아이유 콘서트")
                .build();

        // then
        assertThat(concert.getTitle()).isEqualTo("아이유 콘서트");
        assertThat(concert.getArtist()).isEqualTo("아이유");
        assertThat(concert.getVenue()).isEqualTo("서울 올림픽공원");
        assertThat(concert.getDescription()).isEqualTo("2024 아이유 콘서트");
    }

    @Test
    @DisplayName("콘서트 정보를 수정한다")
    void updateConcertInfo() {
        // given
        Concert concert = Concert.builder()
                .title("BTS 콘서트")
                .artist("BTS")
                .venue("잠실 주경기장")
                .description("BTS 월드투어")
                .build();

        // when
        concert.updateVenue("고척스카이돔");

        // then
        assertThat(concert.getVenue()).isEqualTo("고척스카이돔");
    }
}