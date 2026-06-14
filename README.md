# Elixir / Express Elixir / SORBNET

Projekt ma na celu odtworzenie architektury polskiego systemu rozliczeń międzybankowych w uproszczonym modelu 1:1, z podziałem na trzy główne systemy płatnicze: Elixir, Express Elixir oraz SORBNET. Każdy z nich został zaimplementowany jako oddzielny, autonomiczny mikroserwis.

## 1. Opis Systemów i Zmiana Paradygmatu Rozrachunku

**Elixir** – system sesyjnego rozliczania przelewów detalicznych. Płatności są przyjmowane, kolejkowane w ramach sesji i rozliczane w wyznaczonych oknach clearingowych.

**Express Elixir** – system płatności natychmiastowych. Płatności są przekazywane i przetwarzane w czasie rzeczywistym bez oczekiwania na okna sesyjne.

**SORBNET** – system RTGS (Real-Time Gross Settlement) prowadzony przez Bank Centralny, przeznaczony do rozrachunku wysokokwotowego oraz ostatecznego rozliczania transakcji w czasie rzeczywistym.

> **Ważna uwaga architektoniczna:** W zoptymalizowanej wersji systemu transakcje detaliczne (zarówno sesyjne, jak i natychmiastowe) są rozliczane i logowane lokalnie w swoich silnikach płatniczych. Stały forwarding każdego przelewu do SORBNET-u został wyłączony. Komunikaty trafiają do SORBNET-u wyłącznie w dwóch przypadkach:
> - Gdy transakcja wymaga bezpośredniego rozrachunku brutto na rachunkach w banku centralnym (np. netting przy braku płynności).
> - W sytuacjach awaryjnych lub niedoborów płynnościowych (Liquidity Requests), generowanych automatycznie przez silniki i zatwierdzanych operatorsko w GUI SORBNET-u.

## 2. Architektura Systemu i Komunikacja Asynchroniczna (Kafka)

Projekt działa w architekturze mikroserwisowej (distributed services). Komunikacja pomiędzy modułami realizowana jest asynchronicznie poprzez platformę Apache Kafka.

### Rejestr Topików Kafka (Topics Registry)

Dla zapewnienia pełnej izolacji procesów natychmiastowych od sesyjnych, komunikacja została rozdzielona na dedykowane topiki.

**Zasada partycjonowania:** Kluczem komunikatu (Message Key) dla transakcji oraz żądań płynności jest zawsze biznesowy identyfikator operacji (`paymentId` lub `requestId`). Gwarantuje to zachowanie kolejności komunikatów (Partition Ordering) w obrębie jednej operacji.

| Nazwa Topiku | Producent | Konsument (Grupa) | Format Payload | Opis Biznesowy |
|---|---|---|---|---|
| `payments.sorbnet` | ELIXIR | SORBNET (`sorbnet-group`) | XML (`<Document>`) | Zlecenia płatnicze ELIXIR kierowane do rozrachunku w SORBNET. |
| `payments.express.sorbnet` | ELIXIR EXPRESS | SORBNET (`sorbnet-group`) | XML (`<Document>`) | Zlecenia płatnicze Express wymagające interwencji rozrachunkowej w SORBNET. |
| `liquidity.requests.sorbnet` | ELIXIR | SORBNET (`sorbnet-group`) | XML (`<Document>`) | Żądania płynnościowe (camt.050) w przypadku braku środków na rozliczenie sesji ELIXIR. |
| `liquidity.requests.express.sorbnet` | ELIXIR EXPRESS | SORBNET (`sorbnet-group`) | XML (`<Document>`) | Żądania płynnościowe dla systemu Express, gdy bank nie ma pokrycia. |
| `responses.elixir` | SORBNET | ELIXIR (`elixir-group`) | XML (`<Document>`) | Komunikaty zwrotne ze statusem przetworzenia dla modułu ELIXIR. |
| `responses.elixir-express` | SORBNET | ELIXIR EXPRESS (`elixir-express-group`) | XML (`<Document>`) | Komunikaty zwrotne ze statusem przetworzenia dla modułu Express. |
| `notifications.banks` | SORBNET | SORBNET (`sorbnet-group`) | Text / XML | Globalne powiadomienia o rozrachunkach (Websocket `/topic/settlements`). |
| `events.emergency` | SORBNET | SORBNET (`sorbnet-group`) | Text / XML | Powiadomienia o sytuacjach awaryjnych (Websocket `/topic/emergency`). |
| `events.gridlock` | SORBNET | SORBNET (`sorbnet-group`) | Text / XML | Detekcja zakleszczeń płynnościowych w banku centralnym (Websocket `/topic/gridlock`). |

### Strategia Obsługi Błędów i Niezawodności (Fault Tolerance)

