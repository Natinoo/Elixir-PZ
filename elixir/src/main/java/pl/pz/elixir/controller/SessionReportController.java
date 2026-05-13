package pl.pz.elixir.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.model.SessionReport;
import pl.pz.elixir.service.SessionReportService;

import java.util.List;

@RestController
public class SessionReportController {

    private final SessionReportService sessionReportService;

    public SessionReportController(SessionReportService sessionReportService) {
        this.sessionReportService = sessionReportService;
    }

    @GetMapping("/api/elixir/session-report/latest")
    public SessionReport latest() {
        return sessionReportService.getLastReport();
    }

    @GetMapping("/api/elixir/session-report/history")
    public List<SessionReport> history() {
        return sessionReportService.getAllReports();
    }
}