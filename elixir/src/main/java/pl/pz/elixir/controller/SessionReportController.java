package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.model.SessionReport;
import pl.pz.elixir.service.SessionReportService;

import java.util.List;

@RestController
@Tag(name = "Raporty sesyjne", description = "Podgląd wyników zamkniętych sesji (netting, rozliczenia międzybankowe)")
public class SessionReportController {

    private final SessionReportService sessionReportService;

    public SessionReportController(SessionReportService sessionReportService) {
        this.sessionReportService = sessionReportService;
    }

    @GetMapping(value = "/api/elixir/session-report/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz ostatni raport sesji", description = "Zwraca szczegóły ostatniej zakończonej sesji.")
    @ApiResponse(responseCode = "200", description = "Raport znaleziony",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"sessionId\":\"2024-05-28T10:00\",\"nettingResult\":{}}")))
    public SessionReport latest() {
        return sessionReportService.getLastReport();
    }

    @GetMapping(value = "/api/elixir/session-report/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Historia raportów", description = "Zwraca wszystkie zamknięte sesje wraz z wynikami.")
    @ApiResponse(responseCode = "200", description = "Lista raportów",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "[{\"sessionId\":\"2024-05-28T10:00\"},{\"sessionId\":\"2024-05-28T11:00\"}]")))
    public List<SessionReport> history() {
        return sessionReportService.getAllReports();
    }
}