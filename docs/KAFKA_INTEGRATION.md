# Dokumentacja Integracji Asynchronicznej (Kafka): Sorbnet ↔ Elixir

Dokument opisuje architekturę sterowaną zdarzeniami (Event-Driven) pomiędzy aplikacjami **Sorbnet** oraz **Elixir** (w tym Express Elixir). Komunikacja odbywa się w pełni asynchronicznie przy wykorzystaniu Apache Kafka i formatu XML (JAXB).

---

## 1. Używane Topiki Kafka

| Nazwa Topic | Rola Sorbnet | Rola Elixir | Zastosowanie biznesowe |
| :--- | :--- | :--- | :--- |
| `payments.sorbnet` | **Konsument** | **Producent** | Standardowe zlecenia płatności (RTGS/Netting) wychodzące do Sorbnet. |
| `payments.express.sorbnet`| **Konsument** | **Producent** | Przyspieszone zlecenia z systemu Express Elixir do Sorbnet. |
| `responses.elixir` | **Producent** | **Konsument** | Statusy przetworzonych przelewów z Sorbnet wracające do głównego Elixira. |
| `responses.elixir-express`| **Producent** | **Konsument** | Statusy przetworzonych przelewów z Sorbnet wracające do Express Elixira. |
| `payments.elixir` | N/A | Konsument/Producent | Wewnętrzny topik Elixira do kolejkowania przelewów przed sesją rozrachunkową. |
| `events.gridlock` | Producent / Konsument | N/A | Zdarzenia techniczne Sorbnet: zawieszenie przelewów z powodu braku płynności. |
| `events.emergency` | Producent / Konsument | N/A | Alerty Sorbnet o ryzyku przekroczenia limitów zadłużenia. |

---

## 2. Macierz Przekierowań (Routing Matrix)

Procesowanie w systemie SORBNet zachowuje ścisłą separację kanałów standardowych (`ELIXIR`) oraz przyspieszonych (`ELIXIR_EXPRESS`). 

| 📥 Topik Wejściowy (Request) | ⚙️ Źródło Sorbnet (Source) | 📤 Topik Wyjściowy (Response/Error) |
| :--- | :--- | :--- |
| `payments.sorbnet` | `ELIXIR` | `responses.elixir` |
| `payments.express.sorbnet` | `ELIXIR_EXPRESS` | `responses.elixir-express` |

---

## 3. Format Komunikatów

Do przesyłania danych pomiędzy systemami wykorzystywany jest format **XML** (JAXB).

### Zlecenie przelewu (Elixir ➔ Sorbnet)
Root tag: `<Payment>` (po stronie Elixira) / `<SorbnetPaymentRequest>` (po stronie Sorbnetu).

| Pole | Typ | Wymagane | Opis / Przykład |
| :--- | :--- | :---: | :--- |
| `paymentId` | String (UUID) | **Tak** | Unikalny identyfikator generowany przez aplikację nadawczą (Elixir). |
| `amount` | Double | **Tak** | Kwota przelewu (musi być `> 0`). |
| `currency` | String | **Tak** | Kod waluty (walidowane w Elixir: dozwolone tylko `PLN`). |
| `senderBankId` | String | **Tak** | Identyfikator banku nadawcy (np. `BANK_A`). |
| `receiverBankId` | String | **Tak** | Identyfikator banku odbiorcy (np. `BANK_B`). |
| `senderAccount` | String | **Tak** | Numer rachunku nadawcy. |
| `receiverAccount` | String | **Tak** | Numer rachunku odbiorcy. |
| `title` | String | **Tak** | Tytuł płatności (max 140 znaków). |

### Odpowiedź z wynikiem lub błędem (Sorbnet ➔ Elixir)
Komunikat wystawiany przez system Sorbnet na topikach response po przetworzeniu lub błędzie. Root tag: `<SorbnetPaymentResponse>`.

| Pole | Typ | Opis |
| :--- | :--- | :--- |
| `paymentId` | String | Identyfikator przelewu z pierwotnego żądania. |
| `status` | String | Wynik biznesowy Sorbnet: `SETTLED`, `REJECTED`, lub `GRIDLOCK_HELD`. |
| `message` | String | Opis wyniku (np. `Payment processed`, `XML parse error`, `SENDER_BLOCKED`). |
| `senderBankId` | String | Identyfikator banku nadawcy. |
| `receiverBankId`| String | Identyfikator banku odbiorcy. |
| `senderAccount` | String | Rachunek nadawcy. |
| `receiverAccount`| String | Rachunek odbiorcy. |
| `amount` | BigDecimal | Kwota przetwarzanego przelewu. |
| `settledAt` | String | Data i czas wygenerowania odpowiedzi (ISO-8601). |

