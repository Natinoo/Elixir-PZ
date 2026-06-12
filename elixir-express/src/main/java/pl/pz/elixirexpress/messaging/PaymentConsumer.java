package pl.pz.elixirexpress.messaging;

/**
 * Ten forwarding został celowo wyłączony.
 *
 * Elixir Express nie wysyła już każdego zwykłego przelewu do Sorbnetu.
 * Express sam loguje i rozlicza swoje transakcje lokalnie.
 * Do Sorbnetu trafiają tylko requesty płynnościowe wysyłane z ExpressPaymentService
 * na topic liquidity.requests.express.sorbnet, gdy bank nie ma płynności.
 */
public class PaymentConsumer {
}