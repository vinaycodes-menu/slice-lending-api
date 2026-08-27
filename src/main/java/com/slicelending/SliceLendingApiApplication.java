package com.slicelending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SliceLendingApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SliceLendingApiApplication.class, args);
    }

}


