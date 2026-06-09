package pl.pz.sorbnet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pl.pz.sorbnet.dto.PaymentListResponseDto;
import pl.pz.sorbnet.dto.PaymentResponseDto;
import pl.pz.sorbnet.dto.SorbnetPaymentDto;
import pl.pz.sorbnet.model.Payment;
import pl.pz.sorbnet.repository.PaymentRepository;
import pl.pz.sorbnet.service.SorbnetPaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(
    value = "/api/sorbnet/payments",
    produces = MediaType.APPLICATION_XML_VALUE
)
@Tag(
    name = "SORBNet Payments XML",
    description = "XML-only API do wysyłania przelewów, pobierania historii oraz sprawdzania szczegółów płatności w systemie SORBNet."
)
public class SorbnetPaymentController {

    private final SorbnetPaymentService service;
    private final PaymentRepository paymentRepo;

    public SorbnetPaymentController(SorbnetPaymentService service,
                                    PaymentRepository paymentRepo) {
        this.service = service;
        this.paymentRepo = paymentRepo;
    }

    @Operation(
        summary = "Wyślij przelew SORBNet",
        description = """
            Endpoint przyjmuje zlecenie przelewu wyłącznie w formacie XML.
            Odpowiedź również zwracana jest wyłącznie jako XML.
            Możliwe statusy odpowiedzi to SETTLED, REJECTED oraz GRIDLOCK_HELD.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Przelew został przetworzony i zwrócono wynik operacji w XML.",
            content = @Content(
                mediaType = MediaType.APPLICATION_XML_VALUE,
                schema = @Schema(implementation = PaymentResponseDto.class),
                examples = {
                    @ExampleObject(
                        name = "Rozliczony",
                        value = """
                            <SorbnetPaymentResponse>
                                <paymentId>SORB-20260609-0001</paymentId>
                                <status>SETTLED</status>
                                <message>Przelew został rozliczony</message>
                                <senderBankId>BANK_A</senderBankId>
                                <receiverBankId>BANK_B</receiverBankId>
                                <senderAccount>11111100000000000000000001</senderAccount>
                                <receiverAccount>22222200000000000000000002</receiverAccount>
                                <amount>1000000.00</amount>
                                <settledAt>2026-06-09T13:00:00</settledAt>
                            </SorbnetPaymentResponse>
                            """
                    ),
                    @ExampleObject(
                        name = "Idempotent",
                        value = """
                            <SorbnetPaymentResponse>
                                <paymentId>SORB-20260609-0001</paymentId>
                                <status>SETTLED</status>
                                <message>Przelew już przetworzony (idempotent)</message>
                                <senderBankId>BANK_A</senderBankId>
                                <receiverBankId>BANK_B</receiverBankId>
                                <senderAccount>11111100000000000000000001</senderAccount>
                                <receiverAccount>22222200000000000000000002</receiverAccount>
                                <amount>1000000.00</amount>
                            </SorbnetPaymentResponse>
                            """
                    ),
                    @ExampleObject(
                        name = "Gridlock",
                        value = """
                            <SorbnetPaymentResponse>
                                <paymentId>SORB-20260609-0002</paymentId>
                                <status>GRIDLOCK_HELD</status>
                                <message>Przelew został wstrzymany w kolejce gridlock</message>
                                <senderBankId>BANK_A</senderBankId>
                                <receiverBankId>BANK_C</receiverBankId>
                                <senderAccount>11111100000000000000000001</senderAccount>
                                <receiverAccount>33333300000000000000000003</receiverAccount>
                                <amount>8000000.00</amount>
                            </SorbnetPaymentResponse>
                            """
                    ),
                    @ExampleObject(
                        name = "Odrzucony — bank zablokowany",
                        value = """
                            <SorbnetPaymentResponse>
                                <paymentId>SORB-20260609-0003</paymentId>
                                <status>REJECTED</status>
                                <message>SENDER_BLOCKED</message>
                                <senderBankId>BANK_A</senderBankId>
                                <receiverBankId>BANK_B</receiverBankId>
                                <senderAccount>11111100000000000000000001</senderAccount>
                                <receiverAccount>22222200000000000000000002</receiverAccount>
                                <amount>500000.00</amount>
                            </SorbnetPaymentResponse>
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Niepoprawna struktura XML lub błędne dane wejściowe.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nie znaleziono wskazanego banku lub rachunku.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Przelew nie może zostać rozliczony z przyczyn biznesowych.",
            content = @Content
        )
    })
    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<PaymentResponseDto> send(
            @RequestBody(
                required = true,
                description = "Żądanie przelewu w formacie XML.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_XML_VALUE,
                    schema = @Schema(implementation = SorbnetPaymentDto.class),
                    examples = {
                        @ExampleObject(
                            name = "BANK_A → BANK_B (standardowy)",
                            value = """
                                <SorbnetPaymentRequest>
                                    <paymentId>SORB-20260609-0001</paymentId>
                                    <amount>1000000.00</amount>
                                    <currency>PLN</currency>
                                    <senderBankId>BANK_A</senderBankId>
                                    <receiverBankId>BANK_B</receiverBankId>
                                    <senderAccount>11111100000000000000000001</senderAccount>
                                    <receiverAccount>22222200000000000000000002</receiverAccount>
                                    <title>Rozrachunek międzybankowy</title>
                                    <status>NEW</status>
                                </SorbnetPaymentRequest>
                                """
                        ),
                        @ExampleObject(
                            name = "BANK_A → BANK_C (gridlock — kwota powyżej limitu)",
                            value = """
                                <SorbnetPaymentRequest>
                                    <paymentId>SORB-20260609-0002</paymentId>
                                    <amount>8000000.00</amount>
                                    <currency>PLN</currency>
                                    <senderBankId>BANK_A</senderBankId>
                                    <receiverBankId>BANK_C</receiverBankId>
                                    <senderAccount>11111100000000000000000001</senderAccount>
                                    <receiverAccount>33333300000000000000000003</receiverAccount>
                                    <title>Duży przelew rozrachunkowy</title>
                                    <status>NEW</status>
                                </SorbnetPaymentRequest>
                                """
                        ),
                        @ExampleObject(
                            name = "BANK_B → BANK_C (rachunek pomocniczy)",
                            value = """
                                <SorbnetPaymentRequest>
                                    <paymentId>SORB-20260609-0003</paymentId>
                                    <amount>500000.00</amount>
                                    <currency>PLN</currency>
                                    <senderBankId>BANK_B</senderBankId>
                                    <receiverBankId>BANK_C</receiverBankId>
                                    <senderAccount>22222200000000000000000003</senderAccount>
                                    <receiverAccount>33333300000000000000000003</receiverAccount>
                                    <title>Przelew pomocniczy</title>
                                    <status>NEW</status>
                                </SorbnetPaymentRequest>
                                """
                        ),
                        @ExampleObject(
                            name = "NBP → BANK_A (dokapitalizowanie)",
                            value = """
                                <SorbnetPaymentRequest>
                                    <paymentId>SORB-20260609-0004</paymentId>
                                    <amount>3000000.00</amount>
                                    <currency>PLN</currency>
                                    <senderBankId>NBP</senderBankId>
                                    <receiverBankId>BANK_A</receiverBankId>
                                    <senderAccount>10100100000000000000000000</senderAccount>
                                    <receiverAccount>11111100000000000000000001</receiverAccount>
                                    <title>Zasilenie rachunku rozrachunkowego przez NBP</title>
                                    <status>NEW</status>
                                </SorbnetPaymentRequest>
                                """
                        )
                    }
                )
            )
            @org.springframework.web.bind.annotation.RequestBody SorbnetPaymentDto dto) {

        Map<String, Object> result = service.process(dto);
        return ResponseEntity.ok(mapProcessResultToResponse(result, dto));
    }

    @Operation(
        summary = "Pobierz historię przelewów banku",
        description = """
            Zwraca historię przelewów dla wskazanego banku wyłącznie w formacie XML.
            Jeżeli parametr from nie zostanie podany, zwracane są przelewy od początku bieżącego dnia.
            Zakres historii jest ograniczony maksymalnie do jednego miesiąca wstecz.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Historia przelewów została zwrócona poprawnie w XML.",
            content = @Content(
                mediaType = MediaType.APPLICATION_XML_VALUE,
                schema = @Schema(implementation = PaymentListResponseDto.class),
                examples = @ExampleObject(
                    name = "XML history response",
                    value = """
                        <Payments>
                            <payment>
                                <paymentId>SORB-20260609-0001</paymentId>
                                <status>SETTLED</status>
                                <message>Przelew został rozliczony</message>
                                <senderBankId>BANK_A</senderBankId>
                                <receiverBankId>BANK_B</receiverBankId>
                                <senderAccount>11111100000000000000000001</senderAccount>
                                <receiverAccount>22222200000000000000000002</receiverAccount>
                                <amount>1000000.00</amount>
                                <settledAt>2026-06-09T13:00:00</settledAt>
                            </payment>
                            <payment>
                                <paymentId>SORB-20260609-0002</paymentId>
                                <status>GRIDLOCK_HELD</status>
                                <message>Przelew oczekuje w kolejce gridlock</message>
                                <senderBankId>BANK_A</senderBankId>
                                <receiverBankId>BANK_C</receiverBankId>
                                <senderAccount>11111100000000000000000001</senderAccount>
                                <receiverAccount>33333300000000000000000003</receiverAccount>
                                <amount>8000000.00</amount>
                            </payment>
                        </Payments>
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Niepoprawne parametry zapytania, np. błędny format daty.",
            content = @Content
        )
    })
    @GetMapping
    public PaymentListResponseDto history(
            @Parameter(
                name = "bankId",
                in = ParameterIn.QUERY,
                required = true,
                description = "Identyfikator banku, dla którego pobierana jest historia przelewów.",
                example = "BANK_A"
            )
            @RequestParam String bankId,

            @Parameter(
                name = "from",
                in = ParameterIn.QUERY,
                description = "Data początkowa w formacie yyyy-MM-dd. Jeżeli brak, system zwraca przelewy od początku bieżącego dnia.",
                example = "2026-06-01"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxFrom = now.minusMonths(1);
        LocalDateTime start = from == null
                ? LocalDate.now().atStartOfDay()
                : from.atStartOfDay();

        if (start.isBefore(maxFrom)) {
            start = maxFrom;
        }

        List<PaymentResponseDto> payments = paymentRepo.findByBankIdAndFromBetween(bankId, start, now)
                .stream()
                .map(this::mapPaymentToResponse)
                .toList();

        return new PaymentListResponseDto(payments);
    }

    @Operation(
        summary = "Pobierz szczegóły przelewu",
        description = "Zwraca szczegóły pojedynczego przelewu wyłącznie w formacie XML."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Znaleziono przelew i zwrócono jego szczegóły w XML.",
            content = @Content(
                mediaType = MediaType.APPLICATION_XML_VALUE,
                schema = @Schema(implementation = PaymentResponseDto.class),
                examples = @ExampleObject(
                    name = "XML payment details",
                    value = """
                        <SorbnetPaymentResponse>
                            <paymentId>SORB-20260609-0001</paymentId>
                            <status>SETTLED</status>
                            <message>Przelew został rozliczony</message>
                            <senderBankId>BANK_A</senderBankId>
                            <receiverBankId>BANK_B</receiverBankId>
                            <senderAccount>11111100000000000000000001</senderAccount>
                            <receiverAccount>22222200000000000000000002</receiverAccount>
                            <amount>1000000.00</amount>
                            <settledAt>2026-06-09T13:00:00</settledAt>
                        </SorbnetPaymentResponse>
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nie znaleziono przelewu o podanym identyfikatorze.",
            content = @Content
        )
    })
    @GetMapping("/{paymentId}")
    public PaymentResponseDto getById(
            @Parameter(
                description = "Unikalny identyfikator przelewu.",
                example = "SORB-20260609-0001"
            )
            @PathVariable String paymentId) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Nie znaleziono przelewu: " + paymentId));

        return mapPaymentToResponse(payment);
    }

    private PaymentResponseDto mapProcessResultToResponse(Map<String, Object> result, SorbnetPaymentDto requestDto) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId((String) result.get("paymentId"));
        response.setStatus(String.valueOf(result.get("status")));
        response.setMessage((String) result.getOrDefault("message", result.getOrDefault("info", defaultMessage(String.valueOf(result.get("status"))))));
        response.setSenderBankId((String) result.getOrDefault("senderBankId", requestDto.getSenderBankId()));
        response.setReceiverBankId((String) result.getOrDefault("receiverBankId", requestDto.getReceiverBankId()));
        response.setSenderAccount(requestDto.getSenderAccount());
        response.setReceiverAccount(requestDto.getReceiverAccount());

        Object amount = result.get("amount");
        if (amount instanceof BigDecimal bd) {
            response.setAmount(bd);
        } else {
            response.setAmount(requestDto.getAmount());
        }

        Object settledAt = result.get("settledAt");
        if (settledAt != null) {
            response.setSettledAt(settledAt.toString());
        }

        return response;
    }

    private PaymentResponseDto mapPaymentToResponse(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setPaymentId(payment.getPaymentId());
        dto.setStatus(payment.getStatus() != null ? payment.getStatus().name() : null);
        dto.setMessage(resolvePaymentMessage(payment));
        dto.setSenderBankId(payment.getSenderBankId());
        dto.setSenderAccount(payment.getSenderAccount());
        dto.setReceiverAccount(payment.getReceiverAccount());
        dto.setReceiverBankId(payment.getReceiverBankId());
        dto.setAmount(payment.getAmount());
        dto.setSettledAt(payment.getSettledAt() != null ? payment.getSettledAt().toString() : null);
        return dto;
    }

    private String resolvePaymentMessage(Payment payment) {
        if (payment.getRejectionReason() != null && !payment.getRejectionReason().isBlank()) {
            return payment.getRejectionReason();
        }
        if (payment.getStatus() == null) {
            return "Brak informacji o statusie płatności";
        }
        return defaultMessage(payment.getStatus().name());
    }

    private String defaultMessage(String status) {
        return switch (status) {
            case "SETTLED"      -> "Przelew został rozliczony";
            case "GRIDLOCK_HELD"-> "Przelew oczekuje w kolejce gridlock";
            case "REJECTED"     -> "Przelew został odrzucony";
            default             -> "Brak dodatkowej informacji";
        };
    }
}