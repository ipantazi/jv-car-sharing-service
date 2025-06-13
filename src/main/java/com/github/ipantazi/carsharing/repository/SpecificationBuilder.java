package com.github.ipantazi.carsharing.repository;

import org.springframework.data.jpa.domain.Specification;

public interface SpecificationBuilder<T> {
    Specification<T> build(Long userId, Boolean isActive);
}
