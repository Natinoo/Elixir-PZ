package pl.pz.sorbnet.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "Payments")
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(
        name = "Payments",
        description = "Lista przelewów zwracana przez endpoint historii płatności."
)
public class PaymentListResponseDto {

    @XmlElement(name = "payment")
    @ArraySchema(
            arraySchema = @Schema(description = "Lista przelewów spełniających kryteria wyszukiwania."),
            schema = @Schema(implementation = PaymentResponseDto.class)
    )
    private List<PaymentResponseDto> payments = new ArrayList<>();

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