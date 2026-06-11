package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.ElixirPaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/elixir/payments")
@Tag(name = "Przelewy Elixir", description = "Zarządzanie przelewami w systemie Elixir w formacie ISO 20022 XML")
public class ElixirPaymentController {

    private static final Logger log = LoggerFactory.getLogger(ElixirPaymentController.class);
    private final ElixirPaymentService paymentService;

    public ElixirPaymentController(ElixirPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            summary = "Utwórz nowy przelew Elixir",
            description = "Przyjmuje uproszczony komunikat ISO 20022 pacs.008 i dodaje przelew do bieżącej sesji Elixir.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_XML_VALUE,
                            examples = @ExampleObject(
                                    name = "ISO 20022 request",
                                    value = """
                                            <Document>
                                                <FIToFICstmrCdtTrf>
                                                    <GrpHdr>
                                                        <MsgId>ELIX-20260606-0001</MsgId>
                                                        <CreDtTm>2026-06-06T10:00:00</CreDtTm>
                                                        <NbOfTxs>1</NbOfTxs>
                                                        <TtlIntrBkSttlmAmt Ccy=\"PLN\">1000.00</TtlIntrBkSttlmAmt>
                                                        <SttlmInf>
                                                            <SttlmMtd>CLRG</SttlmMtd>
                                                            <ClrSys><Cd>ELIXIR</Cd></ClrSys>
                                                        </SttlmInf>
                                                    </GrpHdr>
                                                    <CdtTrfTxInf>
                                                        <PmtId>
                                                            <InstrId>ELIX-20260606-0001</InstrId>
                                                            <EndToEndId>ELIX-20260606-0001</EndToEndId>
                                                            <TxId>ELIX-20260606-0001</TxId>
                                                        </PmtId>
                                                        <IntrBkSttlmAmt Ccy=\"PLN\">1000.00</IntrBkSttlmAmt>
                                                        <DbtrAgt><FinInstnId><BICFI>BANK_A</BICFI></FinInstnId></DbtrAgt>
                                                        <CdtrAgt><FinInstnId><BICFI>BANK_B</BICFI></FinInstnId></CdtrAgt>
                                                        <DbtrAcct><Id><IBAN>ANY</IBAN></Id></DbtrAcct>
                                                        <CdtrAcct><Id><IBAN>ANY</IBAN></Id></CdtrAcct>
                                                        <RmtInf><Ustrd>Przelew klientowski</Ustrd></RmtInf>
                                                        <SplmtryData>
                                                            <Envlp>
                                                                <ServiceCode>ELIXIR</ServiceCode>
                                                                <SenderBankId>BANK_A</SenderBankId>
                                                                <ReceiverBankId>BANK_B</ReceiverBankId>
                                                            </Envlp>
                                                        </SplmtryData>
                                                    </CdtTrfTxInf>
                                                </FIToFICstmrCdtTrf>
                                            </Document>
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Przelew przyjęty do sesji",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE)),
            @ApiResponse(responseCode = "400", description = "Błędne dane",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE))
    })
    public ResponseEntity<String> createPayment(@RequestBody ElixirPaymentDto paymentDto) {
        log.info("POST /api/elixir/payments senderBank={}, receiverBank={}, amount={}",
                paymentDto.getSenderBankId(), paymentDto.getReceiverBankId(), paymentDto.getAmount());
        try {
            return ResponseEntity.ok(paymentService.processPayment(paymentDto));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorXml(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorXml("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz wszystkie przelewy")
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping(value = "/queued", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy w kolejce")
    public List<Payment> queuedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.QUEUED);
    }

    @GetMapping(value = "/processed", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy przetworzone")
    public List<Payment> processedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.PROCESSED);
    }

    @GetMapping(value = "/blocked", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy zablokowane")
    public List<Payment> blockedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.BLOCKED);
    }

    @GetMapping(value = "/rejected", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy odrzucone")
    public List<Payment> rejectedPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.REJECTED);
    }

    @GetMapping(value = "/waiting-liquidity", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy oczekujące na uzupełnienie płynności")
    public List<Payment> waitingForLiquidityPayments() {
        return paymentService.getPaymentsByStatus(PaymentStatus.WAITING_FOR_LIQUIDITY);
    }

    private String errorXml(String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Error><message>" + escapeXml(message) + "</message></Error>";
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}