- **Izolacja Błędów Parsowania (JAXBException):** Jeśli odebrany komunikat XML jest uszkodzony, konsument przechwytuje wyjątek, pobiera klucz wiadomości (Record Key) jako identyfikator awaryjny i automatycznie odsyła na topik zwrotny komunikat z flagą `REJECTED` oraz opisem `"XML parse error"`.
- **Obsługa Błędów Aplikacyjnych (Exception):** W przypadku awarii bazy danych lub logiki biznesowej wewnątrz `SorbnetPaymentService`, blok `catch` generuje automatyczną odpowiedź o statusie `REJECTED` i przesyła ją na odpowiedni kanał zwrotny (`responses.*`) z komunikatem `"Processing error: [treść wyjątku]"`.
- **Izolacja Wydajnościowa:** Awaria lub spowolnienie konsumenta w module sesyjnym ELIXIR nie wpływa na wydajność ani stabilność przetwarzania komunikatów czasu rzeczywistego w module ELIXIR EXPRESS.

## 3. Standard ISO 20022 - Wiedza Domenowa i Specyfikacja

Komunikacja z systemem SORBNET wykorzystuje ujednolicony, międzynarodowy standard ISO 20022 oparty na strukturach XML (komunikaty MX). Zapewnia on ścisłą walidację schematami XSD oraz bogatą strukturę danych (identyfikatory, kontener danych lokalnych).

### Struktura Nazewnictwa Komunikatów

Każdy komunikat posiada unikalny identyfikator typu (np. `pacs.008.001.08`):

- **pacs** (Payments Clearing and Settlement) – Rozliczenia i czyszczenie płatności międzybankowych.
- **camt** (Cash Management) – Zarządzanie gotówką i płynnością rachunków.

### 3.1 Zlecenia Płatnicze (SorbnetPaymentDto - odpowiednik pacs.008)

Przesyłane na topiki `payments.sorbnet` oraz `payments.express.sorbnet`. Zawiera nagłówek grupy (`GrpHdr`) oraz sekcję danych uzupełniających (`SplmtryData`), która identyfikuje system źródłowy.

```xml
<Document>
    <FIToFICstmrCdtTrf>
        <GrpHdr>
            <MsgId>SORB-1718362000000</MsgId>
            <CreDtTm>2026-06-14T12:00:00</CreDtTm>
            <NbOfTxs>1</NbOfTxs>
            <TtlIntrBkSttlmAmt Ccy="PLN">150000.00</TtlIntrBkSttlmAmt>
            <SttlmInf>
                <SttlmMtd>CLRG</SttlmMtd>
                <ClrSys>
                    <Cd>ELIXIR_EXPRESS</Cd>
                </ClrSys>
            </SttlmInf>
        </GrpHdr>
        <CdtTrfTxInf>
            <PmtId>
                <InstrId>TX-998231</InstrId>
                <EndToEndId>TX-998231</EndToEndId>
                <TxId>TX-998231</TxId>
            </PmtId>
            <IntrBkSttlmAmt Ccy="PLN">150000.00</IntrBkSttlmAmt>
            <Dbtr><Nm>Jan Kowalski</Nm></Dbtr>
            <DbtrAcct><Id><IBAN>PL12102030400000111122223333</IBAN></Id></DbtrAcct>
            <DbtrAgt><FinInstnId><BICFI>BANKPLPW</BICFI></FinInstnId></DbtrAgt>
            <Cdtr><Nm>Firma ABC</Nm></Cdtr>
            <CdtrAcct><Id><IBAN>PL98403020100000444455556666</IBAN></Id></CdtrAcct>
            <CdtrAgt><FinInstnId><BICFI>BANKPLSA</BICFI></FinInstnId></CdtrAgt>
            <RmtInf><Ustrd>Zapłata za fakturę FV/100/2026</Ustrd></RmtInf>
            <SplmtryData>
                <Envlp>
                    <ServiceCode>ELIXIR_EXPRESS</ServiceCode>
                    <SenderBankId>BANKPLPW</SenderBankId>
                    <ReceiverBankId>BANKPLSA</ReceiverBankId>
                </Envlp>
            </SplmtryData>
        </CdtTrfTxInf>
    </FIToFICstmrCdtTrf>
</Document>
```

### 3.2 Żądanie Płynności (LiquidityTransferRequestDto - odpowiednik camt.050)

Generowane automatycznie i wysyłane na topiki `liquidity.requests.*` w sytuacji wykrycia niedoboru środków na rachunku technologicznym podczas rozliczeń sesyjnych lub natychmiastowych.

