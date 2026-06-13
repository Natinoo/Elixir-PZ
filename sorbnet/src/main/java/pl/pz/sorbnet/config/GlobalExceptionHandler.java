package pl.pz.sorbnet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Globalna obsługa błędów — zamiast surowego 500 ze stack trace zwraca
 * czytelny komunikat z właściwym kodem HTTP.
 *
 * Konwencja kodów:
 * - "Nieznany bank/rachunek..."          -> 404 NOT_FOUND
 * - mismatch / blokada / brak płynności   -> 409 CONFLICT
 * - błędne argumenty                      -> 400 BAD_REQUEST
 * - ResponseStatusException               -> kod ustawiony przy rzuceniu
 * - reszta                                -> 500 (z komunikatem, bez trace)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return build(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Błąd przetwarzania żądania";
        HttpStatus status = classify(msg);
        if (status.is5xxServerError()) {
            log.error("Nieoczekiwany błąd: {}", msg, ex); // pełny trace tylko do logów
        } else {
            log.warn("Błąd biznesowy [{}]: {}", status.value(), msg);
        }
        return build(status, msg);
    }

    /** Mapuje komunikat wyjątku na sensowny kod HTTP. */
    private HttpStatus classify(String msg) {
        String m = msg.toLowerCase();
        if (m.contains("nieznany") || m.contains("nie znaleziono") || m.contains("brak konta")) {
            return HttpStatus.NOT_FOUND;
        }
        if (m.contains("zablokowan") || m.contains("brak środków") || m.contains("brak srodkow")
                || m.contains("mismatch") || m.contains("limit") || m.contains("przetworzony")) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}