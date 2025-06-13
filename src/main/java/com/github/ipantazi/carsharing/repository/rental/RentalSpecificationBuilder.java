package com.github.ipantazi.carsharing.repository.rental;

import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.SpecificationBuilder;
import com.github.ipantazi.carsharing.repository.SpecificationProviderManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentalSpecificationBuilder implements SpecificationBuilder<Rental> {
    private final SpecificationProviderManager<Rental> specificationProviderManager;

    @Override
    public Specification<Rental> build(Long userId, Boolean isActive) {
        Specification<Rental> spec = Specification.where(null);
        if (userId != null) {
            spec = spec.and(specificationProviderManager.getSpecificationProvider("userId")
                    .getSpecification(userId));
        }
        if (isActive != null) {
            spec = spec.and(specificationProviderManager.getSpecificationProvider("is_active")
                    .getSpecification(isActive));
        }
        return spec;
    }
}