```xml
<Document>
    <LiquidityCreditTransferRequest>
        <GrpHdr>
            <MsgId>REQ-77213</MsgId>
            <CreDtTm>2026-06-14T12:01:10</CreDtTm>
        </GrpHdr>
        <TrfInstr>
            <ReqId>REQ-77213</ReqId>
            <SessionId>SESS-2026-02B</SessionId>
            <PaymentId>PMT-LIQ-99</PaymentId>
            <BankId>BANKPLPW</BankId>
            <SourceServiceCode>SORBNET</SourceServiceCode>
            <TargetServiceCode>ELIXIR</TargetServiceCode>
            <SourceAccount>SORB-ACC-112</SourceAccount>
            <TargetAccount>ELIX-TECH-990</TargetAccount>
            <Amt Ccy="PLN">5000000.00</Amt>
            <Reason>Brak środków na rozliczenie sesji II</Reason>
            <SourceHasFunds>true</SourceHasFunds>
        </TrfInstr>
    </LiquidityCreditTransferRequest>
</Document>
```

### 3.3 Odpowiedź Rozrachunkowa (PaymentResponseDto / LiquidityTransferResponseDto)

Komunikat zwrotny produkowany przez SORBNET na topiki `responses.elixir` oraz `responses.elixir-express` po przetworzeniu zlecenia.

```xml
<Document>
    <LiquidityCreditTransferResponse>
        <GrpHdr>
            <MsgId>LIQRESP-REQ-77213</MsgId>
            <CreDtTm>2026-06-14T12:02:00</CreDtTm>
        </GrpHdr>
        <TrfSts>
            <OrgnlTxId>REQ-77213</OrgnlTxId>
            <BankId>BANKPLPW</BankId>
            <SourceServiceCode>SORBNET</SourceServiceCode>
            <TargetServiceCode>ELIXIR</TargetServiceCode>
            <SourceAccount>SORB-ACC-112</SourceAccount>
            <TargetAccount>ELIX-TECH-990</TargetAccount>
            <PaymentId>PMT-LIQ-99</PaymentId>
            <Amt Ccy="PLN">5000000.00</Amt>
            <TxSts>SETTLED</TxSts>
            <AddtlInf>Rozliczono pomyślnie w systemie RTGS</AddtlInf>
            <SettledAt>2026-06-14T12:01:58</SettledAt>
        </TrfSts>
    </LiquidityCreditTransferResponse>
</Document>
```

## 4. Matryce i Słowniki Mapowania

### Matryca Statusów (Status Matrix)

Konsumenci komunikatów zwrotnych w modułach ELIXIR oraz EXPRESS dokonują automatycznej translacji statusów technicznych systemu SORBNET (`TxSts`) na wewnętrzne stany biznesowe płatności (`PaymentStatus`):

| Status SORBNET (TxSts) | Status Lokalny (PaymentStatus) | Opis Biznesowy |
|---|---|---|
| `SETTLED`, `ACSC`, `ACCP`, `ACSP` | `PROCESSED` | Płatność została ostatecznie i nieodwołalnie rozliczona w banku centralnym. |
| `REJECTED`, `RJCT` | `REJECTED` | Odrzucenie zlecenia (np. błąd walidacji XML, krytyczny brak środków na koncie SORBNET). |
| `BLOCKED`, `GRIDLOCK_HELD`, `BLCK`, `PDNG` | `BLOCKED` | Transakcja wstrzymana w algorytmie optymalizacji płynnościowej (gridlock) lub oczekuje na środki. |
| Dowolny inny / Brak wartości | `QUEUED` | Transakcja została zarejestrowana i oczekuje w kolejce zleceń centralnych. |

### Słownik Wybranych Tagów XML ISO 20022

- **GrpHdr** (Group Header) – Nagłówek grupy / komunikatu.
- **CdtTrfTxInf** (Credit Transfer Transaction Information) – Szczegóły transakcji przelewu.
- **IntrBkSttlmAmt** (Interbank Settlement Amount) – Kwota rozrachunku międzybankowego.
- **Dbtr / Cdtr** (Debtor / Creditor) – Dłużnik (Zleceniodawca) / Wierzyciel (Odbiorca).
- **Agt** (Agent) – Instytucja finansowa / Bank pośredniczący (identyfikowany przez kod BICFI).
- **RmtInf / Ustrd** (Remittance Information / Unstructured) – Tytuł płatności w formie nieustrukturyzowanego tekstu.

## 5. Kontekst Krajowy i Przepływ Środków

W polskim systemie bankowym integracja tych standardów ma kluczowe znaczenie:

- **SORBNET (NBP):** Działa jako system rozrachunku brutto w czasie rzeczywistym. Każde zlecenie jest procesowane osobno i nieodwołalnie. Obsługuje płatności wysokokwotowe (standardowo >= 1 mln PLN) oraz operacje płynnościowe.
- **ELIXIR (KIR):** Działa w systemie nettingu wielostronnego. Transakcje detaliczne są zbierane i kompensowane w trzech sesjach clearingowych w ciągu dnia, wyznaczając końcową pozycję netto (debet/kredyt) dla każdego banku.
- **Punkt styku:** Przejście na standard ISO 20022 eliminuje potrzebę uciążliwej transformacji danych. Gdy w systemie ELIXIR brakuje płynności do zamknięcia sesji, automatycznie wygenerowany komunikat płynnościowy bezpośrednio i bezpiecznie odpytuje oraz obciąża rachunek RTGS banku komercyjnego w Narodowym Banku Polskim.

