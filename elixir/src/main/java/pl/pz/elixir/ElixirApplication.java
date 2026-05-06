package pl.pz.elixir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElixirApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElixirApplication.class, args);
    }
}