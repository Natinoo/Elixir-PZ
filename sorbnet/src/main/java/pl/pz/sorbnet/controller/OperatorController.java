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
                        "bankId": "NBP",
                        "bankName": "Narodowy Bank Polski",
                        "balance": 10000000.00,
                        "debtLimit": 0.00,
                        "blocked": false,
                        "overlimitSince": null,
                        "blockedAt": null
                      },
                      {
                        "bankId": "BANK_A",
                        "bankName": "Bank A",
                        "balance": 5000000.00,
                        "debtLimit": 2000000.00,
                        "blocked": false,
                        "overlimitSince": null,
                        "blockedAt": null
                      },
                      {
                        "bankId": "BANK_B",
                        "bankName": "Bank B",
                        "balance": 5000000.00,
                        "debtLimit": 2000000.00,
                        "blocked": false,
                        "overlimitSince": null,
                        "blockedAt": null
                      },
                      {
                        "bankId": "BANK_C",
                        "bankName": "Bank C",
                        "balance": 5000000.00,
                        "debtLimit": 2000000.00,
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
                          "bankId": "BANK_A",
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
            @Parameter(description = "Identyfikator banku.", example = "BANK_A")
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
                          "bankId": "BANK_A",
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
            @Parameter(description = "Identyfikator banku.", example = "BANK_A")
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
                        "bankId": "BANK_A",
                        "bankName": "Bank A",
                        "balance": -3500000.00,
                        "debtLimit": 2000000.00,
                        "blocked": false,
                        "overlimitSince": "2026-06-09T11:00:00",
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
                        "paymentId": "SORB-20260609-0002",
                        "senderBankId": "BANK_A",
                        "receiverBankId": "BANK_C",
                        "senderAccount": "11111100000000000000000001",
                        "receiverAccount": "33333300000000000000000003",
                        "amount": 8000000.00,
                        "currency": "PLN",
                        "title": "Duży przelew rozrachunkowy",
                        "status": "GRIDLOCK_HELD",
                        "createdAt": "2026-06-09T12:00:00",
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
                examples = {
                    @ExampleObject(
                        name = "Wszystkie rozliczone",
                        value = """
                            [
                              {
                                "paymentId": "SORB-20260609-0001",
                                "senderBankId": "BANK_A",
                                "receiverBankId": "BANK_B",
                                "senderAccount": "11111100000000000000000001",
                                "receiverAccount": "22222200000000000000000002",
                                "amount": 1000000.00,
                                "currency": "PLN",
                                "title": "Rozrachunek międzybankowy",
                                "status": "SETTLED",
                                "createdAt": "2026-06-09T13:00:00",
                                "settledAt": "2026-06-09T13:00:01"
                              }
                            ]
                            """
                    ),
                    @ExampleObject(
                        name = "Odrzucone dla BANK_B",
                        value = """
                            [
                              {
                                "paymentId": "SORB-20260609-0005",
                                "senderBankId": "BANK_B",
                                "receiverBankId": "BANK_C",
                                "senderAccount": "22222200000000000000000002",
                                "receiverAccount": "33333300000000000000000003",
                                "amount": 500000.00,
                                "currency": "PLN",
                                "title": "Przelew odrzucony",
                                "status": "REJECTED",
                                "createdAt": "2026-06-09T10:00:00",
                                "settledAt": null
                              }
                            ]
                            """
                    )
                }
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
                description = "Status przelewu. Dozwolone wartości: SETTLED, GRIDLOCK_HELD, REJECTED.",
                example = "SETTLED",
                schema = @Schema(implementation = PaymentStatus.class)
            )
            @RequestParam(required = false) String status,

            @Parameter(
                name = "bankId",
                in = ParameterIn.QUERY,
                description = "Identyfikator banku występującego jako nadawca lub odbiorca przelewu.",
                example = "BANK_A"
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
                        "paymentId": "SORB-20260609-0001",
                        "senderBankId": "BANK_A",
                        "receiverBankId": "BANK_B",
                        "senderAccount": "11111100000000000000000001",
                        "receiverAccount": "22222200000000000000000002",
                        "amount": 1000000.00,
                        "currency": "PLN",
                        "title": "Rozrachunek międzybankowy",
                        "status": "SETTLED",
                        "createdAt": "2026-06-09T13:00:00",
                        "settledAt": "2026-06-09T13:00:01"
                      },
                      {
                        "paymentId": "SORB-20260609-0004",
                        "senderBankId": "NBP",
                        "receiverBankId": "BANK_A",
                        "senderAccount": "10100100000000000000000000",
                        "receiverAccount": "11111100000000000000000001",
                        "amount": 3000000.00,
                        "currency": "PLN",
                        "title": "Zasilenie rachunku rozrachunkowego przez NBP",
                        "status": "SETTLED",
                        "createdAt": "2026-06-09T12:30:00",
                        "settledAt": "2026-06-09T12:30:01"
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