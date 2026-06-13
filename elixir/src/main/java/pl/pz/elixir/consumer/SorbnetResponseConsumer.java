package pl.pz.elixir.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import pl.pz.elixir.model.PaymentStatus;
import pl.pz.elixir.service.BankLiquidityService;
import pl.pz.elixir.service.ElixirPaymentService;
import pl.pz.elixir.service.SessionService;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;

@Component
public class SorbnetResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(SorbnetResponseConsumer.class);

    private final ElixirPaymentService elixirPaymentService;
    private final BankLiquidityService bankLiquidityService;
    private final SessionService sessionService;

    public SorbnetResponseConsumer(ElixirPaymentService elixirPaymentService,
                                   BankLiquidityService bankLiquidityService,
                                   SessionService sessionService) {
        this.elixirPaymentService = elixirPaymentService;
        this.bankLiquidityService = bankLiquidityService;
        this.sessionService = sessionService;
    }

    @KafkaListener(topics = "responses.elixir", groupId = "elixir-group")
    public void consume(String message) {
        try {
            Document document = parseDocument(message);

            if (isLiquidityTransferResponse(document)) {
                handleLiquidityTransferResponse(document, message);
                return;
            }

            handlePaymentStatusResponse(document, message);
        } catch (Exception e) {
            log.error("Cannot process SORBNet XML response: {}", message, e);
        }
    }

    private void handleLiquidityTransferResponse(Document document, String rawXml) {
        LiquidityResponse response = parseLiquidityResponse(document);

        if (response.requestId == null || response.requestId.isBlank()) {
            log.warn("SORBNet liquidity response ignored: missing requestId. XML={}", rawXml);
            return;
        }
        if (response.bankId == null || response.bankId.isBlank()) {
            log.warn("SORBNet liquidity response ignored: missing bankId. requestId={}, XML={}", response.requestId, rawXml);
            return;
        }
        if (response.amount == null || response.amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("SORBNet liquidity response ignored: invalid amount. requestId={}, bank={}, amount={}",
                    response.requestId, response.bankId, response.amount);
            return;
        }

        String status = normalize(response.status);
        if (!isSettled(status)) {
            log.warn(
                    "SORBNet liquidity response did not settle funds: requestId={}, bank={}, status={}, message={}",
                    response.requestId,
                    response.bankId,
                    response.status,
                    response.message
            );
            return;
        }

        String targetService = firstNonBlank(response.targetServiceCode, BankLiquidityService.ELIXIR);
        if (!BankLiquidityService.ELIXIR.equalsIgnoreCase(targetService)) {
            log.warn(
                    "SORBNet liquidity response ignored: target service is not ELIXIR. requestId={}, bank={}, targetService={}",
                    response.requestId,
                    response.bankId,
                    response.targetServiceCode
            );
            return;
        }

        bankLiquidityService.applyConfirmedLiquidityTransfer(
                firstNonBlank(response.sourceServiceCode, BankLiquidityService.SORBNET),
                BankLiquidityService.ELIXIR,
                response.bankId,
                response.amount
        );
        bankLiquidityService.unblockBank(BankLiquidityService.ELIXIR, response.bankId);
        sessionService.markLiquidityRequestCompleted(response.requestId, response.bankId);

        log.info(
                "SORBNet liquidity response processed: requestId={}, bank={}, amount={}, status={}, settledAt={}, message={}",
                response.requestId,
                response.bankId,
                response.amount,
                response.status,
                response.settledAt,
                response.message
        );

        SessionService.SessionCloseResult result = sessionService.closeSession();
        log.info(
                "ELIXIR session retry after liquidity response: requestId={}, bank={}, sessionId={}, status={}",
                response.requestId,
                response.bankId,
                result.getSessionId(),
                result.getStatus()
        );
    }

    private void handlePaymentStatusResponse(Document document, String rawXml) {
        SorbnetResponse response = parsePaymentStatusResponse(document);

        if (response.paymentId == null || response.paymentId.isBlank()) {
            log.warn("SORBNet payment response ignored: missing paymentId. XML={}", rawXml);
            return;
        }

        PaymentStatus mappedStatus = mapStatus(response.status);
        elixirPaymentService.updatePaymentStatus(response.paymentId, mappedStatus);

        log.info(
                "SORBNet payment response processed: paymentId={}, sorbnetStatus={}, mappedStatus={}, settledAt={}, message={}",
                response.paymentId,
                response.status,
                mappedStatus,
                response.settledAt,
                response.message
        );
    }

    private Document parseDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        return factory
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
    }

    private boolean isLiquidityTransferResponse(Document document) {
        return document.getElementsByTagName("LiquidityCreditTransferResponse").getLength() > 0
                || (document.getElementsByTagName("TrfSts").getLength() > 0
                && firstNonBlank(tag(document, "ReqId"), tag(document, "BankId")) != null);
    }

    private LiquidityResponse parseLiquidityResponse(Document document) {
        LiquidityResponse response = new LiquidityResponse();
        response.requestId = firstNonBlank(
                tag(document, "ReqId"),
                tag(document, "OrgnlTxId"),
                tag(document, "OrgnlInstrId"),
                tag(document, "RequestId"),
                tag(document, "MsgId")
        );
        response.bankId = firstNonBlank(tag(document, "BankId"), tag(document, "bankId"));
        response.sourceServiceCode = firstNonBlank(tag(document, "SourceServiceCode"), tag(document, "sourceServiceCode"));
        response.targetServiceCode = firstNonBlank(tag(document, "TargetServiceCode"), tag(document, "targetServiceCode"));
        response.sourceAccount = firstNonBlank(tag(document, "SourceAccount"), tag(document, "sourceAccount"));
        response.targetAccount = firstNonBlank(tag(document, "TargetAccount"), tag(document, "targetAccount"));
        response.amount = parseAmount(firstNonBlank(
                tag(document, "Amt"),
                tag(document, "Amount"),
                tag(document, "amount"),
                tag(document, "IntrBkSttlmAmt")
        ));
        response.status = firstNonBlank(tag(document, "Sts"), tag(document, "Status"), tag(document, "status"), tag(document, "TxSts"));
        response.message = firstNonBlank(tag(document, "Msg"), tag(document, "Message"), tag(document, "message"), tag(document, "AddtlInf"));
        response.settledAt = firstNonBlank(tag(document, "SettledAt"), tag(document, "settledAt"), tag(document, "CreDtTm"));
        return response;
    }

    private SorbnetResponse parsePaymentStatusResponse(Document document) {
        SorbnetResponse response = new SorbnetResponse();

        // Obsługiwane formaty:
        // 1) stary: <SorbnetPaymentResponse><paymentId>...</paymentId><status>SETTLED</status></SorbnetPaymentResponse>
        // 2) ISO-style: <Document><CstmrPmtStsRpt>...<OrgnlTxId>...</OrgnlTxId><TxSts>...</TxSts></CstmrPmtStsRpt></Document>
        response.paymentId = firstNonBlank(
                tag(document, "paymentId"),
                tag(document, "PaymentId"),
                tag(document, "OrgnlTxId"),
                tag(document, "OrgnlInstrId"),
                tag(document, "OrgnlPmtInfId")
        );

        response.status = firstNonBlank(
                tag(document, "status"),
                tag(document, "Status"),
                tag(document, "TxSts")
        );

        response.message = firstNonBlank(
                tag(document, "message"),
                tag(document, "Message"),
                tag(document, "AddtlInf")
        );

        response.settledAt = firstNonBlank(
                tag(document, "settledAt"),
                tag(document, "SettledAt"),
                tag(document, "CreDtTm")
        );

        return response;
    }

    private String tag(Document document, String tagName) {
        if (document.getElementsByTagName(tagName).getLength() == 0) {
            return null;
        }

        String value = document.getElementsByTagName(tagName).item(0).getTextContent();
        return value == null ? null : value.trim();
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace(" ", "").replace(",", ".");
        return new BigDecimal(normalized);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean isSettled(String status) {
        return switch (status) {
            case "SETTLED", "ACSC", "ACCP", "ACSP" -> true;
            default -> false;
        };
    }

    private PaymentStatus mapStatus(String sorbnetStatus) {
        String status = normalize(sorbnetStatus);
        if (status.isBlank()) {
            log.warn("Empty SORBNet status received. Mapping to BLOCKED.");
            return PaymentStatus.BLOCKED;
        }

        return switch (status) {
            case "SETTLED", "ACSC", "ACCP", "ACSP" -> PaymentStatus.PROCESSED;
            case "REJECTED", "RJCT" -> PaymentStatus.REJECTED;
            case "GRIDLOCK_HELD", "BLOCKED", "BLCK", "PDNG" -> PaymentStatus.BLOCKED;
            default -> {
                log.warn("Unknown SORBNet status received: {}. Mapping to BLOCKED.", sorbnetStatus);
                yield PaymentStatus.BLOCKED;
            }
        };
    }

    private static class LiquidityResponse {
        private String requestId;
        private String bankId;
        private String sourceServiceCode;
        private String targetServiceCode;
        private String sourceAccount;
        private String targetAccount;
        private BigDecimal amount;
        private String status;
        private String message;
        private String settledAt;
    }

    private static class SorbnetResponse {
        private String paymentId;
        private String status;
        private String message;
        private String settledAt;
    }
}