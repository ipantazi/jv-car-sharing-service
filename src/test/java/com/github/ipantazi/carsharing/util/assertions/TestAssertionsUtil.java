package com.github.ipantazi.carsharing.util.assertions;

import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MvcResult;

public class TestAssertionsUtil {
    protected TestAssertionsUtil() {
    }

    public static void assertObjectsAreEqualIgnoringFields(Object actual,
                                                           Object expected,
                                                           String... fieldsToIgnore) {
        assertThat(actual).isNotNull();
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields(fieldsToIgnore)
                .withComparatorForType(
                        (BigDecimal b1, BigDecimal b2) -> {
                            if (b1 == null && b2 == null) {
                                return 0;
                            }
                            if (b1 == null) {
                                return -1;
                            }
                            if (b2 == null) {
                                return 1;
                            }
                            return b1.compareTo(b2);
                        },
                        BigDecimal.class
                )
                .isEqualTo(expected);
    }

    public static <T> void assertCollectionsAreEqualIgnoringFields(Collection<T> actual,
                                                               Collection<T> expected,
                                                               String... fieldsToIgnore) {
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields(fieldsToIgnore)
                .withComparatorForType(
                        (BigDecimal b1, BigDecimal b2) -> {
                            if (b1 == null && b2 == null) {
                                return 0;
                            }
                            if (b1 == null) {
                                return -1;
                            }
                            if (b2 == null) {
                                return 1;
                            }
                            return b1.compareTo(b2);
                        },
                        BigDecimal.class
                )
                .ignoringCollectionOrder()
                .isEqualTo(expected);
    }

    public static void assertValidationError(MvcResult result,
                                             ObjectMapper objectMapper,
                                             int expectedStatus,
                                             String expectedMessage) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(body.get("status").asInt()).isEqualTo(expectedStatus);
        assertThat(body.get("message").asText()).isEqualTo(expectedMessage);
        assertThat(body.get("timestamp").asText()).isNotBlank();
    }

    public static void assertValidationErrorList(MvcResult result,
                                                 ObjectMapper objectMapper,
                                                 List<String> expectedMessages) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        JsonNode errors = body.get("message");
        assertThat(errors).isNotNull();
        List<String> actualErrorMessages = new ArrayList<>();
        errors.forEach(error -> actualErrorMessages.add(error.asText()));

        assertThat(body.get("status").asInt()).isEqualTo(BAD_REQUEST);
        assertThat(body.get("timestamp").asText()).isNotBlank();
        assertThat(actualErrorMessages).containsExactlyInAnyOrderElementsOf(expectedMessages);
    }

    public static <T> void assertPageMetadataEquals(Page<T> actual, Page<?> expected) {
        assertThat(actual.getTotalElements()).isEqualTo(expected.getTotalElements());
        assertThat(actual.getSize()).isEqualTo(expected.getSize());
        assertThat(actual.getSort()).isEqualTo(expected.getSort());
        assertThat(actual.getNumber()).isEqualTo(expected.getNumber());
    }

    public static void assertPageMetadataEquals(MvcResult result,
                                          ObjectMapper objectMapper,
                                          Pageable expectedPageable,
                                          int expectedTotalElements) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

        int actualPageNumber = root.get("number").asInt();
        int actualPageSize = root.get("size").asInt();
        int actualTotalPages = root.get("totalPages").asInt();
        long actualTotalElements = root.get("totalElements").asLong();

        int expectedPageNumber = expectedPageable.getPageNumber();
        int expectedPageSize = expectedPageable.getPageSize();

        int expectedTotalPages = (int) Math.ceil((double) expectedTotalElements / expectedPageSize);

        assertThat(actualPageNumber).isEqualTo(expectedPageNumber);
        assertThat(actualPageSize).isEqualTo(expectedPageSize);
        assertThat(actualTotalElements).isEqualTo(expectedTotalElements);
        assertThat(actualTotalPages).isEqualTo(expectedTotalPages);
    }
}
