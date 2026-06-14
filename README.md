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

## 8. Technologie i Plany Rozwoju

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
