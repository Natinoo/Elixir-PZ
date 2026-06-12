package pl.pz.elixirexpress.model;

public enum PaymentStatus {
    QUEUED,
    PROCESSED,
    BLOCKED,
    REJECTED,
    GRIDLOCK_HELD,
    WAITING_FOR_LIQUIDITY,
    SETTLED
}