## 6. Struktura Projektu

```plaintext
payment-system/
│
├── elixir/             # Moduł sesyjnego systemu płatności detalicznych
│   └── ...
├── elixir-express/     # Moduł natychmiastowego systemu płatności detalicznych
│   └── ...
├── sorbnet/            # Moduł systemu RTGS (Bank Centralny)
│   └── ...
│
├── docker-compose.yml  # Konteneryzacja infrastruktury (Kafka, Zookeeper)
├── start.bat           # Skrypt uruchamiający środowisko
├── stop.bat            # Skrypt zatrzymujący środowisko
└── README.md           # Główna dokumentacja systemu
```

Każdy moduł to osobna aplikacja Spring Boot posiadająca własny plik `application.properties` oraz niezależną konfigurację połączenia z klastrem Kafka.

## 7. Uruchomienie i Obsługa Systemu

### Wymagania wstępne

- Docker oraz Docker Compose
- Java 17 i Maven (do zbudowania paczek)

### Procedura startowa

1. Uruchom cały system (skrypt podniesie kontenery infrastrukturalne oraz aplikacje):

```bash
./start.bat
```

2. Aby zatrzymać wszystkie usługi i wyczyścić kontenery:

```bash
./stop.bat
```

### Dokumentacja API (Swagger / OpenAPI)

Po poprawnym uruchomieniu aplikacji, interfejsy Swagger UI dostępne są pod adresami:

- **Elixir:** http://localhost:8081/swagger-ui/index.html
- **Elixir-Express:** http://localhost:8082/swagger-ui/index.html
- **Sorbnet:** http://localhost:8083/swagger-ui/index.html

### Interfejsy Graficzne (GUI)

Każdy system udostępnia dedykowany panel pracownika i operatora banku:

- **Elixir GUI:** http://localhost:8081/
- **Elixir-Express GUI:** http://localhost:8082/
- **Sorbnet GUI:** http://localhost:8083/

## 9. Architektura Kompletnego Ekosystemu Rozliczeniowego

### Trzy Filary Systemu Rozliczeniowego

| Cecha / System | ELIXIR (Kompensata) | SORBNET (RTGS) | Express Elixir (Płatności Natychmiastowe) |
|---|---|---|---|
| Model rozliczeń | Netting wielostronny (sesyjny) | RTGS (Brutto w czasie rzeczywistym) | RTGS oparty na przedpłatach (Pre-funding) |
| Dostępność | Dni robocze, 3 okna sesyjne | Dni robocze, godziny operacyjne NBP | 24 / 7 / 365 (Ciągła) |
| Typ transakcji | Detaliczne, niskokwotowe, masowe | Wysokokwotowe (>1mln PLN), pilne, międzybankowe | Detaliczne, natychmiastowe (P2P, mały biznes) |
| Zarządzanie ryzykiem | Blokada banku + żądanie ratunkowe | Odrzucenie transakcji / kolejkowanie | Blokada transakcji po wyczerpaniu pokrycia |

### Moduł ELIXIR: System Sesyjnego Nettingu

Moduł działa w trybie Deferred Net Settlement (DNS). Zamiast przetwarzać każdą płatność osobno, czeka na zamknięcie okna sesyjnego przez `SessionSchedulerService`.

- **Architektura plików i rola:** `SessionService` zbiera paczki XML (ISO 20022), a `NettingService` redukuje tysiące przelewów do kilku transferów wynikowych.
- **Zależność od SORBNET:** Jeśli `BankLiquidityService` wykaże, że bank przekroczył `debtLimit`, sesja ELIXIR zostaje zamrożona (`WAITING_FOR_LIQUIDITY`). System nie przeleje środków do banków-wierzycieli, dopóki dłużnik nie wyrówna pozycji za pomocą transferu ratunkowego z SORBNET-u.

### Moduł SORBNET: System Brutto w Czasie Rzeczywistym (RTGS)

Moduł symulujący system banku centralnego. W przeciwieństwie do ELIXIR-a, nie istnieje tutaj pojęcie "sesji" ani "nettingu". Każda transakcja oznaczona jako SORBNET musi zostać rozliczona natychmiast i indywidualnie.

