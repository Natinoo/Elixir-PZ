package pl.pz.sorbnet.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JacksonXmlRootElement(localName = "Payments")
@Schema(
    name = "Payments",
    description = "Lista płatności zwracana jako odpowiedź XML."
)
public class PaymentListResponseDto {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "payment")
    @Schema(description = "Elementy listy płatności.")
    private List<PaymentResponseDto> payments;

    public PaymentListResponseDto() {
    }

    public PaymentListResponseDto(List<PaymentResponseDto> payments) {
        this.payments = payments;
    }

    public List<PaymentResponseDto> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentResponseDto> payments) {
        this.payments = payments;
    }
}