package com.github.ipantazi.carsharing.repository.rental;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.repository.RepositoryTestDataUtil.KEY_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.model.Rental;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
public class UserIdSpecificationProviderTest {
    @Mock
    private Root<Rental> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder criteriaBuilder;
    @Mock
    private Path<String> path;
    @InjectMocks
    private UserIdSpecificationProvider provider;

    @Test
    @DisplayName("Verify getKey() method works.")
    public void getKey_ReturnsCorrectKey() {
        assertThat(provider.getKey()).isNotNull().isEqualTo(KEY_USER_ID);
    }

    @Test
    @DisplayName("Verify getSpecification() returns correct predicate")
    public void getSpecification_ValidParams_ReturnsCorrectSpecification() {
        // Given
        Predicate expectedPredicate = mock(Predicate.class);
        when(root.<String>get(KEY_USER_ID)).thenReturn(path);
        when(criteriaBuilder.equal(path, EXISTING_USER_ID)).thenReturn(expectedPredicate);

        // When
        Specification<Rental> actualSpecification = provider.getSpecification(EXISTING_USER_ID);

        // Then
        assertThat(actualSpecification).isNotNull();
        Predicate actualPredicate = actualSpecification.toPredicate(root, query, criteriaBuilder);
        assertThat(actualPredicate).isNotNull();
        assertThat(actualPredicate).isEqualTo(expectedPredicate);
        verify(root, times(1)).get(KEY_USER_ID);
        verify(criteriaBuilder, times(1)).equal(path, EXISTING_USER_ID);
        verifyNoMoreInteractions(criteriaBuilder, root, query);
    }

    @Test
    @DisplayName("Verify that the method returns empty specification for non-long parameters.")
    public void getSpecification_NonLongParams_ReturnsSpecificationWhereNull() {
        // When
        Specification<Rental> actualSpecification = provider.getSpecification("NOT LONG");

        // Then
        assertThat(actualSpecification).isNull();
        verifyNoInteractions(criteriaBuilder, root);
    }

    @Test
    @DisplayName("Verify that the method returns empty specification for null parameters")
    public void getSpecification_NulParams_ReturnsSpecificationWhereNull() {
        // When
        Specification<Rental> actualSpecification = provider.getSpecification(null);

        // Then
        assertThat(actualSpecification).isNull();
        verifyNoInteractions(criteriaBuilder, root);
    }

    @Test
    @DisplayName("Verify that the method returns empty specification for negative user ID")
    public void getSpecification_NegativeValue_ReturnsSpecificationWhereNull() {
        // When
        Specification<Rental> actualSpecification = provider.getSpecification(-1L);

        // Then
        assertThat(actualSpecification).isNull();
    }
}
