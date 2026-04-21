package com.example.elixir.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private String paymentId;
    private Double amount;
    private String currency;
    private String senderAccount;
    private String receiverAccount;
    private String title;
}