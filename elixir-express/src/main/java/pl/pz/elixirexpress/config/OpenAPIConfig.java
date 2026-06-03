package pl.pz.elixirexpress.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Elixir Express – System Szybkich Przelewów")
                        .version("1.0.0")
                        .description("API dla banków uczestniczących w systemie Elixir Express. Umożliwia natychmiastowe wysyłanie przelewów, sprawdzanie statusów oraz obsługę zdarzeń awaryjnych (gridlock/emergency)."));
    }
}