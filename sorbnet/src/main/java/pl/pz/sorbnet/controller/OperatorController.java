package pl.pz.sorbnet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pl.pz.sorbnet.model.BankAccount;
import pl.pz.sorbnet.model.Payment;
import pl.pz.sorbnet.model.PaymentStatus;
import pl.pz.sorbnet.repository.BankAccountRepository;
import pl.pz.sorbnet.repository.PaymentRepository;
import pl.pz.sorbnet.service.GridlockResolutionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sorbnet/operator")
@Tag(
    name = "Operator API",
    description = "Endpointy administracyjne dla operatora systemu RTGS SORBNet. Umożliwiają monitoring banków, przelewów, sytuacji nadzwyczajnych, blokad oraz kolejki gridlock."
)
public class OperatorController {

    private final BankAccountRepository accountRepo;
    private final PaymentRepository paymentRepo;
    private final GridlockResolutionService gridlockService;

    public OperatorController(BankAccountRepository accountRepo,
                              PaymentRepository paymentRepo,
                              GridlockResolutionService gridlockService) {
        this.accountRepo = accountRepo;
        this.paymentRepo = paymentRepo;
        this.gridlockService = gridlockService;
    }

    @Operation(
        summary = "Pobierz wszystkie banki",
        description = """
            Zwraca pełną listę banków uczestniczących w systemie wraz z ich rachunkami rozliczeniowymi.
            Endpoint przeznaczony do zasilenia widoków operatora pokazujących stan uczestników systemu,
            ich saldo, limity zadłużenia oraz status blokady.
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista banków została zwrócona poprawnie.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BankAccount.class),
            examples = @ExampleObject(
                name = "Lista banków",
                value = """
                    [
                      {
                        "bankId": "PKO",
                        "bankName": "PKO Bank Polski",
                        "balance": 15000000.00,
                        "debtLimit": 30000000.00,
                        "blocked": false,
                        "overlimitSince": null,
                        "blockedAt": null
                      },
                      {
                        "bankId": "PEKAO",
                        "bankName": "Bank Pekao SA",
                        "balance": -5000000.00,
                        "debtLimit": 25000000.00,
                        "blocked": false,
                        "overlimitSince": null,
                        "blockedAt": null
                      }
                    ]
                    """
            )
        )
    )
    @GetMapping("/banks")
    public List<BankAccount> allBanks() {
        return accountRepo.findAll();
    }

    @Operation(
        summary = "Zablokuj bank",
        description = """
            Blokuje udział banku w systemie.
            Zablokowany bank nie może wysyłać ani odbierać nowych przelewów
            do czasu odblokowania przez operatora lub automatyki systemowej.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Bank został zablokowany.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object"),
                examples = @ExampleObject(
                    value = """
                        {
                          "bankId": "PKO",
                          "status": "BLOCKED"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nie znaleziono banku o podanym identyfikatorze.",
            content = @Content(schema = @Schema(implementation = Void.class))
        )
    })
    @PostMapping("/banks/{bankId}/block")
    public ResponseEntity<Map<String, Object>> block(
            @Parameter(description = "Identyfikator banku.", example = "PKO")
            @PathVariable String bankId) {
        gridlockService.blockBank(bankId);
        return ResponseEntity.ok(Map.of("bankId", bankId, "status", "BLOCKED"));
    }

    @Operation(
        summary = "Odblokuj bank",
        description = """
            Przywraca udział banku w systemie po ustaniu przyczyny blokady.
            Operacja operatorska stosowana po odzyskaniu płynności lub zakończeniu sytuacji nadzwyczajnej.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Bank został odblokowany.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object"),
                examples = @ExampleObject(
                    value = """
                        {
                          "bankId": "PKO",
                          "status": "UNBLOCKED"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nie znaleziono banku o podanym identyfikatorze.",
            content = @Content(schema = @Schema(implementation = Void.class))
        )
    })
    @PostMapping("/banks/{bankId}/unblock")
    public ResponseEntity<Map<String, Object>> unblock(
            @Parameter(description = "Identyfikator banku.", example = "PKO")
            @PathVariable String bankId) {
        gridlockService.unblockBank(bankId);
        return ResponseEntity.ok(Map.of("bankId", bankId, "status", "UNBLOCKED"));
    }

    @Operation(
        summary = "Pobierz sytuacje nadzwyczajne",
        description = """
            Zwraca listę banków znajdujących się ponad limitem zadłużenia
            albo wymagających interwencji operatora z powodu utraty płynności.
            Endpoint zasila widok alarmowy GUI operatora.
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista banków w stanie alarmowym.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BankAccount.class),
            examples = @ExampleObject(
                name = "Banki wymagające interwencji",
                value = """
                    [
                      {
                        "bankId": "PKO",
                        "bankName": "PKO Bank Polski",
                        "balance": -35000000.00,
                        "debtLimit": 30000000.00,
                        "blocked": false,
                        "overlimitSince": "2026-05-27T15:00:00",
                        "blockedAt": null
                      }
                    ]
                    """
            )
        )
    )
    @GetMapping("/emergencies")
    public List<BankAccount> emergencies() {
        return accountRepo.findOverLimit();
    }

