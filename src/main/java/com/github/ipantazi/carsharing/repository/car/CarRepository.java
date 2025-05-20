package com.github.ipantazi.carsharing.repository.car;

import com.github.ipantazi.carsharing.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    boolean existsByModelAndBrand(String model, String brand);

    @Query(value = """
    SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
    FROM cars
    WHERE model = :model AND brand = :brand AND is_deleted = TRUE
            """, nativeQuery = true)
    Long existsSoftDeletedByModelAndBrand(@Param("model") String model,
                                          @Param("brand") String brand);
}
