package pl.pz.elixir.controller;

import org.springframework.web.bind.annotation.*;
import pl.pz.elixir.model.SessionReport;
import pl.pz.elixir.service.BankReportService;
import pl.pz.elixir.service.SessionReportService;

import java.util.List;

@RestController
@RequestMapping("/api/elixir/bank")
public class BankReportController {

    private final SessionReportService sessionReportService;
    private final BankReportService bankReportService;

    public BankReportController(SessionReportService sessionReportService, BankReportService bankReportService) {

        this.sessionReportService = sessionReportService;
        this.bankReportService = bankReportService;
    }

    @GetMapping("/{bank}/report")
    public List<String> report(@PathVariable String bank) {

        SessionReport report = sessionReportService.getLastReport();

        if (report == null) {
            return List.of();
        }

        return bankReportService.getBankReport(bank, report.getNettingResult());
    }
}