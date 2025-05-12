package com.github.ipantazi.carsharing;

import org.springframework.boot.SpringApplication;

public class TestJvCarSharingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(Application::main).with(TestcontainersConfiguration.class).run(args);
    }

}
