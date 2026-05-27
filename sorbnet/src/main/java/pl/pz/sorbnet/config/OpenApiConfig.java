package pl.pz.sorbnet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sorbnetOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SORBNet API")
                        .version("1.0.0")
                        .description("""
                            API systemu SORBNet symulującego międzybankowy system RTGS.

                            Zakres dokumentacji obejmuje:
                            - inicjację i status przelewów międzybankowych,
                            - monitoring rachunków rozliczeniowych i płynności banków,
                            - działania operatorskie, w tym blokady banków, sytuacje nadzwyczajne i kolejkę gridlock.

                            API jest wykorzystywane przez:
                            - GUI operatora systemu,
                            - GUI pracownika banku,
                            - zespoły integrujące system bankowy z modułem RTGS.

                            Komunikacja wewnętrzna z systemami Elixir i Express Elixir odbywa się asynchronicznie
                            przez Kafka i jest dokumentowana osobno poza OpenAPI.
                            """)
                        .contact(new Contact()
                                .name("Zespół SORBNet")
                                .email("sorbnet@local.dev"))
                        .license(new License()
                                .name("Internal project documentation")
                                .url("https://example.local")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Lokalne środowisko developerskie")
                ));
    }
}