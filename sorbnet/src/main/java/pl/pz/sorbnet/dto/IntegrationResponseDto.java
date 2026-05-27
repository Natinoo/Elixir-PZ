package pl.pz.sorbnet.dto;

import java.time.LocalDateTime;

public class IntegrationResponseDto {

    private String sourceSystem;
    private String correlationId;
    private String paymentId;
    private String status;
    private String message;
    private String bankId;
    private LocalDateTime processedAt;

    public IntegrationResponseDto() {
    }

    public IntegrationResponseDto(String sourceSystem, String correlationId, String paymentId,
                                  String status, String message, String bankId, LocalDateTime processedAt) {
        this.sourceSystem = sourceSystem;
        this.correlationId = correlationId;
        this.paymentId = paymentId;
        this.status = status;
        this.message = message;
        this.bankId = bankId;
        this.processedAt = processedAt;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}