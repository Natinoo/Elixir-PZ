package pl.pz.elixirexpress.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health Check", description = "Endpoint do sprawdzania stanu usługi")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Sprawdzenie statusu serwisu", description = "Zwraca informację o działaniu serwisu Elixir Express.")
    @ApiResponse(responseCode = "200", description = "Serwis działa poprawnie",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"service\":\"elixir-express\",\"status\":\"OK\"}")))
    public Map<String, String> health() {
        return Map.of("service", "elixir-express", "status", "OK");
    }
}