package com.github.ipantazi.carsharing.repository.car;

import com.github.ipantazi.carsharing.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    boolean existsByModelAndBrand(String model, String brand);
}
