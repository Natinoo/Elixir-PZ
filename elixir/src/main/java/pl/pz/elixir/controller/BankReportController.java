package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.model.SessionReport;
import pl.pz.elixir.service.BankReportService;
import pl.pz.elixir.service.SessionReportService;

import java.util.List;

@RestController
@RequestMapping("/api/elixir/bank")
@Tag(name = "Raporty bankowe", description = "Endpoints do generowania raportów rozliczeniowych dla poszczególnych banków")
public class BankReportController {

    private final SessionReportService sessionReportService;
    private final BankReportService bankReportService;

    public BankReportController(SessionReportService sessionReportService, BankReportService bankReportService) {
        this.sessionReportService = sessionReportService;
        this.bankReportService = bankReportService;
    }

    @GetMapping(value = "/{bank}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz raport rozliczeniowy dla banku",
            description = "Zwraca listę operacji (w formie tekstowej) dla wskazanego banku na podstawie ostatniej zakończonej sesji.")
    @ApiResponse(responseCode = "200", description = "Raport pobrany pomyślnie",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)),
                    examples = @ExampleObject(value = "[\"BANK_A -> BANK_B: 100.00 PLN\", \"BANK_C -> BANK_A: 50.00 PLN\"]")))
    public List<String> report(
            @Parameter(description = "Nazwa banku (np. BANK_A, BANK_B, BANK_C)", required = true, example = "BANK_A")
            @PathVariable String bank) {
        SessionReport report = sessionReportService.getLastReport();
        if (report == null) {
            return List.of();
        }
        List<String> bankReport = bankReportService.getBankReport(bank, report.getNettingResult());
        return bankReport != null ? bankReport : List.of();
    }
}