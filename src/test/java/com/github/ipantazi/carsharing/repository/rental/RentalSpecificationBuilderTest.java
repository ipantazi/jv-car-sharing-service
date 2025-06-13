package com.github.ipantazi.carsharing.repository.rental;

import static com.github.ipantazi.carsharing.util.repository.RepositoryTestDataUtil.KEY_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.SpecificationProvider;
import com.github.ipantazi.carsharing.repository.SpecificationProviderManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
public class RentalSpecificationBuilderTest {
    @Mock
    private SpecificationProviderManager<Rental> specificationProviderManager;
    @Mock
    private Root<Rental> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder criteriaBuilder;
    @Mock
    private Predicate expectedPredicate;
    @Mock
    private SpecificationProvider<Rental> specificationProvider;
    @InjectMocks
    private RentalSpecificationBuilder builder;

    @Test
    @DisplayName("Verify the build() method works with search by userId.")
    public void build_SearchByUserId_ReturnsBookSpecification() {
        // Given
        Long value = 1L;
        Specification<Rental> expectedSpecification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(KEY_USER_ID), value);

        when(specificationProviderManager.getSpecificationProvider(KEY_USER_ID))
                .thenReturn(specificationProvider);
        when(specificationProvider.getSpecification(value)).thenReturn(expectedSpecification);
        when(criteriaBuilder.equal(root.get(KEY_USER_ID), value)).thenReturn(expectedPredicate);

        // When
        Specification<Rental> actualSpecification = builder.build(value, null);

        // Then
        assertThat(actualSpecification).isNotNull();
        Predicate actualPredicate = actualSpecification.toPredicate(root, query, criteriaBuilder);
        assertThat(actualPredicate).isNotNull();
        assertThat(actualPredicate).isEqualTo(expectedPredicate);
        verify(specificationProviderManager, times(1)).getSpecificationProvider(KEY_USER_ID);
        verify(specificationProvider, times(1)).getSpecification(value);
        verifyNoMoreInteractions(specificationProviderManager, specificationProvider);
    }

    @Test
    @DisplayName("Verify the build() method works with search by active.")
    public void build_SearchByAuthor_ReturnsBookSpecification() {
        // Given
        String key = "is_active";
        Boolean value = true;
        Specification<Rental> expectedSpecification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(key), value);

        when(specificationProviderManager.getSpecificationProvider(key))
                .thenReturn(specificationProvider);
        when(specificationProvider.getSpecification(value)).thenReturn(expectedSpecification);
        when(criteriaBuilder.equal(root.get(key), value)).thenReturn(expectedPredicate);

        // When
        Specification<Rental> actualSpecification = builder.build(null, value);

        // Then
        assertThat(actualSpecification).isNotNull();
        Predicate actualPredicate = actualSpecification.toPredicate(root, query, criteriaBuilder);
        assertThat(actualPredicate).isNotNull();
        assertThat(actualPredicate).isEqualTo(expectedPredicate);
        verify(specificationProviderManager, times(1)).getSpecificationProvider(key);
        verify(specificationProvider, times(1)).getSpecification(value);
        verifyNoMoreInteractions(specificationProviderManager, specificationProvider);
    }

    @Test
    @DisplayName("Verify that the method returns empty specification."
            + "when a all parameters are null.")
    public void build_AllParamsNull_ReturnsEmptySpecification() {
        // When
        Specification<Rental> actual = builder.build(null, null);

        // Then
        assertThat(actual).isEqualTo(Specification.where(null));
    }
}
