package com.github.ipantazi.carsharing.util.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public class MockMvcUtil {
    private MockMvcUtil() {
    }

    public static MockMvc buildMockMvc(WebApplicationContext applicationContext) {
        return MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }
}
