package kr.hhplus.be.server.payment.application;

import kr.hhplus.be.server.amount.domain.AmountRepository;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertRepository;
import kr.hhplus.be.server.payment.domain.PaymentRepository;
import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.dto.PaymentResponse;
import kr.hhplus.be.server.reservation.domain.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.schedule.domain.Schedule;
import kr.hhplus.be.server.schedule.domain.ScheduleRepository;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ScheduleRepository scheduleRepository;
    private final ConcertRepository concertRepository;
    private final AmountRepository amountRepository;

    @Transactional
    public PaymentResponse execute(String userId, Long reservationId) {
        // Get reservation with lock
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));
        
        // Validate reservation belongs to user
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 예약은 사용자의 예약이 아닙니다");
        }
        
        // Check reservation status
        if (reservation.getStatus() == Reservation.Status.CONFIRMED) {
            throw new IllegalStateException("이미 결제된 예약입니다");
        }
        
        if (reservation.getStatus() == Reservation.Status.CANCELLED) {
            throw new IllegalStateException("취소된 예약입니다");
        }
        
        if (reservation.isExpired()) {
            throw new IllegalStateException("만료된 예약입니다");
        }
        
        // Check if payment already exists
        if (paymentRepository.existsActivePaymentByReservationId(reservationId)) {
            throw new IllegalStateException("이미 결제가 진행 중이거나 완료되었습니다");
        }
        
        // Get seat information
        Seat seat = seatRepository.findById(reservation.getSeatId())
                .orElseThrow(() -> new IllegalStateException("좌석을 찾을 수 없습니다"));
        
        // Create payment
        Payment payment = Payment.create(userId, reservationId, seat.getPrice());
        Payment savedPayment = paymentRepository.save(payment);
        
        try {
            // Process payment (deduct from user balance)
            var amount = amountRepository.findByUserIdWithLock(userId)
                    .orElseThrow(() -> new IllegalStateException("사용자 잔액 정보를 찾을 수 없습니다"));
            amount.use(seat.getPrice());
            amountRepository.save(amount);
            
            // Complete payment
            savedPayment.complete();
            paymentRepository.save(savedPayment);
            
            // Confirm reservation
            reservation.confirm(savedPayment.getId());
            reservationRepository.save(reservation);
            
            // Confirm seat reservation
            seat.confirmReservation();
            seatRepository.save(seat);
            
            log.info("Payment completed: {} for reservation: {}", savedPayment.getId(), reservationId);
            
            // Get additional info for response
            Schedule schedule = scheduleRepository.findById(reservation.getScheduleId())
                    .orElseThrow(() -> new IllegalStateException("일정을 찾을 수 없습니다"));
            
            Concert concert = concertRepository.findById(schedule.getConcertId())
                    .orElseThrow(() -> new IllegalStateException("콘서트를 찾을 수 없습니다"));
            
            return convertToResponse(savedPayment, concert, schedule, seat);
            
        } catch (Exception e) {
            // Fail payment on any error
            savedPayment.fail(e.getMessage());
            paymentRepository.save(savedPayment);
            
            log.error("Payment failed for reservation: {}", reservationId, e);
            throw e;
        }
    }

    private PaymentResponse convertToResponse(Payment payment, Concert concert, Schedule schedule, Seat seat) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .reservationId(payment.getReservationId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paidAt(payment.getPaidAt())
                .concertTitle(concert.getTitle())
                .performanceDate(schedule.getPerformanceTime())
                .seatNumber(seat.getSeatNumber())
                .build();
    }
}