- **`SorbnetProcessorService`:** Serwis procesujący płatności wysokokwotowe. Przyjmuje transakcję, pomija algorytmy nettingowe i uderza bezpośrednio do `BankLiquidityService` w celu natychmiastowego obciążenia konta SORBNET banku nadawcy i uznania konta odbiorcy.
- **Kolejkowanie (FIFO z priorytetami):** Jeśli bank nie ma w tym momencie środków na koncie w banku centralnym, transakcja SORBNET nie jest odrzucana natychmiast. Trafia do kolejki systemowej (`SorbnetPaymentQueue`). Czeka tam na zasilenie konta (np. z transakcji przychodzących od innych banków lub kredytu technicznego).
- **Integracja przez Kafkę (`SorbnetKafkaListener`):** Nasłuchuje na dedykowanym topicu wiadomości ratunkowych z ELIXIR-a. Gdy widzi żądanie `requiredTopUp`, automatycznie generuje i procesuje priorytetową transakcję zasilającą konto ELIXIR danego banku kosztem jego środków w banku centralnym.

### Moduł Express Elixir: System Płatności Natychmiastowych

Najbardziej restrykcyjny moduł pod kątem czasu odpowiedzi (cel: < 1 sekunda). Działa w trybie ciągłym (24/7), co wymusza zupełnie inną architekturę zarządzania płynnością – opartą na gwarancjach finansowych (Pre-funding).

- **`ExpressElixirService`:** Obsługuje transakcje natychmiastowe. Nie korzysta z bazy danych w sposób blokujący i unika transakcji rozproszonych, aby zachować najwyższą wydajność.
- **Konta Powiernicze (Collateral Accounts):** Banki przed uruchomieniem systemu muszą "zamrozić" określoną kwotę na dedykowanych kontach zabezpieczających w `BankLiquidityService`.
- **Autoryzacja w ułamku sekundy:** Gdy klient zleca przelew Express Elixir, system sprawdza stan konta przedpłaconego banku nadawcy:
  - Jeśli środki są dostępne: saldo jest natychmiast aktualizowane, a transakcja otrzymuje status `COMPLETED`.
  - Jeśli bank wyczerpie swój limit przedpłaty: transakcja zostaje natychmiast odrzucona ze statusem `INSUFFICIENT_COLLATERAL`. System nie czeka na dosyłanie środków (w przeciwieństwie do ELIXIR-a i SORBNET-u).

### Przepływ Danych i Współpraca w Całym Ekosystemie

Poniższy schemat obrazuje, jak zdarzenia w jednym systemie triggerują akcje w pozostałych modułach aplikacji:

```plaintext
[Klient / Systemy Zewnętrzne]
   │
   ├───> (Zlecenie Przelewu Ekspresowego) ──> [ExpressElixirService] ──> Autoryzacja 24/7 z konta Pre-fund (Natychmiastowy finał)
   │
   ├───> (Zlecenie Przelewu Zwykłego)     ──> [SessionService]        ──> Buforowanie XML ──> Netting (Co X minut)
   │                                                                                                │
   │                                                                                     (Brak płynności w ELIXIR)
   │                                                                                                │
   │                                                                                                v
   └───> (Zlecenie Przelewu >1 mln PLN)   ──> [SorbnetProcessorService] <─── [Kafka] <─── Wysłanie SOS do SORBNET
                                                       │
                                            Rozliczenie RTGS (Brutto)
                                                       │
                                                       v
                                            [BankLiquidityService] ──> Aktualizacja kont centralnych i odblokowanie ELIXIR
```

### Koordynacja przez BankLiquidityService

Wszystkie trzy systemy spotykają się w jednym punkcie – w bazie danych kont bankowych. `BankLiquidityService` zarządza trzema osobnymi saldami dla każdego banku:

- **`elixirBalance`** – do rozliczeń sesyjnych (z prawem do kontrolowanego debetu `debtLimit`).
- **`sorbnetBalance`** – realny, twardy pieniądz w banku centralnym (bez możliwości debetu).
- **`expressCollateral`** – wydzielona część pieniędzy zamrożona na potrzeby transakcji 24/7.
## 10. Schemat Bazy Danych i Dane Początkowe

### Moduł: ELIXIR

#### Tabela: `bank_accounts`

Przechowuje informacje o kontach banków uczestniczących w systemie Elixir.

| Kolumna | Typ (SQL) | Opis |
|---|---|---|
| `id` | BIGSERIAL (PK) | Unikalny identyfikator rekordu. |
| `service_code` | VARCHAR(32) | Kod serwisu, domyślnie `"ELIXIR"`. |
| `bank_id` | VARCHAR(50) | Identyfikator banku (np. `"BANK_A"`). |
| `bank_name` | VARCHAR(255) | Pełna nazwa banku. |
| `account_number` | VARCHAR(64) | Numer rachunku rozliczeniowego. |
| `balance` | NUMERIC(19,2) | Bieżące saldo banku w systemie. |
| `debt_limit` | NUMERIC(19,2) | Dopuszczalny limit zadłużenia. |
| `blocked` | BOOLEAN | Flaga informująca o blokadzie konta. |
| `overlimit_since` | TIMESTAMP | Czas przekroczenia limitu zadłużenia. |
| `blocked_at` | TIMESTAMP | Czas nałożenia blokady. |

