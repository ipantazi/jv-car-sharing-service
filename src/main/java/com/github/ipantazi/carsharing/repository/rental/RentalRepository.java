package com.github.ipantazi.carsharing.repository.rental;

import com.github.ipantazi.carsharing.model.Rental;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {
    Optional<Rental> findRentalByIdAndUserId(Long rentalId, Long userId);

    Page<Rental> findAll(Specification<Rental> spec, Pageable pageable);

    List<Rental> findAllByReturnDateLessThanEqualAndActualReturnDateIsNull(LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Rental r WHERE r.id = :id")
    Optional<Rental> lockRentalForUpdate(@Param("id") Long rentalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Rental r
        WHERE r.carId = :carId
          AND r.actualReturnDate IS NULL
            """)
    List<Rental> lockActiveRentalsForUpdateByCarId(@Param("carId") Long carId);
}
