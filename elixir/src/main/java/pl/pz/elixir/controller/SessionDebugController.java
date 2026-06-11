package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.service.SessionService;

@RestController
@RequestMapping("/api/elixir/session")
@Tag(name = "Debugowanie sesji", description = "Endpointy pomocnicze do testowania ręcznego zamykania sesji")
public class SessionDebugController {

    private final SessionService sessionService;

    public SessionDebugController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/close-test")
    @Operation(
            summary = "Ręczne zamknięcie sesji testowej",
            description = "Kończy bieżącą sesję i uruchamia proces rozliczeń: netting, kontrolę płynności oraz ewentualne wysłanie wyniku do Sorbnetu."
    )
    @ApiResponse(responseCode = "200", description = "Sesja zamknięta")
    public SessionService.SessionCloseResult closeTestSession() {
        return sessionService.closeSession();
    }
}