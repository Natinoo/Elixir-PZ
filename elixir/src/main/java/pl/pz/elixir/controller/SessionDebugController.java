package pl.pz.elixir.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.service.SessionService;

@RestController
@RequestMapping("/api/elixir/session")
public class SessionDebugController {

    private final SessionService sessionService;

    public SessionDebugController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/close-test")
    public String closeTestSession() {
        sessionService.closeSession("TEST-MANUAL");
        return "TEST SESSION CLOSED";
    }
}