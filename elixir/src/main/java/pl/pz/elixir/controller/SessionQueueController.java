package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.elixir.dto.ElixirPaymentDto;
import pl.pz.elixir.service.SessionService;

import java.util.List;

@RestController
@Tag(name = "Kolejka sesyjna", description = "Podgląd bieżącej sesji – przelewy oczekujące na rozliczenie")
public class SessionQueueController {

    private final SessionService sessionService;

    public SessionQueueController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping(value = "/api/elixir/session-queue", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pobierz przelewy w bieżącej sesji", description = "Zwraca listę przelewów, które wpłynęły w trakcie aktualnej sesji.")
    @ApiResponse(responseCode = "200", description = "Lista przelewów",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "[{\"paymentId\":\"queue1\",\"amount\":200.0}]")))
    public List<ElixirPaymentDto> queue() {
        return sessionService.getCurrentSession();
    }
}