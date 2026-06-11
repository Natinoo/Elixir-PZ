package pl.pz.sorbnet.model;

/**
 * Status requestu płynnościowego w SORBNET:
 * - PENDING: czeka na decyzję operatora banku w GUI,
 * - EXECUTED: operator wykonał przelew zasilający konto ELIXIR,
 * - REJECTED: operator odrzucił request (lub brak środków na rachunku SORBNET).
 */
public enum LiquidityRequestStatus {
    PENDING,
    EXECUTED,
    REJECTED
}