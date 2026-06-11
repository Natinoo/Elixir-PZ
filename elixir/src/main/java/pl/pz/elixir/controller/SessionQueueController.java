package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.service.SessionService;

import java.util.List;

@RestController
@RequestMapping("/api/elixir/session")
@Tag(name = "Sesja Elixir", description = "Zamykanie sesji, netting i kontrola płynności przed wysłaniem wyniku do SORBNET")
public class SessionQueueController {

    private final SessionService sessionService;

    public SessionQueueController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping(value = "/current", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy aktualnej sesji")
    public List<ElixirPaymentDto> currentSession() {
        return sessionService.getCurrentSessionSnapshot();
    }

    @PostMapping(value = "/close", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Zamknij sesję i wyślij wynik nettingu do SORBNET albo utwórz żądanie płynności")
    public SessionService.SessionCloseResult closeSession() {
        return sessionService.closeSession();
    }
}