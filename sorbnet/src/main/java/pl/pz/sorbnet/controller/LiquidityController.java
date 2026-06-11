package pl.pz.sorbnet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.pz.sorbnet.model.LiquidityRequest;
import pl.pz.sorbnet.service.LiquidityService;

import java.util.List;
import java.util.Map;

/**
 * Endpointy dla GUI SORBNET — operator banku widzi tu requesty płynnościowe
 * z ELIXIR / ELIXIR EXPRESS i wyklikuje przelew zasilający konto techniczne
 * banku w danym serwisie.
 *
 * Endpointy GUI zwracają JSON (komunikacja międzysystemowa Kafka/API
 * pozostaje w ISO 20022 XML).
 */
@RestController
@RequestMapping(value = "/api/sorbnet/liquidity", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
        name = "SORBNet Liquidity",
        description = "Obsługa requestów płynnościowych z systemów ELIXIR — podgląd, wykonanie i odrzucenie przez operatora banku."
)
public class LiquidityController {

    private final LiquidityService liquidityService;

    public LiquidityController(LiquidityService liquidityService) {
        this.liquidityService = liquidityService;
    }

    @Operation(
            summary = "Lista oczekujących requestów płynnościowych",
            description = "Zwraca requesty w stanie PENDING, czekające na decyzję operatora banku."
    )
    @GetMapping("/requests")
    public List<LiquidityRequest> pending() {
        return liquidityService.findPending();
    }

    @Operation(
            summary = "Historia requestów płynnościowych banku",
            description = "Zwraca wszystkie requesty (PENDING/EXECUTED/REJECTED) dla wskazanego banku."
    )
    @GetMapping("/requests/bank/{bankId}")
    public List<LiquidityRequest> byBank(
            @Parameter(description = "Identyfikator banku.", example = "BANK_A")
            @PathVariable String bankId) {
        return liquidityService.findByBank(bankId);
    }

    @Operation(
            summary = "Wykonaj przelew płynnościowy",
            description = """
                    Operator banku zatwierdza request: rachunek banku w SORBNET zostaje
                    obciążony, a do serwisu źródłowego (ELIXIR / ELIXIR EXPRESS) wysyłana
                    jest odpowiedź ISO 20022 ze statusem SETTLED, na podstawie której
                    serwis zasila techniczne konto banku i domyka sesję.
                    """
    )
    @PostMapping("/requests/{requestId}/execute")
    public Map<String, Object> execute(
            @Parameter(description = "Identyfikator requestu płynnościowego.", example = "LIQ-REQ-20260611-0001")
            @PathVariable String requestId) {
        return liquidityService.execute(requestId);
    }

    @Operation(
            summary = "Odrzuć request płynnościowy",
            description = "Operator banku odrzuca request; serwis źródłowy otrzymuje odpowiedź ISO 20022 ze statusem REJECTED."
    )
    @PostMapping("/requests/{requestId}/reject")
    public Map<String, Object> reject(
            @Parameter(description = "Identyfikator requestu płynnościowego.", example = "LIQ-REQ-20260611-0001")
            @PathVariable String requestId,
            @Parameter(description = "Opcjonalny powód odrzucenia.", example = "Brak zgody skarbnika banku")
            @RequestParam(required = false) String reason) {
        return liquidityService.reject(requestId, reason);
    }
}