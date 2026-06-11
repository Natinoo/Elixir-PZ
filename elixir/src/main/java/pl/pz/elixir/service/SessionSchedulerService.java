package pl.pz.elixir.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "elixir.session.test-mode", havingValue = "true")
public class SessionSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SessionSchedulerService.class);

    private final SessionService sessionService;

    public SessionSchedulerService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Scheduled(fixedDelayString = "${elixir.session.interval:900000}")
    public void closeTestSession() {
        try {
            SessionService.SessionCloseResult result = sessionService.closeSession();
            if (!"EMPTY_SESSION".equals(result.getStatus())) {
                log.info("Automatic ELIXIR session closed: sessionId={}, status={}",
                        result.getSessionId(), result.getStatus());
            }
        } catch (Exception e) {
            log.error("Cannot close automatic ELIXIR session", e);
        }
    }
}