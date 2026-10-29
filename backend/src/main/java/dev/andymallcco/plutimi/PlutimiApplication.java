package dev.andymallcco.plutimi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PlutimiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlutimiApplication.class, args);
    }

}
