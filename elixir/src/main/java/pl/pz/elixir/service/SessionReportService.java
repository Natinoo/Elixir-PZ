package pl.pz.elixir.service;

import org.springframework.stereotype.Service;
import pl.pz.elixir.model.SessionReport;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SessionReportService {

    private final List<SessionReport> reports = new ArrayList<>();

    public void saveReport(String sessionName, List<String> result) {

        SessionReport report = new SessionReport(sessionName, LocalDateTime.now(), result);

        reports.add(report);
    }

    public List<SessionReport> getAllReports() {
        return reports;
    }

    public SessionReport getLastReport() {

        if (reports.isEmpty()) {
            return null;
        }

        return reports.get(reports.size() - 1);
    }
}