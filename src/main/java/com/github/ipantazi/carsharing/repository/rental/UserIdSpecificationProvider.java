package com.github.ipantazi.carsharing.repository.rental;

import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.SpecificationProvider;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class UserIdSpecificationProvider implements SpecificationProvider<Rental> {
    public static final String KEY = "userId";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Specification<Rental> getSpecification(Object params) {
        if (params instanceof Long userId && userId > 0) {
            return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                    root.get(this.getKey()), userId
            ));
        }
        return null;
    }
}
