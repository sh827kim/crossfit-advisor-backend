package org.spark.crossfit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(value = org.spark.crossfit.config.CustomApplicationConfig.class)
@SpringBootApplication
public class CrossfitAdvisorBackendApplication {

    static void main(String[] args) {
        SpringApplication.run(CrossfitAdvisorBackendApplication.class, args);
    }

}