#### Tabela: `settlement_bank_accounts`

Definiuje domyślne rachunki rozrachunkowe dla danego banku i serwisu.

| Kolumna | Typ (SQL) | Opis |
|---|---|---|
| `id` | BIGSERIAL (PK) | Unikalny identyfikator rekordu. |
| `service_code` | VARCHAR(32) | Kod serwisu. |
| `bank_id` | VARCHAR(50) | Identyfikator banku. |
| `account_number` | VARCHAR(64) | Numer rachunku powiązanego z bankiem. |
| `is_default` | BOOLEAN | Określa, czy jest to domyślne konto rozliczeniowe. |

#### Tabela: `payments`

Gromadzi zlecenia płatnicze wchodzące do sesji Elixir.

| Kolumna | Typ (SQL) | Opis |
|---|---|---|
| `payment_id` | VARCHAR(255) (PK) | Unikalny identyfikator płatności (UUID). |
| `service_code` | VARCHAR(32) | Kod serwisu (np. `"ELIXIR"`). |
| `sender_bank_id` | VARCHAR(255) | Identyfikator banku nadawcy. |
| `receiver_bank_id` | VARCHAR(255) | Identyfikator banku odbiorcy. |
| `sender_account` | VARCHAR(255) | Rachunek klienta nadawcy. |
| `receiver_account` | VARCHAR(255) | Rachunek klienta odbiorcy. |
| `sender_name` | VARCHAR(255) | Nazwa nadawcy. |
| `receiver_name` | VARCHAR(255) | Nazwa odbiorcy. |
| `amount` | NUMERIC(19,2) | Kwota przelewu. |
| `currency` | VARCHAR(255) | Waluta przelewu. |
| `title` | VARCHAR(255) | Tytuł płatności. |
| `status` | VARCHAR(255) | Status (`QUEUED`, `IN_SESSION`, `PROCESSED`, `REJECTED`, itp.). |
| `created_at` | TIMESTAMP | Data utworzenia zlecenia. |
| `type` | VARCHAR(32) | Typ zlecenia. |
| `session_id` | VARCHAR(255) | Identyfikator sesji przypisanej do płatności. |

---

### Moduł: EXPRESS ELIXIR

#### Tabela: `bank_accounts`

Przechowuje salda i limity banków dla systemu płatności natychmiastowych.

| Kolumna | Typ (SQL) | Opis |
|---|---|---|
| `bank_id` | VARCHAR(50) (PK) | Unikalny identyfikator banku (klucz główny). |
| `bank_name` | VARCHAR(255) | Pełna nazwa banku. |
| `balance` | NUMERIC(19,2) | Bieżące saldo banku. |
| `debt_limit` | NUMERIC(19,2) | Dodatni limit dopuszczalnego zadłużenia (zejścia na minus). |
| `blocked` | BOOLEAN | Flaga informująca o blokadzie operacji. |
| `overlimit_since` | TIMESTAMP | Moment pierwszego zatrzymania płatności z powodu braku płynności. |
| `blocked_at` | TIMESTAMP | Czas nałożenia ostatecznej blokady. |

#### Tabela: `payments`

Rejestruje przelewy natychmiastowe między klientami.

| Kolumna | Typ (SQL) | Opis |
|---|---|---|
| `payment_id` | VARCHAR(255) (PK) | Unikalny identyfikator płatności. |
| `sender_name` | VARCHAR(255) | Nazwa nadawcy. |
| `receiver_name` | VARCHAR(255) | Nazwa odbiorcy. |
| `sender_account` | VARCHAR(255) | Rachunek IBAN nadawcy. |
| `receiver_account` | VARCHAR(255) | Rachunek IBAN odbiorcy. |
| `amount` | NUMERIC(19,2) | Kwota płatności. |
| `currency` | VARCHAR(10) | Kod waluty. |
| `title` | VARCHAR(255) | Tytuł przelewu. |
| `sender_bank_id` | VARCHAR(50) | Identyfikator banku nadawcy. |
| `receiver_bank_id` | VARCHAR(50) | Identyfikator banku odbiorcy. |
| `status` | VARCHAR(50) | Status przelewu (`QUEUED`, `PROCESSED`, `GRIDLOCK_HELD`, itp.). |
| `created_at` | TIMESTAMP | Czas przyjęcia zlecenia. |
| `type` | VARCHAR(50) | Typ płatności, domyślnie `"EXPRESS"`. |
| `held_reason` | VARCHAR(255) | Powód wstrzymania (jeśli dotyczy). |
| `processed_at` | TIMESTAMP | Czas ostatecznego przetworzenia przelewu. |

