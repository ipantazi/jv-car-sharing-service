package com.github.ipantazi.carsharing.repository.rental;

import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.SpecificationProvider;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class IsActiveSpecificationProvider implements SpecificationProvider<Rental> {
    public static final String KEY = "is_active";
    private static final String ACTUAL_RETURN_DATE = "actualReturnDate";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Specification<Rental> getSpecification(Object params) {
        if (params instanceof Boolean isActive) {
            return (root, query, criteriaBuilder) -> isActive
                    ? criteriaBuilder.isNull(root.get(ACTUAL_RETURN_DATE))
                    : criteriaBuilder.isNotNull(root.get(ACTUAL_RETURN_DATE));
        }
        return null;
    }
}