    @Operation(
        summary = "Pobierz kolejkę gridlock",
        description = """
            Zwraca przelewy aktualnie przetrzymywane w kolejce gridlock resolution.
            Endpoint służy do monitorowania zatorów płatniczych i przelewów, które nie mogły zostać
            natychmiast rozliczone z powodu ograniczeń płynnościowych.
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista przelewów wstrzymanych przez gridlock.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Payment.class),
            examples = @ExampleObject(
                name = "Przelewy w gridlock",
                value = """
                    [
                      {
                        "id": 15,
                        "senderBankId": "PKO",
                        "receiverBankId": "PEKAO",
                        "amount": 12000000.00,
                        "status": "GRIDLOCK_HELD",
                        "submittedAt": "2026-05-27T16:10:00",
                        "settledAt": null
                      }
                    ]
                    """
            )
        )
    )
    @GetMapping("/gridlock")
    public List<Payment> gridlockQueue() {
        return paymentRepo.findByStatus(PaymentStatus.GRIDLOCK_HELD);
    }

    @Operation(
        summary = "Pobierz przelewy operatora",
        description = """
            Zwraca przelewy z możliwością filtrowania po statusie i/lub identyfikatorze banku.
            Parametr bankId filtruje przelewy, w których wskazany bank występuje jako nadawca albo odbiorca.
            Parametr status powinien odpowiadać jednej z wartości enuma PaymentStatus.
            Jeśli podane są oba parametry, zwracane są przelewy danego banku o wskazanym statusie.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista przelewów spełniających kryteria filtrowania.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Payment.class),
                examples = @ExampleObject(
                    name = "Przelewy operatora",
                    value = """
                        [
                          {
                            "id": 21,
                            "senderBankId": "PKO",
                            "receiverBankId": "MBANK",
                            "amount": 3500000.00,
                            "status": "SETTLED",
                            "submittedAt": "2026-05-27T09:15:00",
                            "settledAt": "2026-05-27T09:15:03"
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Niepoprawna wartość parametru status.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object"),
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "Niepoprawna wartość parametru status: DONE"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/payments")
    public List<Payment> allPayments(
            @Parameter(
                name = "status",
                in = ParameterIn.QUERY,
                description = "Status przelewu. Dozwolone wartości wynikają z enuma PaymentStatus.",
                example = "SETTLED",
                schema = @Schema(implementation = PaymentStatus.class)
            )
            @RequestParam(required = false) String status,

            @Parameter(
                name = "bankId",
                in = ParameterIn.QUERY,
                description = "Identyfikator banku występującego jako nadawca lub odbiorca przelewu.",
                example = "PKO"
            )
            @RequestParam(required = false) String bankId) {

        PaymentStatus parsedStatus = parseStatus(status);

        if (parsedStatus != null && bankId != null) {
            return paymentRepo.findBySenderBankIdOrReceiverBankId(bankId, bankId)
                    .stream()
                    .filter(p -> p.getStatus() == parsedStatus)
                    .toList();
        }
        if (parsedStatus != null) {
            return paymentRepo.findByStatus(parsedStatus);
        }
        if (bankId != null) {
            return paymentRepo.findBySenderBankIdOrReceiverBankId(bankId, bankId);
        }
        return paymentRepo.findAll();
    }

    @Operation(
        summary = "Pobierz przelewy rozliczone dzisiaj",
        description = """
            Zwraca listę przelewów o statusie SETTLED, które zostały rozliczone od początku bieżącego dnia.
            Endpoint zasila widoki dziennego rozrachunku i monitoringu aktywności systemu.
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista dzisiejszych rozliczonych przelewów.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = Payment.class),
            examples = @ExampleObject(
                name = "Dzisiejsze przelewy rozliczone",
                value = """
                    [
                      {
                        "id": 31,
                        "senderBankId": "PKO",
                        "receiverBankId": "ING",
                        "amount": 8000000.00,
                        "status": "SETTLED",
                        "submittedAt": "2026-05-27T11:00:00",
                        "settledAt": "2026-05-27T11:00:02"
                      }
                    ]
                    """
            )
        )
    )
    @GetMapping("/payments/settled-today")
    public List<Payment> settledToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return paymentRepo.findByStatusAndSettledAtAfter(PaymentStatus.SETTLED, startOfDay);
    }

    private PaymentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Niepoprawna wartość parametru status: " + status
            );
        }
    }
}