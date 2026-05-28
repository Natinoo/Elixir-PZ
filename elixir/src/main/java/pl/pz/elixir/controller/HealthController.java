package pl.pz.elixir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health Check", description = "Endpoint do sprawdzania stanu usługi")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Sprawdzenie statusu serwisu", description = "Zwraca informację o działaniu serwisu Elixir.")
    @ApiResponse(responseCode = "200", description = "Serwis działa poprawnie",
            content = @Content(schema = @Schema(implementation = Map.class)))
    public Map<String, String> health() {
        return Map.of(
                "service", "elixir",
                "status", "OK"
        );
    }
}