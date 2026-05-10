package pl.pz.elixir.model;

import java.time.LocalDateTime;
import java.util.List;

public class SessionReport {

    private String sessionName;
    private LocalDateTime closedAt;
    private List<String> nettingResult;

    public SessionReport() {
    }

    public SessionReport(String sessionName,
                         LocalDateTime closedAt,
                         List<String> nettingResult) {

        this.sessionName = sessionName;
        this.closedAt = closedAt;
        this.nettingResult = nettingResult;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public List<String> getNettingResult() {
        return nettingResult;
    }

    public void setNettingResult(List<String> nettingResult) {
        this.nettingResult = nettingResult;
    }
}