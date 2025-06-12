package kr.hhplus.be.server.infrastructure.persistance.reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.hhplus.be.server.domain.reservation.ReservationRepository;
import kr.hhplus.be.server.domain.reservation.model.Reservation;
import kr.hhplus.be.server.domain.reservation.model.Reservation.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationJpaRepository implements ReservationRepository{

    private final SpringReservationJpa springReservationJpa;

    @Override
    public Optional<Reservation> findByIdWithLock(Long id) {
        return springReservationJpa.findByIdWithLock(id)
            .map(this::toDomainModel);
    }

    @Override
    public boolean existsByUserIdAndScheduleIdAndStatusIn(String userId, Long scheduleId,
        List<Status> statuses) {
        return springReservationJpa.existsByUserIdAndScheduleIdAndStatusIn(userId, scheduleId
            , statuses);
    }

    @Override
    public List<Reservation> findExpiredTemporaryReservations(Status status,
        LocalDateTime expirationTime) {
        List<ReservationEntity> expiredEntities = springReservationJpa
            .findExpiredTemporaryReservations(status, expirationTime);
        return expiredEntities.stream()
            .map(this::toDomainModel)
            .toList();
    }

    @Override
    public List<Reservation> findByUserIdAndStatus(String userId, Status status) {
        return springReservationJpa.findByUserIdAndStatus(userId, status)
            .stream()
            .map(this::toDomainModel)
            .toList();
    }

    @Override
    public Optional<Reservation> findByUserIdAndSeatId(String userId, Long seatId) {
        return springReservationJpa.findByUserIdAndSeatId(userId, seatId)
            .map(this::toDomainModel);
    }

    @Override
    public List<Reservation> findActiveReservationsByUserId(String userId) {
        return springReservationJpa.findActiveReservationsByUserId(userId)
            .stream()
            .map(this::toDomainModel)
            .toList();
    }


    public Reservation save(Reservation reservation) {
        ReservationEntity entity = toEntity(reservation);
        ReservationEntity savedEntity = springReservationJpa.save(entity);
        if (reservation.getId() == null) {
            reservation.assignId(savedEntity.getId());
        }
        return reservation;
    }

    @Override
    public void saveAll(List<Reservation> expiredReservations) {
        List<ReservationEntity> entities = expiredReservations.stream()
            .map(this::toEntity)
            .toList();
        List<ReservationEntity> savedEntities = springReservationJpa.saveAll(entities);
        
        // 저장된 엔티티와 도메인 모델을 매핑
        for (int i = 0; i < expiredReservations.size(); i++) {
            Reservation reservation = expiredReservations.get(i);
            ReservationEntity savedEntity = savedEntities.get(i);
            
            if (reservation.getId() == null) {
                reservation.assignId(savedEntity.getId());
            }
        }
    }

    public Optional<Reservation> findById(Long id) {
        return springReservationJpa.findById(id)
            .map(this::toDomainModel);
    }

    public void deleteById(Long id) {
        springReservationJpa.deleteById(id);
    }

    private ReservationEntity toEntity(Reservation reservation) {
        return ReservationEntity.builder()
            .id(reservation.getId())
            .userId(reservation.getUserId())
            .scheduleId(reservation.getScheduleId())
            .seatId(reservation.getSeatId())
            .status(reservation.getStatus())
            .reservedAt(reservation.getReservedAt())
            .confirmedAt(reservation.getConfirmedAt())
            .expiredAt(reservation.getExpiredAt())
            .cancelledAt(reservation.getCancelledAt())
            .paymentId(reservation.getPaymentId())
            .version(reservation.getVersion())
            .build();
    }

    private Reservation toDomainModel(ReservationEntity entity) {
        return Reservation.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .scheduleId(entity.getScheduleId())
            .seatId(entity.getSeatId())
            .status(entity.getStatus())
            .reservedAt(entity.getReservedAt())
            .confirmedAt(entity.getConfirmedAt())
            .expiredAt(entity.getExpiredAt())
            .cancelledAt(entity.getCancelledAt())
            .paymentId(entity.getPaymentId())
            .version(entity.getVersion())
            .build();
    }
}
