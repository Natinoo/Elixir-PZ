package pl.pz.sorbnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SorbnetApplication {

    public static void main(String[] args) {
        SpringApplication.run(SorbnetApplication.class, args);
    }
}