---

## 4. Reguły Biznesowe Sorbnet (Procesowanie RTGS)

Aplikacja **Sorbnet** przetwarza przelewy w czasie rzeczywistym i nadaje im jeden z trzech ostatecznych statusów:

* **`SETTLED`** (Sukces)
    * *Warunki:* Saldo banku nadawcy (`balance + debtLimit`) jest wystarczające, konta rozliczeniowe zgodne.
    * *Efekt:* Środki natychmiastowo księgowane. Transakcja trwale rozliczona.
* **`GRIDLOCK_HELD`** (Brak płynności)
    * *Warunki:* Przelew spowodowałby przekroczenie limitu zadłużenia (`debtLimit`) przez bank nadawcy.
    * *Efekt:* Przelew zawieszony (w kolejce Gridlock). Generowane alerty bezpieczeństwa do GUI i na topiki Kafka.
* **`REJECTED`** (Odrzucenie techniczne)
    * *Warunki:* Nierozpoznany/zablokowany bank, niezgodność rachunku z bankiem nadawcy (`SENDER_ACCOUNT_MISMATCH`) lub błędy formatu komunikatu.
    * *Efekt:* Płatność bezpowrotnie odrzucana.

---

## 5. Architektura i Cykl Życia w Systemie Elixir

Aplikacja **Elixir** odpowiada za agregację standardowych płatności, przeprowadzanie sesji rozliczeniowych z wykorzystaniem mechanizmu **Nettingu** (kompensaty wielostronnej) oraz asynchroniczną synchronizację statusów z bazą systemu Sorbnet.

### 5.1. Tłumaczenie Statusów (Mapping Matrix)
System Elixir konsumuje XML zwrotny z Sorbnetu (`responses.elixir`) w klasie `SorbnetResponseConsumer` i automatycznie tłumaczy stany na swoje wewnętrzne statusy w bazie danych (tabela `payments`, kolumna `status` enum `PaymentStatus`):

| Otrzymany status XML (Sorbnet) | Tłumaczenie do bazy DB (Elixir) | Uwagi |
| :--- | :--- | :--- |
| `SETTLED` | `PROCESSED` | Rozrachunek potwierdzony w NBP. |
| `REJECTED` | `REJECTED` | Błąd formalny, brak autoryzacji lub blokada konta. |
| `GRIDLOCK_HELD` | `BLOCKED` | Wstrzymane w Sorbnecie (oczekiwanie na płynność banku nadawcy). |

> **Zabezpieczenie przed nieznanym stanem:** Jeśli z Sorbnetu powróci jakikolwiek inny status, aplikacja Elixir rzuci techniczny wyjątek `IllegalArgumentException`, a status w bazie Elixira nie zostanie zmieniony.

### 5.2. Cykl Życia Przelewu (Workflow Elixir)

1. **Walidacja Wejściowa:** API przyjmuje DTO i serwis `ElixirPaymentService` sprawdza kompletność danych biznesowych (rachunki, banki nadawcy/odbiorcy, ujemne kwoty).
2. **Persistence:** Zlecenie jest zapisywane w bazie Elixira ze statusem inicjalnym **`QUEUED`** (oznaczającym wejście do sesji rozrachunkowej). Wyznaczany jest unikalny `paymentId` (UUID).
3. **Kolejkowanie Zdarzeń:** Elixir wysyła wewnętrznie komunikat na Kafka topik `payments.elixir`, z którego czyta `ElixirKafkaConsumer` i ładuje obiekt do bieżącej instancji `SessionService`.
4. **Agregacja i Netting:**
   * Przelewy zbierane są w pamięci do wyznaczonego czasu (np. Sesje: Poranna `09:30`, Południowa `13:30`, Popołudniowa `16:00`).
   * Serwis `NettingService` wylicza wzajemne kompensaty między bankami (saldo transakcji), minimalizując liczbę koniecznych przelewów w samym systemie RTGS Sorbnet.
5. **Synchronizacja:** Zlecone transfery/rezultaty trafiają asynchronicznie do systemu Sorbnet.
6. **Aktualizacja Zwrotna:** Po przetworzeniu żądania przez NBP, `SorbnetResponseConsumer` odczytuje wiadomość, wyciąga status i wywołuje `updatePaymentStatus()`, zamykając cykl.
