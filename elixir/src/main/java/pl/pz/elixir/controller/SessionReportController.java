package pl.pz.elixir.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.model.SessionReport;
import pl.pz.elixir.service.SessionReportExportService;
import pl.pz.elixir.service.SessionReportService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/elixir/session-report")
public class SessionReportController {

    private static final MediaType DOCX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final SessionReportService sessionReportService;
    private final SessionReportExportService exportService;

    public SessionReportController(SessionReportService sessionReportService,
                                   SessionReportExportService exportService) {
        this.sessionReportService = sessionReportService;
        this.exportService = exportService;
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SessionReport> allReports() {
        return sessionReportService.getAllReports();
    }

    @GetMapping(value = "/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SessionReport> latestReport() {
        SessionReport report = sessionReportService.getLastReport();
        if (report == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(report);
    }

    @GetMapping(value = "/latest.docx", produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<byte[]> latestReportDocx() {
        SessionReport report = requireLastReport();
        return fileResponse(
                exportService.exportSessionReportDocx(report),
                DOCX_MEDIA_TYPE,
                fileName(report.getSessionName(), "docx")
        );
    }

    @GetMapping(value = "/latest.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> latestReportPdf() {
        SessionReport report = requireLastReport();
        return fileResponse(
                exportService.exportSessionReportPdf(report),
                MediaType.APPLICATION_PDF,
                fileName(report.getSessionName(), "pdf")
        );
    }

    private SessionReport requireLastReport() {
        SessionReport report = sessionReportService.getLastReport();
        if (report == null) {
            throw new IllegalStateException("Brak raportu z ostatniej sesji ELIXIR.");
        }
        return report;
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

    private String fileName(String sessionName, String extension) {
        String safeSession = sessionName == null || sessionName.isBlank()
                ? "latest"
                : sessionName.replaceAll("[^A-Za-z0-9_-]", "_");
        return "elixir-session-report-" + safeSession + "." + extension;
    }
}