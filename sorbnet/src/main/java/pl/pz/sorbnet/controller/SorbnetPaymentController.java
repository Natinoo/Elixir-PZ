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
    name = "SORBNet Payments ISO 20022",
    description = """
        XML-only API systemu SORBNet zgodne ze standardem ISO 20022.
        Zlecenia przelewów przyjmowane są jako komunikaty pacs.008-style
        (FIToFICstmrCdtTrf), a odpowiedzi zwracane jako raporty statusu
        pain.002-style (CstmrPmtStsRpt). Wszystkie komunikaty mają element
        główny Document.
        """
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
        summary = "Wyślij przelew SORBNet (pacs.008)",
        description = """
            Endpoint przyjmuje zlecenie przelewu wyłącznie jako komunikat ISO 20022
            pacs.008-style z elementem głównym Document. Identyfikacja banków odbywa się
            przez BICFI (DbtrAgt/CdtrAgt), rachunków przez IBAN (DbtrAcct/CdtrAcct),
            a serwis źródłowy przekazywany jest w SplmtryData/Envlp/ServiceCode
            (SORBNET, ELIXIR lub ELIXIR_EXPRESS).
            Odpowiedź zwracana jest jako raport statusu pain.002-style (CstmrPmtStsRpt).
            Możliwe statusy (TxSts) to SETTLED, REJECTED oraz GRIDLOCK_HELD.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Przelew został przetworzony i zwrócono raport statusu ISO 20022.",
            content = @Content(
                mediaType = MediaType.APPLICATION_XML_VALUE,
                schema = @Schema(implementation = PaymentResponseDto.class),
                examples = {
                    @ExampleObject(
                        name = "Rozliczony (SETTLED)",
                        value = """
                            <Document>
                                <CstmrPmtStsRpt>
                                    <GrpHdr>
                                        <MsgId>RESP-SORB-20260611-0001</MsgId>
                                        <CreDtTm>2026-06-11T13:00:01</CreDtTm>
                                    </GrpHdr>
                                    <OrgnlPmtInfAndSts>
                                        <OrgnlPmtInfId>SORB-20260611-0001</OrgnlPmtInfId>
                                        <TxInfAndSts>
                                            <OrgnlInstrId>SORB-20260611-0001</OrgnlInstrId>
                                            <OrgnlTxId>SORB-20260611-0001</OrgnlTxId>
                                            <TxSts>SETTLED</TxSts>
                                            <StsRsnInf>
                                                <AddtlInf>Przelew został rozliczony</AddtlInf>
                                            </StsRsnInf>
                                            <OrgnlTxRef>
                                                <IntrBkSttlmAmt Ccy="PLN">1000000.00</IntrBkSttlmAmt>
                                                <DbtrAgt><FinInstnId><BICFI>BANK_A</BICFI></FinInstnId></DbtrAgt>
                                                <CdtrAgt><FinInstnId><BICFI>BANK_B</BICFI></FinInstnId></CdtrAgt>
                                                <DbtrAcct><Id><IBAN>SORBNET-A-00000000000000000001</IBAN></Id></DbtrAcct>
                                                <CdtrAcct><Id><IBAN>SORBNET-B-00000000000000000002</IBAN></Id></CdtrAcct>
                                                <SplmtryData><Envlp><SourceServiceCode>SORBNET</SourceServiceCode></Envlp></SplmtryData>
                                            </OrgnlTxRef>
                                            <SettledAt>2026-06-11T13:00:01</SettledAt>
                                        </TxInfAndSts>
                                    </OrgnlPmtInfAndSts>
                                </CstmrPmtStsRpt>
                            </Document>
                            """
                    ),
                    @ExampleObject(
                        name = "Gridlock (GRIDLOCK_HELD)",
                        value = """
                            <Document>
                                <CstmrPmtStsRpt>
                                    <GrpHdr>
                                        <MsgId>RESP-SORB-20260611-0002</MsgId>
                                        <CreDtTm>2026-06-11T13:05:00</CreDtTm>
                                    </GrpHdr>
                                    <OrgnlPmtInfAndSts>
                                        <OrgnlPmtInfId>SORB-20260611-0002</OrgnlPmtInfId>
                                        <TxInfAndSts>
                                            <OrgnlInstrId>SORB-20260611-0002</OrgnlInstrId>
                                            <OrgnlTxId>SORB-20260611-0002</OrgnlTxId>
                                            <TxSts>GRIDLOCK_HELD</TxSts>
                                            <StsRsnInf>
                                                <AddtlInf>Payment held in gridlock queue</AddtlInf>
                                            </StsRsnInf>
                                            <OrgnlTxRef>
                                                <IntrBkSttlmAmt Ccy="PLN">8000000.00</IntrBkSttlmAmt>
                                                <DbtrAgt><FinInstnId><BICFI>BANK_A</BICFI></FinInstnId></DbtrAgt>
                                                <CdtrAgt><FinInstnId><BICFI>BANK_C</BICFI></FinInstnId></CdtrAgt>
                                                <DbtrAcct><Id><IBAN>SORBNET-A-00000000000000000001</IBAN></Id></DbtrAcct>
                                                <CdtrAcct><Id><IBAN>SORBNET-C-00000000000000000003</IBAN></Id></CdtrAcct>
                                                <SplmtryData><Envlp><SourceServiceCode>SORBNET</SourceServiceCode></Envlp></SplmtryData>
                                            </OrgnlTxRef>
                                        </TxInfAndSts>
                                    </OrgnlPmtInfAndSts>
                                </CstmrPmtStsRpt>
                            </Document>
                            """
                    ),
                    @ExampleObject(
                        name = "Odrzucony — bank zablokowany (REJECTED)",
                        value = """
                            <Document>
                                <CstmrPmtStsRpt>
                                    <GrpHdr>
                                        <MsgId>RESP-SORB-20260611-0003</MsgId>
                                        <CreDtTm>2026-06-11T13:10:00</CreDtTm>
                                    </GrpHdr>
                                    <OrgnlPmtInfAndSts>
                                        <OrgnlPmtInfId>SORB-20260611-0003</OrgnlPmtInfId>
                                        <TxInfAndSts>
                                            <OrgnlInstrId>SORB-20260611-0003</OrgnlInstrId>
                                            <OrgnlTxId>SORB-20260611-0003</OrgnlTxId>
                                            <TxSts>REJECTED</TxSts>
                                            <StsRsnInf>
                                                <AddtlInf>SENDER_BLOCKED</AddtlInf>
                                            </StsRsnInf>
                                            <OrgnlTxRef>
                                                <IntrBkSttlmAmt Ccy="PLN">500000.00</IntrBkSttlmAmt>
                                                <DbtrAgt><FinInstnId><BICFI>BANK_A</BICFI></FinInstnId></DbtrAgt>
                                                <CdtrAgt><FinInstnId><BICFI>BANK_B</BICFI></FinInstnId></CdtrAgt>
                                                <DbtrAcct><Id><IBAN>SORBNET-A-00000000000000000001</IBAN></Id></DbtrAcct>
                                                <CdtrAcct><Id><IBAN>SORBNET-B-00000000000000000002</IBAN></Id></CdtrAcct>
                                                <SplmtryData><Envlp><SourceServiceCode>SORBNET</SourceServiceCode></Envlp></SplmtryData>
                                            </OrgnlTxRef>
                                        </TxInfAndSts>
                                    </OrgnlPmtInfAndSts>
                                </CstmrPmtStsRpt>
                            </Document>
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Niepoprawna struktura komunikatu ISO 20022 (np. brak elementu Document/FIToFICstmrCdtTrf) lub błędne dane wejściowe.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nie znaleziono wskazanego banku (BICFI) lub rachunku (IBAN).",
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
                description = "Zlecenie przelewu w formacie ISO 20022 pacs.008-style (root Document).",
                content = @Content(
                    mediaType = MediaType.APPLICATION_XML_VALUE,
                    schema = @Schema(implementation = SorbnetPaymentDto.class),
                    examples = {
                        @ExampleObject(
                            name = "BANK_A → BANK_B (klientowski, z danymi stron)",
                            description = "IBAN-y pominięte — SORBNET sam dociąga domyślne rachunki banków. Bank zlecający może podać Dbtr/Cdtr z imieniem i nazwiskiem klientów.",
                            value = """
                                <Document>
                                    <FIToFICstmrCdtTrf>
                                        <GrpHdr>
                                            <MsgId>SORB-20260612-1001</MsgId>
                                            <CreDtTm>2026-06-12T13:00:00</CreDtTm>
                                            <NbOfTxs>1</NbOfTxs>
                                            <TtlIntrBkSttlmAmt Ccy="PLN">1000000.00</TtlIntrBkSttlmAmt>
                                            <SttlmInf>
                                                <SttlmMtd>CLRG</SttlmMtd>
                                                <ClrSys><Cd>SORBNET</Cd></ClrSys>
                                            </SttlmInf>
                                        </GrpHdr>
                                        <CdtTrfTxInf>
                                            <PmtId>
                                                <InstrId>SORB-20260612-1001</InstrId>
                                                <EndToEndId>SORB-20260612-1001</EndToEndId>
                                                <TxId>SORB-20260612-1001</TxId>
                                            </PmtId>
                                            <IntrBkSttlmAmt Ccy="PLN">1000000.00</IntrBkSttlmAmt>
                                            <Dbtr><Nm>Jan Kowalski</Nm></Dbtr>
                                            <DbtrAgt><FinInstnId><BICFI>BANK_A</BICFI></FinInstnId></DbtrAgt>
                                            <Cdtr><Nm>Anna Nowak</Nm></Cdtr>
                                            <CdtrAgt><FinInstnId><BICFI>BANK_B</BICFI></FinInstnId></CdtrAgt>
                                            <RmtInf><Ustrd>Rozrachunek międzybankowy</Ustrd></RmtInf>
                                            <SplmtryData>
                                                <Envlp>
                                                    <ServiceCode>SORBNET</ServiceCode>
                                                    <SenderBankId>BANK_A</SenderBankId>
                                                    <ReceiverBankId>BANK_B</ReceiverBankId>
                                                </Envlp>
                                            </SplmtryData>
                                        </CdtTrfTxInf>
                                    </FIToFICstmrCdtTrf>
                                </Document>
                                """
                        ),
                        @ExampleObject(
                            name = "BANK_A → BANK_B (standardowy)",
                            value = """
                                <Document>
                                    <FIToFICstmrCdtTrf>
                                        <GrpHdr>
                                            <MsgId>SORB-20260611-0001</MsgId>
                                            <CreDtTm>2026-06-11T13:00:00</CreDtTm>
                                            <NbOfTxs>1</NbOfTxs>
                                            <TtlIntrBkSttlmAmt Ccy="PLN">1000000.00</TtlIntrBkSttlmAmt>
                                            <SttlmInf>
                                                <SttlmMtd>CLRG</SttlmMtd>
                                                <ClrSys><Cd>SORBNET</Cd></ClrSys>
                                            </SttlmInf>
                                        </GrpHdr>
                                        <CdtTrfTxInf>
                                            <PmtId>
                                                <InstrId>SORB-20260611-0001</InstrId>
                                                <EndToEndId>SORB-20260611-0001</EndToEndId>
                                                <TxId>SORB-20260611-0001</TxId>
                                            </PmtId>
                                            <IntrBkSttlmAmt Ccy="PLN">1000000.00</IntrBkSttlmAmt>
                                            <DbtrAcct><Id><IBAN>SORBNET-A-00000000000000000001</IBAN></Id></DbtrAcct>
                                            <DbtrAgt><FinInstnId><BICFI>BANK_A</BICFI></FinInstnId></DbtrAgt>
                                            <CdtrAcct><Id><IBAN>SORBNET-B-00000000000000000002</IBAN></Id></CdtrAcct>
                                            <CdtrAgt><FinInstnId><BICFI>BANK_B</BICFI></FinInstnId></CdtrAgt>
                                            <RmtInf><Ustrd>Rozrachunek międzybankowy</Ustrd></RmtInf>
                                            <SplmtryData>
                                                <Envlp>
                                                    <ServiceCode>SORBNET</ServiceCode>
                                                    <SenderBankId>BANK_A</SenderBankId>
                                                    <ReceiverBankId>BANK_B</ReceiverBankId>
                                                </Envlp>
                                            </SplmtryData>
                                        </CdtTrfTxInf>
                                    </FIToFICstmrCdtTrf>
                                </Document>
                                """
                        ),
                        @ExampleObject(
                            name = "BANK_A → BANK_C (gridlock — kwota powyżej limitu)",
                            value = """
                                <Document>
                                    <FIToFICstmrCdtTrf>
                                        <GrpHdr>
                                            <MsgId>SORB-20260611-0002</MsgId>
                                            <CreDtTm>2026-06-11T13:05:00</CreDtTm>
                                            <NbOfTxs>1</NbOfTxs>
                                            <TtlIntrBkSttlmAmt Ccy="PLN">8000000.00</TtlIntrBkSttlmAmt>
                                            <SttlmInf>
                                                <SttlmMtd>CLRG</SttlmMtd>
                                                <ClrSys><Cd>SORBNET</Cd></ClrSys>
                                            </SttlmInf>
                                        </GrpHdr>
                                        <CdtTrfTxInf>
                                            <PmtId>
                                                <InstrId>SORB-20260611-0002</InstrId>
                                                <EndToEndId>SORB-20260611-0002</EndToEndId>
                                                <TxId>SORB-20260611-0002</TxId>
                                            </PmtId>
                                            <IntrBkSttlmAmt Ccy="PLN">8000000.00</IntrBkSttlmAmt>
                                            <DbtrAcct><Id><IBAN>SORBNET-A-00000000000000000001</IBAN></Id></DbtrAcct>
                                            <DbtrAgt><FinInstnId><BICFI>BANK_A</BICFI></FinInstnId></DbtrAgt>
                                            <CdtrAcct><Id><IBAN>SORBNET-C-00000000000000000003</IBAN></Id></CdtrAcct>
                                            <CdtrAgt><FinInstnId><BICFI>BANK_C</BICFI></FinInstnId></CdtrAgt>
                                            <RmtInf><Ustrd>Duży przelew rozrachunkowy</Ustrd></RmtInf>
                                            <SplmtryData>
                                                <Envlp>
                                                    <ServiceCode>SORBNET</ServiceCode>
                                                    <SenderBankId>BANK_A</SenderBankId>
                                                    <ReceiverBankId>BANK_C</ReceiverBankId>
                                                </Envlp>
                                            </SplmtryData>
                                        </CdtTrfTxInf>
                                    </FIToFICstmrCdtTrf>
                                </Document>
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
            Każda pozycja listy jest raportem statusu ISO 20022 (CstmrPmtStsRpt).
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
                    name = "Historia ISO 20022",
                    value = """
                        <Payments>
                            <payment>
                                <CstmrPmtStsRpt>
                                    <GrpHdr>
                                        <MsgId>RESP-SORB-20260611-0001</MsgId>
                                        <CreDtTm>2026-06-11T13:00:01</CreDtTm>
                                    </GrpHdr>
                                    <OrgnlPmtInfAndSts>
                                        <OrgnlPmtInfId>SORB-20260611-0001</OrgnlPmtInfId>
                                        <TxInfAndSts>
                                            <OrgnlInstrId>SORB-20260611-0001</OrgnlInstrId>
                                            <OrgnlTxId>SORB-20260611-0001</OrgnlTxId>
                                            <TxSts>SETTLED</TxSts>
                                            <StsRsnInf><AddtlInf>Przelew został rozliczony</AddtlInf></StsRsnInf>
                                            <OrgnlTxRef>
                                                <IntrBkSttlmAmt Ccy="PLN">1000000.00</IntrBkSttlmAmt>
                                                <DbtrAgt><FinInstnId><BICFI>BANK_A</BICFI></FinInstnId></DbtrAgt>
                                                <CdtrAgt><FinInstnId><BICFI>BANK_B</BICFI></FinInstnId></CdtrAgt>
                                                <DbtrAcct><Id><IBAN>SORBNET-A-00000000000000000001</IBAN></Id></DbtrAcct>
                                                <CdtrAcct><Id><IBAN>SORBNET-B-00000000000000000002</IBAN></Id></CdtrAcct>
                                                <SplmtryData><Envlp><SourceServiceCode>SORBNET</SourceServiceCode></Envlp></SplmtryData>
                                            </OrgnlTxRef>
                                            <SettledAt>2026-06-11T13:00:01</SettledAt>
                                        </TxInfAndSts>
                                    </OrgnlPmtInfAndSts>
                                </CstmrPmtStsRpt>
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
        description = "Zwraca szczegóły pojedynczego przelewu jako raport statusu ISO 20022 (CstmrPmtStsRpt) w XML."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Znaleziono przelew i zwrócono jego szczegóły w XML.",
            content = @Content(
                mediaType = MediaType.APPLICATION_XML_VALUE,
                schema = @Schema(implementation = PaymentResponseDto.class),
                examples = @ExampleObject(
                    name = "Szczegóły przelewu ISO 20022",
                    value = """
                        <Document>
                            <CstmrPmtStsRpt>
                                <GrpHdr>
                                    <MsgId>RESP-SORB-20260611-0001</MsgId>
                                    <CreDtTm>2026-06-11T13:00:01</CreDtTm>
                                </GrpHdr>
                                <OrgnlPmtInfAndSts>
                                    <OrgnlPmtInfId>SORB-20260611-0001</OrgnlPmtInfId>
                                    <TxInfAndSts>
                                        <OrgnlInstrId>SORB-20260611-0001</OrgnlInstrId>
                                        <OrgnlTxId>SORB-20260611-0001</OrgnlTxId>
                                        <TxSts>SETTLED</TxSts>
                                        <StsRsnInf><AddtlInf>Przelew został rozliczony</AddtlInf></StsRsnInf>
                                        <OrgnlTxRef>
                                            <IntrBkSttlmAmt Ccy="PLN">1000000.00</IntrBkSttlmAmt>
                                            <DbtrAgt><FinInstnId><BICFI>BANK_A</BICFI></FinInstnId></DbtrAgt>
                                            <CdtrAgt><FinInstnId><BICFI>BANK_B</BICFI></FinInstnId></CdtrAgt>
                                            <DbtrAcct><Id><IBAN>SORBNET-A-00000000000000000001</IBAN></Id></DbtrAcct>
                                            <CdtrAcct><Id><IBAN>SORBNET-B-00000000000000000002</IBAN></Id></CdtrAcct>
                                            <SplmtryData><Envlp><SourceServiceCode>SORBNET</SourceServiceCode></Envlp></SplmtryData>
                                        </OrgnlTxRef>
                                        <SettledAt>2026-06-11T13:00:01</SettledAt>
                                    </TxInfAndSts>
                                </OrgnlPmtInfAndSts>
                            </CstmrPmtStsRpt>
                        </Document>
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
                description = "Unikalny identyfikator przelewu (OrgnlTxId).",
                example = "SORB-20260611-0001"
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
        response.setSourceServiceCode(requestDto.getType() != null ? requestDto.getType() : "SORBNET");

        Object amount = result.get("amount");
        if (amount instanceof BigDecimal bd) {
            response.setAmount(bd);
        } else {
            response.setAmount(requestDto.getAmount());
        }
        response.setCurrency(requestDto.getCurrency());

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
        dto.setCurrency(payment.getCurrency());
        dto.setSourceServiceCode(payment.getSourceService() != null ? payment.getSourceService() : "SORBNET");
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