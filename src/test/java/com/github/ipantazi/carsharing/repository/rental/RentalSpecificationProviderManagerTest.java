package com.github.ipantazi.carsharing.repository.rental;

import static com.github.ipantazi.carsharing.util.repository.RepositoryTestDataUtil.INVALID_KEY;
import static com.github.ipantazi.carsharing.util.repository.RepositoryTestDataUtil.KEY_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.exception.DataProcessingException;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.SpecificationProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RentalSpecificationProviderManagerTest {
    @Mock
    private SpecificationProvider<Rental> expectedProvider;
    private RentalSpecificationProviderManager manager;

    @BeforeEach
    void setUp() {
        List<SpecificationProvider<Rental>> providers = List.of(expectedProvider);
        manager = new RentalSpecificationProviderManager(providers);

        when(expectedProvider.getKey()).thenReturn(KEY_USER_ID);
    }

    @Test
    @DisplayName("Verify getSpecificationProviders() method works.")
    public void getSpecificationProviders_ValidKey_ReturnsSpecificationProviders() {
        // When
        SpecificationProvider<Rental> actual = manager.getSpecificationProvider(KEY_USER_ID);

        // Then
        assertThat(actual).isNotNull();
        assertThat(actual.getKey()).isEqualTo(KEY_USER_ID);
        assertThat(actual).isEqualTo(expectedProvider);
        verify(expectedProvider, times(2)).getKey();
    }

    @Test
    @DisplayName("Verify that an exception is throw when sort key is invalid.")
    public void getSpecificationProviders_InvalidKey_ThrowsException() {
        // When
        assertThatThrownBy(() -> manager.getSpecificationProvider(INVALID_KEY))
                .isInstanceOf(DataProcessingException.class)
                .hasMessage("Can't find correct specification provider for key: " + INVALID_KEY);

        // Then
        verify(expectedProvider, times(1)).getKey();
    }
}
