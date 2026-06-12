package pl.pz.elixirexpress.messaging;

/**
 * Klasa pozostawiona tylko dla kompatybilności plików projektu.
 * Normalne przelewy Express nie są już forwardowane do Sorbnetu.
 * Requesty płynnościowe wysyła bezpośrednio ExpressPaymentService.
 */
public class SorbnetProducer {
    public void sendToSorbnet(String payload) {
        // intentionally disabled
    }
}