---

### Moduł: SORBNET

#### Tabela: `bank_accounts`

Techniczne i główne rachunki banków w systemie SORBNET.

| Kolumna | Typ (SQL) | Opis |
|---|---|---|
| `id` | BIGSERIAL (PK) | Unikalny identyfikator. |
| `service_code` | VARCHAR(32) | Kod serwisu, domyślnie `"SORBNET"`. |
| `bank_id` | VARCHAR(50) | Identyfikator banku. |
| `bank_name` | VARCHAR(255) | Nazwa banku. |
| `account_number` | VARCHAR(64) | Numer rachunku w systemie SORBNET. |
| `balance` | NUMERIC(19,2) | Saldo rachunku. |
| `debt_limit` | NUMERIC(19,2) | Limit zadłużenia. |
| `blocked` | BOOLEAN | Flaga blokady konta. |
| `overlimit_since` | TIMESTAMP | Czas przekroczenia limitu zadłużenia. |
| `blocked_at` | TIMESTAMP | Czas blokady. |

#### Tabela: `bank_settlement_accounts`

Konta używane specjalnie do rozrachunków międzybankowych w ramach SORBNET.

| Kolumna | Typ (SQL) | Opis |
|---|---|---|
| `id` | BIGSERIAL (PK) | Identyfikator wewnętrzny. |
| `bank_id` | VARCHAR(50) | Identyfikator właściciela rachunku. |
| `account_number` | VARCHAR(64) | Numer rachunku. |
| `is_default` | BOOLEAN | Flaga wskazująca na rachunek domyślny dla banku. |

#### Tabela: `liquidity_requests`

Żądania o zasilenie płynności wystawiane przez Elixir lub Express Elixir w kierunku SORBNET.

| Kolumna | Typ (SQL) | Opis |
|---|---|---|
| `request_id` | VARCHAR(64) (PK) | Identyfikator requestu z serwisu źródłowego (`ReqId`). |
| `original_message_id` | VARCHAR(128) | ID komunikatu ISO 20022. |
| `bank_id` | VARCHAR(50) | Identyfikator banku wnioskującego o płynność. |
| `requesting_service_code` | VARCHAR(32) | Serwis proszący (`ELIXIR` / `ELIXIR_EXPRESS`). |
| `session_id` | VARCHAR(64) | Opcjonalne powiązanie z sesją Elixir. |
| `source_account` | VARCHAR(64) | Konto w SORBNET obciążane. |
| `target_account` | VARCHAR(64) | Techniczne konto docelowe. |
| `amount` | NUMERIC(19,2) | Kwota wnioskowanej płynności. |
| `currency` | VARCHAR(3) | Waluta (np. `"PLN"`). |
| `message` | VARCHAR(512) | Powód żądania płynności. |
| `source_has_funds` | BOOLEAN | Czy według lustra Elixira, konto posiada środki. |
| `status` | VARCHAR(32) | Status (`PENDING`, `EXECUTED`, `REJECTED`). |
| `received_at` | TIMESTAMP | Czas wpłynięcia requestu. |
| `processed_at` | TIMESTAMP | Czas podjęcia decyzji przez operatora. |
| `payment_id` | VARCHAR(64) | ID przelewu w systemie SORBNET pokrywającego request. |
| `origin_payment_id` | VARCHAR(255) | ID zablokowanego przelewu z Expressa (jeśli dotyczy). |

#### Tabela: `payments`

Wysokotowotowe i kluczowe przelewy rozrachunkowe realizowane w SORBNET (w tym zasilenia płynnościowe).

| Kolumna | Typ (SQL) | Opis |
|---|---|---|
| `payment_id` | VARCHAR(255) (PK) | Unikalny identyfikator przelewu. |
| `sender_bank_id` | VARCHAR(255) | Identyfikator banku nadawcy. |
| `receiver_bank_id` | VARCHAR(255) | Identyfikator banku odbiorcy. |
| `sender_account` | VARCHAR(255) | Numer rachunku nadawcy. |
| `receiver_account` | VARCHAR(255) | Numer rachunku odbiorcy. |
| `sender_name` | VARCHAR(255) | Nazwa nadawcy (opcjonalnie). |
| `receiver_name` | VARCHAR(255) | Nazwa odbiorcy (opcjonalnie). |
| `amount` | NUMERIC(19,2) | Kwota w walucie rozrachunku. |
| `currency` | VARCHAR(3) | Waluta przelewu (np. `"PLN"`). |
| `title` | VARCHAR(255) | Tytuł przelewu. |
| `source_service` | VARCHAR(32) | Serwis źródłowy (np. `"SORBNET"`). |
| `payment_type` | VARCHAR(32) | Typ operacji (np. `"LIQUIDITY_TRANSFER"`). |
| `session_id` | VARCHAR(255) | ID przypisanej sesji (opcjonalnie). |
| `liquidity_request_id` | VARCHAR(255) | Powiązany wniosek o płynność (opcjonalnie). |
| `status` | VARCHAR(32) | Status płatności (`PENDING`, `SETTLED`, `REJECTED`, `GRIDLOCK_HELD`). |
| `created_at` | TIMESTAMP | Data utworzenia. |
| `settled_at` | TIMESTAMP | Data rozliczenia w SORBNET. |
| `rejection_reason` | VARCHAR(512) | Powód odrzucenia (jeśli status to `REJECTED`). |

