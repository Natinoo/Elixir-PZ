package pl.pz.elixir.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.service.SessionService;

import java.util.List;

@RestController
public class SessionQueueController {

    private final SessionService sessionService;

    public SessionQueueController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/api/elixir/session-queue")
    public List<ElixirPaymentDto> queue() {
        return sessionService.getCurrentSession();
    }
}