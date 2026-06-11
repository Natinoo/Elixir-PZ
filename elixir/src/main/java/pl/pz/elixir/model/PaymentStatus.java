package pl.pz.elixir.model;

public enum PaymentStatus {
    QUEUED,
    IN_SESSION,
    WAITING_FOR_LIQUIDITY,
    NETTING_SENT,
    PROCESSED,
    BLOCKED,
    REJECTED
}
