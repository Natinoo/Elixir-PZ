package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.service.SessionService;

import java.util.Map;

@RestController
@RequestMapping("/api/elixir/session")
@Tag(name = "Bieżąca sesja Elixir", description = "Podgląd bieżącego rozrachunku sesji przed jej zamknięciem")
public class CurrentSessionController {

    private final SessionService sessionService;

    public CurrentSessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping(value = "/current-exposure", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz bieżące zobowiązania banków w aktualnej sesji")
    public Map<String, SessionService.CurrentBankExposure> currentExposure() {
        return sessionService.getCurrentBankExposures();
    }

    @GetMapping(value = "/bank/{bankId}/current-exposure", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz bieżące zobowiązania jednego banku w aktualnej sesji")
    public SessionService.CurrentBankExposure currentExposureForBank(@PathVariable String bankId) {
        return sessionService.getCurrentBankExposure(bankId);
    }
}