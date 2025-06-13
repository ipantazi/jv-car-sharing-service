package com.github.ipantazi.carsharing.repository.rental;

import static com.github.ipantazi.carsharing.util.repository.RepositoryTestDataUtil.KEY_IS_ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
public class IsActiveSpecificationProviderTest {
    @Mock
    private Root<Rental> root;
    @Mock
    private CriteriaBuilder criteriaBuilder;
    @Mock
    private CriteriaQuery<Rental> query;
    @Mock
    private Path<String> path;
    @InjectMocks
    private IsActiveSpecificationProvider provider;

    @Test
    @DisplayName("Verify getKey() method works.")
    public void getKey_ReturnsCorrectKey() {
        assertThat(provider.getKey()).isEqualTo(KEY_IS_ACTIVE);
    }

    @Test
    @DisplayName("Verify getSpecification() with parameter isActive = true")
    public void getSpecification_ValidParams_ReturnsCorrectSpecification() {
        // Given
        boolean isActive = true;
        Predicate expectedPredicate = mock(Predicate.class);
        when(root.<String>get("actualReturnDate")).thenReturn(path);
        when(criteriaBuilder.isNull(path)).thenReturn(expectedPredicate);

        // When
        Specification<Rental> actualSpecification = provider.getSpecification(isActive);

        // Then
        assertThat(actualSpecification).isNotNull();
        Predicate actualPredicate = actualSpecification.toPredicate(root, query, criteriaBuilder);
        assertThat(actualPredicate).isNotNull();
        assertThat(actualPredicate).isEqualTo(expectedPredicate);
        verify(root, times(1)).get("actualReturnDate");
        verify(criteriaBuilder, times(1)).isNull(path);
        verify(criteriaBuilder, never()).isNotNull(path);
        verifyNoMoreInteractions(criteriaBuilder, root, query);
    }

    @Test
    @DisplayName("Verify getSpecification() with parameter isActive = false")
    public void getSpecification_ValidParams_ReturnsCorrectSpecification2() {
        // Given
        boolean isActive = false;
        Predicate expectedPredicate = mock(Predicate.class);
        when(root.<String>get("actualReturnDate")).thenReturn(path);
        when(criteriaBuilder.isNotNull(path)).thenReturn(expectedPredicate);

        // When
        Specification<Rental> actualSpecification = provider.getSpecification(isActive);

        // Then
        assertThat(actualSpecification).isNotNull();
        Predicate actualPredicate = actualSpecification.toPredicate(root, query, criteriaBuilder);
        assertThat(actualPredicate).isNotNull();
        assertThat(actualPredicate).isEqualTo(expectedPredicate);
        verify(root, times(1)).get("actualReturnDate");
        verify(criteriaBuilder, times(1)).isNotNull(path);
        verify(criteriaBuilder, never()).isNull(path);
        verifyNoMoreInteractions(criteriaBuilder, root, query);
    }

    @Test
    @DisplayName("Verify that the method returns empty specification for non-boolean parameters.")
    public void getSpecification_NonStringParams_ReturnsSpecificationWhereNull() {
        // When
        Specification<Rental> actualSpecification = provider.getSpecification("NOT BOOLEAN");

        // Then
        assertThat(actualSpecification).isNotNull();
        assertThat(actualSpecification).isEqualTo(Specification.where(null));
        verifyNoInteractions(criteriaBuilder, root);
    }

    @Test
    @DisplayName("Verify that the method returns empty specification for null parameters")
    public void getSpecification_NulParams_ReturnsSpecificationWhereNull() {
        // When
        Specification<Rental> actualSpecification = provider.getSpecification(null);

        // Then
        assertThat(actualSpecification).isNotNull();
        assertThat(actualSpecification).isEqualTo(Specification.where(null));
        verifyNoInteractions(criteriaBuilder, root);
    }
}
