package pl.pz.sorbnet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Wymusza użycie JAXB do (de)serializacji XML w endpointach REST.
 *
 * Bez tego Spring Boot — mając na classpath jackson-dataformat-xml —
 * stawia MappingJackson2XmlHttpMessageConverter PRZED konwerterem JAXB.
 * Jackson ignoruje adnotacje @XmlElement i mapuje po nazwach pól Javy,
 * więc komunikat ISO 20022 (<FIToFICstmrCdtTrf> vs pole fiToFICstmrCdtTrf)
 * deserializuje się do pustego obiektu: wszystkie fasadowe gettery
 * zwracają null i pierwszy lookup w SorbnetPaymentService wybucha
 * błędem typu "Nieznany rachunek nadawcy: null".
 *
 * Konwerter JAXB na pozycji 0 obsługuje wszystkie klasy z @XmlRootElement
 * (SorbnetPaymentDto, PaymentResponseDto, PaymentListResponseDto);
 * Jackson nadal serializuje pozostałe typy (JSON dla GUI, mapy błędów).
 */
@Configuration
public class XmlConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new Jaxb2RootElementHttpMessageConverter());
    }
}