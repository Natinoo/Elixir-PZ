package pl.pz.sorbnet.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

@Entity
@Table(name = "bank_settlement_accounts")
@Schema(
        name = "BankSettlementAccount",
        description = "Rachunek rozliczeniowy banku w systemie SORBNet."
)
public class BankSettlementAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @Schema(description = "Wewnętrzny identyfikator rachunku.", example = "1")
    private Long id;

    @Column(name = "bank_id", nullable = false, length = 50)
    @Schema(
            description = "Identyfikator banku właściciela rachunku.",
            example = "BANK_A",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String bankId;

    @Column(name = "account_number", nullable = false, length = 64, unique = true)
    @Schema(
            description = "Numer rachunku rozliczeniowego banku w SORBNet.",
            example = "11111100000000000000000001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String accountNumber;

    @Column(name = "is_default", nullable = false)
    @Schema(
            description = "Czy rachunek jest domyślnym rachunkiem rozliczeniowym banku.",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean isDefault;

    public BankSettlementAccount() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}