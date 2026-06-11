package pl.pz.elixir.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.model.Payment;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.repository.PaymentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ElixirPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ElixirPaymentService.class);
    private static final String SERVICE_CODE = BankLiquidityService.ELIXIR;
    private static final Set<String> ALLOWED_BANKS = Set.of("BANK_A", "BANK_B", "BANK_C");
    private static final Set<String> ALLOWED_CURRENCIES = Set.of("PLN");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentRepository paymentRepository;
    private final BankLiquidityService bankLiquidityService;

    public ElixirPaymentService(KafkaTemplate<String, String> kafkaTemplate,
                                PaymentRepository paymentRepository,
                                BankLiquidityService bankLiquidityService) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentRepository = paymentRepository;
        this.bankLiquidityService = bankLiquidityService;
        log.info("ElixirPaymentService initialized");
    }

    @Transactional
    public String processPayment(ElixirPaymentDto paymentDto) {
        if (paymentDto == null) {
            throw new IllegalArgumentException("Brak danych przelewu.");
        }

        paymentDto.setType(SERVICE_CODE);

        if (isBlank(paymentDto.getPaymentId())) {
            paymentDto.setPaymentId(UUID.randomUUID().toString());
        }

        validatePayment(paymentDto);

        paymentDto.ensureDefaults();

        Payment payment = new Payment(
                paymentDto.getPaymentId(),
                SERVICE_CODE,
                paymentDto.getSenderBankId(),
                paymentDto.getReceiverBankId(),
                paymentDto.getSenderAccount(),
                paymentDto.getReceiverAccount(),
                paymentDto.getAmount(),
                paymentDto.getCurrency(),
                paymentDto.getTitle(),
                PaymentStatus.QUEUED,
                LocalDateTime.now(),
                SERVICE_CODE,
                null
        );

        paymentRepository.save(payment);

        log.info(
                "Payment saved to DB: id={}, service={}, senderBank={}, receiverBank={}, amount={}",
                payment.getPaymentId(),
                payment.getServiceCode(),
                payment.getSenderBankId(),
                payment.getReceiverBankId(),
                payment.getAmount()
        );

        String payload = toPaymentIsoXml(paymentDto);
        kafkaTemplate.send("payments.elixir", paymentDto.getPaymentId(), payload);

        log.info("ISO 20022 payment sent to Kafka topic payments.elixir");

        return toAcceptedResponseXml(
                paymentDto.getPaymentId(),
                "ACSP",
                "Przelew przyjęty do sesji Elixir"
        );
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    @Transactional
    public void updatePaymentStatus(String paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);

        if (payment == null) {
            log.warn("Payment not found for status update: {}", paymentId);
            return;
        }

        payment.setStatus(status);
        paymentRepository.save(payment);

        log.info("Payment {} updated to status {}", paymentId, status);
    }

    @Transactional
    public void updatePaymentsStatus(List<String> paymentIds, PaymentStatus status) {
        for (String paymentId : paymentIds) {
            updatePaymentStatus(paymentId, status);
        }
    }

    @Transactional
    public void markWaitingForLiquidityAsQueued(String bankId) {
        List<Payment> payments = paymentRepository.findBySenderBankIdAndStatus(
                bankId,
                PaymentStatus.WAITING_FOR_LIQUIDITY
        );

        for (Payment payment : payments) {
            payment.setStatus(PaymentStatus.QUEUED);
            paymentRepository.save(payment);
        }

        log.info("Marked {} waiting payments as QUEUED for bank {}", payments.size(), bankId);
    }

    private void validatePayment(ElixirPaymentDto paymentDto) {
        if (isBlank(paymentDto.getSenderBankId())) {
            throw new IllegalArgumentException("Bank nadawcy jest wymagany.");
        }

        if (isBlank(paymentDto.getReceiverBankId())) {
            throw new IllegalArgumentException("Bank odbiorcy jest wymagany.");
        }

        if (!ALLOWED_BANKS.contains(paymentDto.getSenderBankId())) {
            throw new IllegalArgumentException("Nieprawidłowy bank nadawcy.");
        }

        if (!ALLOWED_BANKS.contains(paymentDto.getReceiverBankId())) {
            throw new IllegalArgumentException("Nieprawidłowy bank odbiorcy.");
        }

        if (paymentDto.getSenderBankId().equals(paymentDto.getReceiverBankId())) {
            throw new IllegalArgumentException("Bank nadawcy i odbiorcy nie mogą być takie same.");
        }

        if (paymentDto.getAmount() == null || paymentDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota musi być większa od zera.");
        }

        if (isBlank(paymentDto.getCurrency())) {
            throw new IllegalArgumentException("Waluta jest wymagana.");
        }

        if (!ALLOWED_CURRENCIES.contains(paymentDto.getCurrency())) {
            throw new IllegalArgumentException("Nieobsługiwana waluta.");
        }

        if (isBlank(paymentDto.getTitle())) {
            throw new IllegalArgumentException("Tytuł przelewu jest wymagany.");
        }

        if (paymentDto.getTitle().length() > 140) {
            throw new IllegalArgumentException("Tytuł przelewu jest za długi.");
        }
    }

    private String toPaymentIsoXml(ElixirPaymentDto paymentDto) {
        paymentDto.ensureDefaults();

        String paymentId = paymentDto.getPaymentId();
        String currency = defaultValue(paymentDto.getCurrency(), "PLN");
        String amount = formatAmount(paymentDto.getAmount());
        String type = defaultValue(paymentDto.getType(), SERVICE_CODE);
        String createdAt = LocalDateTime.now().toString();

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document>
                    <FIToFICstmrCdtTrf>
                        <GrpHdr>
                            <MsgId>%s</MsgId>
                            <CreDtTm>%s</CreDtTm>
                            <NbOfTxs>1</NbOfTxs>
                            <TtlIntrBkSttlmAmt Ccy="%s">%s</TtlIntrBkSttlmAmt>
                            <SttlmInf>
                                <SttlmMtd>CLRG</SttlmMtd>
                                <ClrSys>
                                    <Cd>%s</Cd>
                                </ClrSys>
                            </SttlmInf>
                        </GrpHdr>
                        <CdtTrfTxInf>
                            <PmtId>
                                <InstrId>%s</InstrId>
                                <EndToEndId>%s</EndToEndId>
                                <TxId>%s</TxId>
                            </PmtId>
                            <IntrBkSttlmAmt Ccy="%s">%s</IntrBkSttlmAmt>
                            <Dbtr>
                                <Nm>%s</Nm>
                            </Dbtr>
                            <DbtrAcct>
                                <Id>
                                    <IBAN>%s</IBAN>
                                </Id>
                            </DbtrAcct>
                            <DbtrAgt>
                                <FinInstnId>
                                    <BICFI>%s</BICFI>
                                </FinInstnId>
                            </DbtrAgt>
                            <Cdtr>
                                <Nm>%s</Nm>
                            </Cdtr>
                            <CdtrAcct>
                                <Id>
                                    <IBAN>%s</IBAN>
                                </Id>
                            </CdtrAcct>
                            <CdtrAgt>
                                <FinInstnId>
                                    <BICFI>%s</BICFI>
                                </FinInstnId>
                            </CdtrAgt>
                            <RmtInf>
                                <Ustrd>%s</Ustrd>
                            </RmtInf>
                            <SplmtryData>
                                <Envlp>
                                    <ServiceCode>%s</ServiceCode>
                                    <SenderBankId>%s</SenderBankId>
                                    <ReceiverBankId>%s</ReceiverBankId>
                                </Envlp>
                            </SplmtryData>
                        </CdtTrfTxInf>
                    </FIToFICstmrCdtTrf>
                </Document>
                """.formatted(
                escapeXml(paymentId),
                escapeXml(createdAt),
                escapeXml(currency),
                escapeXml(amount),
                escapeXml(type),

                escapeXml(paymentId),
                escapeXml(paymentId),
                escapeXml(paymentId),
                escapeXml(currency),
                escapeXml(amount),

                escapeXml(defaultValue(paymentDto.getSenderName(), "NIEZNANY NADAWCA")),
                escapeXml(paymentDto.getSenderAccount()),
                escapeXml(paymentDto.getSenderBankId()),

                escapeXml(defaultValue(paymentDto.getReceiverName(), "NIEZNANY ODBIORCA")),
                escapeXml(paymentDto.getReceiverAccount()),
                escapeXml(paymentDto.getReceiverBankId()),

                escapeXml(paymentDto.getTitle()),
                escapeXml(type),
                escapeXml(paymentDto.getSenderBankId()),
                escapeXml(paymentDto.getReceiverBankId())
        );
    }

    private String toAcceptedResponseXml(String paymentId, String status, String message) {
        String createdAt = LocalDateTime.now().toString();

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document>
                    <CstmrPmtStsRpt>
                        <GrpHdr>
                            <MsgId>%s</MsgId>
                            <CreDtTm>%s</CreDtTm>
                        </GrpHdr>
                        <OrgnlPmtInfAndSts>
                            <OrgnlPmtInfId>%s</OrgnlPmtInfId>
                            <TxInfAndSts>
                                <OrgnlInstrId>%s</OrgnlInstrId>
                                <OrgnlTxId>%s</OrgnlTxId>
                                <TxSts>%s</TxSts>
                                <StsRsnInf>
                                    <AddtlInf>%s</AddtlInf>
                                </StsRsnInf>
                            </TxInfAndSts>
                        </OrgnlPmtInfAndSts>
                    </CstmrPmtStsRpt>
                </Document>
                """.formatted(
                escapeXml("RESP-" + paymentId),
                escapeXml(createdAt),
                escapeXml(paymentId),
                escapeXml(paymentId),
                escapeXml(paymentId),
                escapeXml(status),
                escapeXml(message)
        );
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }

        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}