---

### Dane początkowe (Seedery) – Moduł SORBNET

#### Tabela: `bank_accounts`

Poniższa tabela prezentuje dokładny stan danych, jaki jest wstrzykiwany do systemu dla rachunków SORBNET przy starcie aplikacji:

| `service_code` | `bank_id` | `bank_name` | `account_number` | `balance` | `debt_limit` | `blocked` | `overlimit_since` | `blocked_at` |
|---|---|---|---|---|---|---|---|---|
| SORBNET | BANK_A | Bank A - Sorbnet | SORBNET-A-00000000000000000001 | 10 000 000.00 | 30 000 000.00 | false | NULL | NULL |
| SORBNET | BANK_B | Bank B - Sorbnet | SORBNET-B-00000000000000000002 | 10 000 000.00 | 30 000 000.00 | false | NULL | NULL |
| SORBNET | BANK_C | Bank C - Sorbnet | SORBNET-C-00000000000000000003 | 10 000 000.00 | 30 000 000.00 | false | NULL | NULL |

---

### Dane początkowe (Seedery) – Moduł ELIXIR

Skrypt dla modułu Elixir jest dość rozbudowany, ponieważ inicjalizuje nie tylko konta w samym systemie Elixir, ale również dba o spójność kont technicznych w systemie SORBNET (skąd banki mogą zasilać Elixir).

#### Tabela: `bank_accounts`

Dane ładowane przy starcie aplikacji. Co ciekawe, skrypt ten nadpisuje parametry kont SORBNET (ustawiając limit zadłużenia na `0.00`, w przeciwieństwie do poprzedniego skryptu SORBNET, który ustawiał `30 000 000.00`).

| `service_code` | `bank_id` | `bank_name` | `account_number` | `balance` | `debt_limit` | `blocked` |
|---|---|---|---|---|---|---|
| ELIXIR | BANK_A | Bank A - Elixir | ELIXIR-A-00000000000000000001 | 5 000 000.00 | 2 000 000.00 | false |
| ELIXIR | BANK_B | Bank B - Elixir | ELIXIR-B-00000000000000000002 | 5 000 000.00 | 2 000 000.00 | false |
| ELIXIR | BANK_C | Bank C - Elixir | ELIXIR-C-00000000000000000003 | 5 000 000.00 | 2 000 000.00 | false |
| SORBNET | BANK_A | Bank A - Sorbnet | SORBNET-A-00000000000000000001 | 10 000 000.00 | 0.00 | false |
| SORBNET | BANK_B | Bank B - Sorbnet | SORBNET-B-00000000000000000002 | 10 000 000.00 | 0.00 | false |
| SORBNET | BANK_C | Bank C - Sorbnet | SORBNET-C-00000000000000000003 | 10 000 000.00 | 0.00 | false |

Skrypt wykorzystuje `ON CONFLICT DO UPDATE` – przy każdym uruchomieniu salda i limity zostaną przywrócone do powyższych wartości domyślnych.

#### Tabela: `settlement_bank_accounts`

Skrypt powiązuje utworzone wcześniej rachunki jako domyślne rachunki rozliczeniowe (`is_default = true`) dla obu systemów.

| `service_code` | `bank_id` | `account_number` | `is_default` |
|---|---|---|---|
| ELIXIR | BANK_A | ELIXIR-A-00000000000000000001 | true 
## Technologie i Plany Rozwoju

### Wykorzystany stos technologiczny

- Java 17 / Spring Boot 3
- Apache Kafka (komunikacja sterowana zdarzeniami / event-driven)
- Docker / Docker Compose
- REST API (komunikacja synchroniczna/wgląd w stan aplikacji)
- XML / JAXB (obsługa komunikatów finansowych ISO 20022)

### Plany dalszego rozwoju projektu

- [do-wstawienia] Diagram UML komponentów systemu.
- [do-wstawienia] Diagram sekwencji wieloetapowego przepływu płatności.
- [do-wstawienia] Diagram BPMN procesu obsługi płatności i zakleszczeń (gridlock).
