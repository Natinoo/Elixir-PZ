package pl.pz.sorbnet.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PaymentStatus",
        enumAsRef = true,
        description = """
                Status przetwarzania płatności w systemie SORBNet.

                Dostępne wartości:
                - PENDING: przelew został zarejestrowany i oczekuje na dalsze przetwarzanie,
                - SETTLED: przelew został poprawnie rozliczony,
                - REJECTED: przelew został odrzucony,
                - GRIDLOCK_HELD: przelew został czasowo wstrzymany w kolejce gridlock resolution.
                """
)
public enum PaymentStatus {
    PENDING,
    SETTLED,
    REJECTED,
    GRIDLOCK_HELD
}