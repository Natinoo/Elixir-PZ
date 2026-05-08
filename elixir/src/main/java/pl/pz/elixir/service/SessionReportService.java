package pl.pz.elixir.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SessionReportService {

    private List<String> lastSessionReport = new ArrayList<>();

    public void saveReport(List<String> report) {
        this.lastSessionReport = new ArrayList<>(report);
    }

    public List<String> getLastSessionReport() {
        return lastSessionReport;
    }
}