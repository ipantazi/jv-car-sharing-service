package com.github.ipantazi.carsharing.util.controller;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class MvcTestHelper {
    private MvcTestHelper() {
    }

    public static MvcResult createJsonMvcResult(MockMvc mockMvc,
                                                MockHttpServletRequestBuilder requestBuilder,
                                                ResultMatcher expectedStatus,
                                                String jsonRequest) throws Exception {
        return mockMvc.perform(requestBuilder
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(expectedStatus)
                .andReturn();
    }

    public static MvcResult createMvcResult(MockMvc mockMvc,
                                      MockHttpServletRequestBuilder requestBuilder,
                                      ResultMatcher expectedStatus) throws Exception {
        return mockMvc.perform(requestBuilder)
                .andExpect(expectedStatus)
                .andReturn();
    }
}
