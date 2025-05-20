package com.github.ipantazi.carsharing.util.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class ControllerTestUtil {
    private ControllerTestUtil() {
    }

    public static <T> List<T> parsePageContent(MvcResult result,
                                               ObjectMapper objectMapper,
                                               TypeReference<List<T>> typeRef) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return objectMapper.readValue(root.get("content").toString(), typeRef);
    }

    public static List<?> parseResponseToList(MvcResult result,
                                                    ObjectMapper objectMapper) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
    }

    public static <T> T parseResponseToObject(MvcResult result,
                                      ObjectMapper objectMapper,
                                      Class<T> clazz) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                clazz
        );
    }

    public static MockHttpServletRequestBuilder createRequestWithPageable(
            String url,
            Pageable pageable
    ) {
        MockHttpServletRequestBuilder builder = get(url)
                .param("page", String.valueOf(pageable.getPageNumber()))
                .param("size", String.valueOf(pageable.getPageSize()));

        pageable.getSort().forEach(order -> {
            String sortParam = order.getProperty() + ","
                    + order.getDirection().name().toLowerCase();
            builder.param("sort", sortParam);
        });

        return builder;
    }

}
