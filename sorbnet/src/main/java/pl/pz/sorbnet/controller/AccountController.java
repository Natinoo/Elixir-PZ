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
import pl.pz.sorbnet.repository.BankAccountRepository;
import pl.pz.sorbnet.service.SorbnetPaymentService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sorbnet/accounts")
@Tag(
    name = "Settlement Accounts",
    description = "API rachunków rozliczeniowych banków uczestniczących w systemie SORBNet. Umożliwia podgląd stanu rachunków, statusu płynności oraz symulację zasilenia środków przez bank centralny lub inny bank."
)
public class AccountController {

    private final BankAccountRepository accountRepo;
    private final SorbnetPaymentService paymentService;

    public AccountController(BankAccountRepository accountRepo,
                             SorbnetPaymentService paymentService) {
        this.accountRepo = accountRepo;
        this.paymentService = paymentService;
    }

    @Operation(
        summary = "Pobierz wszystkie rachunki rozliczeniowe",
        description = """
            Zwraca listę wszystkich banków uczestniczących w systemie wraz z ich rachunkami rozliczeniowymi.
            Endpoint przeznaczony głównie do podglądu administracyjnego, monitoringu systemu oraz zasilenia GUI operatora.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista rachunków rozliczeniowych została zwrócona poprawnie.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BankAccount.class),
                examples = @ExampleObject(
                    name = "Lista wszystkich rachunków",
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
    })
    @GetMapping
    public List<BankAccount> listAll() {
        return accountRepo.findAll();
    }

    @Operation(
        summary = "Pobierz rachunek rozliczeniowy banku",
        description = """
            Zwraca szczegóły rachunku rozliczeniowego wskazanego banku.
            Odpowiedź obejmuje między innymi identyfikator banku, nazwę, bieżące saldo,
            limit zadłużenia oraz informacje o ewentualnej blokadzie lub przekroczeniu limitu.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Znaleziono rachunek rozliczeniowy banku.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BankAccount.class),
                examples = @ExampleObject(
                    name = "Przykładowy rachunek",
                    value = """
                        {
                          "bankId": "BANK_A",
                          "bankName": "Bank A",
                          "balance": 5000000.00,
                          "debtLimit": 2000000.00,
                          "blocked": false,
                          "overlimitSince": null,
                          "blockedAt": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nie znaleziono banku o podanym identyfikatorze.",
            content = @Content
        )
    })
    @GetMapping("/{bankId}")
    public BankAccount get(
            @Parameter(
                description = "Unikalny identyfikator banku uczestniczącego w systemie.",
                example = "BANK_A"
            )
            @PathVariable String bankId) {
        return accountRepo.findById(bankId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Nie znaleziono banku: " + bankId));
    }

    @Operation(
        summary = "Pobierz bieżący status rachunku banku",
        description = """
            Zwraca zagregowany status rachunku rozliczeniowego banku z punktu widzenia płynności i rozrachunku.
            Endpoint może być wykorzystywany przez GUI banku do prezentacji aktualnego salda, limitu zadłużenia,
            informacji o przekroczeniu limitu, stanie blokady oraz innych danych operacyjnych związanych z uczestnictwem banku w systemie.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Status rachunku został zwrócony poprawnie.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object"),
                examples = {
                    @ExampleObject(
                        name = "Rachunek aktywny",
                        value = """
                            {
                              "bankId": "BANK_A",
                              "bankName": "Bank A",
                              "balance": 5000000.00,
                              "debtLimit": 2000000.00,
                              "availableCredit": 7000000.00,
                              "minDepositToRestore": 0.00,
                              "blocked": false,
                              "overlimitSince": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Przekroczony limit",
                        value = """
                            {
                              "bankId": "BANK_A",
                              "bankName": "Bank A",
                              "balance": -3500000.00,
                              "debtLimit": 2000000.00,
                              "availableCredit": -1500000.00,
                              "minDepositToRestore": 1500000.00,
                              "blocked": false,
                              "overlimitSince": "2026-06-09T11:00:00"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Bank zablokowany",
                        value = """
                            {
                              "bankId": "BANK_B",
                              "bankName": "Bank B",
                              "balance": -4500000.00,
                              "debtLimit": 2000000.00,
                              "availableCredit": -2500000.00,
                              "minDepositToRestore": 2500000.00,
                              "blocked": true,
                              "overlimitSince": "2026-06-09T09:00:00"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nie znaleziono banku o podanym identyfikatorze.",
            content = @Content
        )
    })
    @GetMapping("/{bankId}/status")
    public ResponseEntity<Map<String, Object>> status(
            @Parameter(
                description = "Identyfikator banku, dla którego pobierany jest bieżący status rachunku rozliczeniowego.",
                example = "BANK_A"
            )
            @PathVariable String bankId) {
        return ResponseEntity.ok(paymentService.getAccountStatus(bankId));
    }

    @Operation(
        summary = "Zasil rachunek rozliczeniowy banku",
        description = """
            Symuluje dopływ płynności na rachunek rozliczeniowy wskazanego banku.
            Operacja może reprezentować zasilenie przez bank centralny albo przez inny bank uczestniczący w systemie.
            Endpoint służy do odtwarzania scenariuszy odzyskiwania płynności po przekroczeniu limitu zadłużenia,
            uzupełnienia środków przed zamknięciem sesji nettingowej albo przywrócenia zdolności rozrachunkowej banku.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Zasilenie rachunku zostało wykonane poprawnie.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object"),
                examples = {
                    @ExampleObject(
                        name = "Zasilenie przez NBP",
                        value = """
                            {
                              "bankId": "BANK_A",
                              "depositedAmount": 2000000.00,
                              "balanceBefore": -3500000.00,
                              "balanceAfter": -1500000.00,
                              "overlimitCleared": false
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Zasilenie przez inny bank",
                        value = """
                            {
                              "bankId": "BANK_A",
                              "depositedAmount": 4000000.00,
                              "balanceBefore": -3500000.00,
                              "balanceAfter": 500000.00,
                              "overlimitCleared": true
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Zasilenie BANK_C przez NBP — przywrócenie płynności",
                        value = """
                            {
                              "bankId": "BANK_C",
                              "depositedAmount": 1500000.00,
                              "balanceBefore": -2500000.00,
                              "balanceAfter": -1000000.00,
                              "overlimitCleared": false
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Niepoprawna kwota lub błędne dane wejściowe.",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nie znaleziono banku docelowego lub banku źródłowego.",
            content = @Content
        )
    })
    @PostMapping("/{bankId}/deposit")
    public ResponseEntity<Map<String, Object>> deposit(
            @Parameter(
                description = "Identyfikator banku, którego rachunek rozliczeniowy ma zostać zasilony.",
                example = "BANK_A"
            )
            @PathVariable String bankId,

            @Parameter(
                name = "amount",
                in = ParameterIn.QUERY,
                required = true,
                description = "Kwota zasilenia rachunku rozliczeniowego.",
                example = "2000000.00"
            )
            @RequestParam BigDecimal amount,

            @Parameter(
                name = "sourceBankId",
                in = ParameterIn.QUERY,
                description = "Identyfikator podmiotu zasilającego rachunek. Domyślnie NBP, ale może to być również BANK_B lub BANK_C.",
                example = "NBP"
            )
            @RequestParam(required = false, defaultValue = "NBP") String sourceBankId) {
        return ResponseEntity.ok(paymentService.simulateDeposit(bankId, amount, sourceBankId));
    }
}