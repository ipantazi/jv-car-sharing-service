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
        Specification<Rental> spec1 = (userId != null)
                ? specificationProviderManager.getSpecificationProvider("userId")
                .getSpecification(userId)
                : null;

        Specification<Rental> spec2 = (isActive != null)
                ? specificationProviderManager.getSpecificationProvider("is_active")
                .getSpecification(isActive)
                : null;

        return spec1 == null ? spec2 : spec2 == null ? spec1 : spec1.and(spec2);
    }
}
