package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.model.SessionReport;
import pl.pz.elixir.service.BankReportService;
import pl.pz.elixir.service.SessionReportExportService;
import pl.pz.elixir.service.SessionReportService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/elixir/bank")
@Tag(name = "Raporty bankowe", description = "Endpoints do generowania raportów rozliczeniowych dla poszczególnych banków")
public class BankReportController {

    private static final MediaType DOCX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final SessionReportService sessionReportService;
    private final BankReportService bankReportService;
    private final SessionReportExportService exportService;

    public BankReportController(SessionReportService sessionReportService,
                                BankReportService bankReportService,
                                SessionReportExportService exportService) {
        this.sessionReportService = sessionReportService;
        this.bankReportService = bankReportService;
        this.exportService = exportService;
    }

    @GetMapping(value = "/{bank}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz raport rozliczeniowy dla banku",
            description = "Zwraca listę operacji dla wskazanego banku na podstawie ostatniej zakończonej sesji.")
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

    @GetMapping(value = "/{bank}/report.docx", produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<byte[]> bankReportDocx(@PathVariable String bank) {
        SessionReport report = requireLastReport();
        List<String> lines = bankReport(bank, report);
        return fileResponse(
                exportService.exportBankReportDocx(bank, report, lines),
                DOCX_MEDIA_TYPE,
                fileName(bank, report.getSessionName(), "docx")
        );
    }

    @GetMapping(value = "/{bank}/report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> bankReportPdf(@PathVariable String bank) {
        SessionReport report = requireLastReport();
        List<String> lines = bankReport(bank, report);
        return fileResponse(
                exportService.exportBankReportPdf(bank, report, lines),
                MediaType.APPLICATION_PDF,
                fileName(bank, report.getSessionName(), "pdf")
        );
    }

    private SessionReport requireLastReport() {
        SessionReport report = sessionReportService.getLastReport();
        if (report == null) {
            throw new IllegalStateException("Brak raportu z ostatniej sesji ELIXIR.");
        }
        return report;
    }

    private List<String> bankReport(String bank, SessionReport report) {
        List<String> lines = bankReportService.getBankReport(bank, report.getNettingResult());
        return lines == null ? List.of() : lines;
    }

    private ResponseEntity<byte[]> fileResponse(byte[] bytes, MediaType mediaType, String fileName) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(bytes);
    }

    private String fileName(String bank, String sessionName, String extension) {
        String safeBank = bank == null || bank.isBlank() ? "BANK" : bank.replaceAll("[^A-Za-z0-9_-]", "_");
        String safeSession = sessionName == null || sessionName.isBlank()
                ? "latest"
                : sessionName.replaceAll("[^A-Za-z0-9_-]", "_");
        return "elixir-bank-report-" + safeBank + "-" + safeSession + "." + extension;
    }
}