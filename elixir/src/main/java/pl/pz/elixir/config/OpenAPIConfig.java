package pl.pz.elixir.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Elixir – System Rozliczeń Międzybankowych")
                        .version("1.0.0")
                        .description("API dla banków uczestniczących w systemie Elixir. Umożliwia wysyłanie przelewów, sprawdzanie statusów, zarządzanie płynnością oraz pobieranie raportów sesyjnych."));
    }
}