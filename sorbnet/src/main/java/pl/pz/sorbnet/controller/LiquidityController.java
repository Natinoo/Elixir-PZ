package pl.pz.sorbnet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping(value = "/api/sorbnet/liquidity", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
    name = "SORBNet Liquidity",
    description = """
        Obsługa requestów płynnościowych z systemów ELIXIR / ELIXIR EXPRESS.
        Request trafia do SORBNET asynchronicznie (Kafka, ISO 20022) gdy bankowi
        brakuje środków na lokalne rozliczenie sesji nettingowej. Operator banku
        widzi go w GUI (push WebSocket na /topic/liquidity oraz /topic/alerts/{bankId})
        i decyduje o wykonaniu przelewu zasilającego techniczne konto banku
        w serwisie źródłowym albo o odrzuceniu requestu.
        Endpointy GUI zwracają JSON; komunikacja międzysystemowa pozostaje w ISO 20022 XML.
        """
)
public class LiquidityController {

    private final LiquidityService liquidityService;
    

    public LiquidityController(LiquidityService liquidityService) {
        this.liquidityService = liquidityService;
    }

    @Operation(
        summary = "Lista oczekujących requestów płynnościowych",
        description = """
            Zwraca requesty w stanie PENDING, czekające na decyzję operatora banku,
            posortowane od najstarszego. Endpoint zasila widok alertów płynnościowych
            w GUI pracownika banku oraz operatora SORBNET.
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista oczekujących requestów została zwrócona poprawnie.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            array = @ArraySchema(schema = @Schema(implementation = LiquidityRequest.class)),
            examples = @ExampleObject(
                name = "Oczekujące requesty",
                value = """
                    [
                      {
                        "requestId": "LIQ-ELIXIR-SESSION-7f3a-BANK_A",
                        "sessionId": "ELIXIR-SESSION-7f3a",
                        "bankId": "BANK_A",
                        "requestingServiceCode": "ELIXIR",
                        "sourceAccount": "11111100000000000000000001",
                        "targetAccount": "55555500000000000000000001",
                        "amount": 1500000.00,
                        "currency": "PLN",
                        "message": "Brak płynności w ELIXIR przed lokalnym rozliczeniem sesji nettingowej",
                        "status": "PENDING",
                        "receivedAt": "2026-06-11T13:15:00",
                        "processedAt": null,
                        "paymentId": null
                      }
                    ]
                    """
            )
        )
    )
    @GetMapping("/requests")
    public List<LiquidityRequest> pending() {
        return liquidityService.findPending();
    }

    @Operation(
        summary = "Historia requestów płynnościowych banku",
        description = """
            Zwraca wszystkie requesty płynnościowe wskazanego banku
            (PENDING / EXECUTED / REJECTED), posortowane od najnowszego.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Historia requestów banku została zwrócona poprawnie.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = LiquidityRequest.class)),
                examples = @ExampleObject(
                    name = "Historia banku",
                    value = """
                        [
                          {
                            "requestId": "LIQ-ELIXIR-SESSION-7f3a-BANK_A",
                            "sessionId": "ELIXIR-SESSION-7f3a",
                            "bankId": "BANK_A",
                            "requestingServiceCode": "ELIXIR",
                            "sourceAccount": "11111100000000000000000001",
                            "targetAccount": "55555500000000000000000001",
                            "amount": 1500000.00,
                            "currency": "PLN",
                            "message": "Brak płynności w ELIXIR przed lokalnym rozliczeniem sesji nettingowej",
                            "status": "EXECUTED",
                            "receivedAt": "2026-06-11T13:15:00",
                            "processedAt": "2026-06-11T13:18:42",
                            "paymentId": "LIQ-2c9d4a1e-7b3f-4c2a-9e1d-0f6a8b5c3d2e"
                          },
                          {
                            "requestId": "LIQ-ELIXIR-SESSION-19bc-BANK_A",
                            "sessionId": "ELIXIR-SESSION-19bc",
                            "bankId": "BANK_A",
                            "requestingServiceCode": "ELIXIR_EXPRESS",
                            "sourceAccount": "11111100000000000000000001",
                            "targetAccount": "66666600000000000000000001",
                            "amount": 800000.00,
                            "currency": "PLN",
                            "message": "Brak płynności w sesji",
                            "status": "REJECTED",
                            "receivedAt": "2026-06-10T09:00:00",
                            "processedAt": "2026-06-10T09:05:12",
                            "paymentId": null
                          }
                        ]
                        """
                )
            )
        )
    })
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
            obciążony (z kontrolą limitu zadłużenia), przelew zapisuje się
            w historii SORBNET, a do serwisu źródłowego (ELIXIR / ELIXIR EXPRESS)
            wysyłana jest odpowiedź ISO 20022 (LiquidityCreditTransferResponse)
            ze statusem SETTLED. Na jej podstawie serwis zasila techniczne konto
            banku i domyka sesję czekającą na płynność.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Przelew płynnościowy został wykonany.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(type = "object"),
                examples = @ExampleObject(
                    name = "Request wykonany",
                    value = """
                        {
                          "requestId": "LIQ-ELIXIR-SESSION-7f3a-BANK_A",
                          "status": "EXECUTED",
                          "paymentId": "LIQ-2c9d4a1e-7b3f-4c2a-9e1d-0f6a8b5c3d2e",
                          "bankId": "BANK_A",
                          "amount": 1500000.00,
                          "newBalance": 3500000.00
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nie znaleziono requestu o podanym identyfikatorze albo banku z requestu.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = """
                Request nie może zostać wykonany: został już przetworzony,
                bank jest zablokowany albo obciążenie przekroczyłoby limit
                zadłużenia rachunku banku w SORBNET.
                """,
            content = @Content
        )
    })
    @PostMapping("/requests/{requestId}/execute")
    public Map<String, Object> execute(
            @Parameter(description = "Identyfikator requestu płynnościowego (ReqId).", example = "LIQ-ELIXIR-SESSION-7f3a-BANK_A")
            @PathVariable String requestId) {
        return liquidityService.execute(requestId);
    }

    @Operation(
        summary = "Odrzuć request płynnościowy",
        description = """
            Operator banku odrzuca request. Do serwisu źródłowego wysyłana jest
            odpowiedź ISO 20022 ze statusem REJECTED — sesja w serwisie
            źródłowym pozostaje w stanie oczekiwania na płynność.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Request został odrzucony.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(type = "object"),
                examples = @ExampleObject(
                    name = "Request odrzucony",
                    value = """
                        {
                          "requestId": "LIQ-ELIXIR-SESSION-7f3a-BANK_A",
                          "status": "REJECTED",
                          "message": "Brak zgody skarbnika banku"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nie znaleziono requestu o podanym identyfikatorze.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Request został już przetworzony (status inny niż PENDING).",
            content = @Content
        )
    })
    @PostMapping("/requests/{requestId}/reject")
    public Map<String, Object> reject(
            @Parameter(description = "Identyfikator requestu płynnościowego (ReqId).", example = "LIQ-ELIXIR-SESSION-7f3a-BANK_A")
            @PathVariable String requestId,
            @Parameter(
                name = "reason",
                in = ParameterIn.QUERY,
                description = "Opcjonalny powód odrzucenia, przekazywany do serwisu źródłowego.",
                example = "Brak zgody skarbnika banku"
            )
            @RequestParam(required = false) String reason) {
        return liquidityService.reject(requestId, reason);
    }

       @GetMapping("/requests/all")
                public ResponseEntity<List<LiquidityRequest>> getAllRequests() {
                return ResponseEntity.ok(liquidityService.getAllRequests());
                }
}