package com.github.ipantazi.carsharing.repository.rental;

import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.SpecificationProvider;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class IsActiveSpecificationProvider implements SpecificationProvider<Rental> {
    @Override
    public String getKey() {
        return "is_active";
    }

    @Override
    public Specification<Rental> getSpecification(Object params) {
        if (params instanceof Boolean isActive) {
            return (root, query, criteriaBuilder) -> isActive
                    ? criteriaBuilder.isNull(root.get("actualReturnDate"))
                    : criteriaBuilder.isNotNull(root.get("actualReturnDate"));
        }
        return Specification.where(null);
    }
}
