package pl.pz.sorbnet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS otwarty dla wszystkich originów — środowisko deweloperskie / piaskownica.
 * Bez uwierzytelniania: dowolny zespół (frontend w przeglądarce na dowolnym
 * porcie albo backend) może wołać API bez ograniczeń.
 *
 * allowedOriginPatterns("*") zamiast allowedOrigins("*") — wzorzec działa
 * też, gdyby kiedyś włączono allowCredentials; przy credentials=false i tak
 * jest to bezpieczny wybór dla lokalnego dema.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}