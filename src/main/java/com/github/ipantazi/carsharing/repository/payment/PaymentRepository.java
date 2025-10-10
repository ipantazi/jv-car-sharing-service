package com.github.ipantazi.carsharing.repository.payment;

import com.github.ipantazi.carsharing.model.Payment;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findPaymentsByStatus(Payment.Status status);

    Optional<Payment> findPaymentBySessionId(String sessionId);

    Optional<Payment> findPaymentByRentalIdAndType(Long rentalId, Payment.Type type);

    @Query("""
    SELECT p
    FROM Payment p
    WHERE p.rentalId IN (
        SELECT r.id
        FROM Rental r
        WHERE r.userId = :userId
    )
            """)
    Page<Payment> findPaymentsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = """
    SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
    FROM payments p
    JOIN rentals r ON p.rental_id = r.id
    WHERE r.user_id = :userId AND p.status = :status AND p.is_deleted = false
            """, nativeQuery = true)
    Long existsByUserIdAndStatus(@Param("userId") Long userId,
                                 @Param("status") String status);

    @Query("""
    SELECT COALESCE(SUM(p.amountToPay), 0) 
    FROM Payment p 
    WHERE p.rentalId = :rentalId AND p.status = :status
            """)
    BigDecimal sumAmountToPayByRentalIdAndStatus(@Param("rentalId") Long rentalId,
                                                 @Param("status") Payment.Status status);

}
