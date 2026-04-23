# Elixir / Express Elixir / SORBNET

Projekt ma na celu odtworzenie architektury polskiego systemu rozliczeniowego w uproszczonym modelu 1:1, z podziałem na trzy główne systemy płatnicze: Elixir, Express Elixir oraz SORBNET. Każdy z nich jest zaimplementowany jako oddzielny mikroserwis.

## Opis systemów

- **Elixir** – system sesyjnego rozliczania przelewów detalicznych. Płatności są przyjmowane, kolejkowane w ramach sesji i dopiero później rozliczane. W kolejnych etapach projektu planowane jest dodanie nettingu.
- **Express Elixir** – system płatności natychmiastowych. Płatności są przekazywane i przetwarzane bez oczekiwania na sesję.
- **SORBNET** – system RTGS przeznaczony do rozrachunku wysokokwotowego oraz final settlement pojedynczych zleceń w czasie rzeczywistym.

## Architektura projektu

Projekt został zrealizowany w architekturze **mikroserwisowej** (*microservices / distributed services*), w której poszczególne moduły działają jako niezależne usługi komunikujące się przez Apache Kafka.

Główne moduły:

- `elixir` – przyjmuje zlecenia płatnicze z banków, nadaje im `paymentId` i publikuje komunikaty na odpowiednie topiki Kafki. Dla zleceń rozliczanych sesyjnie utrzymuje listę płatności w bieżącej sesji.
- `elixir-express` – konsument komunikatów przeznaczonych do ścieżki Express Elixir; odbiera płatności z topiku `payments.elixir-express`.
- `sorbnet` – konsument komunikatów przeznaczonych do rozrachunku w SORBNET; odbiera płatności z topiku `payments.sorbnet`.

### Komunikacja asynchroniczna (Kafka)

Komunikacja pomiędzy modułami jest realizowana asynchronicznie poprzez Apache Kafka, uruchomioną w kontenerze Docker.

W projekcie wykorzystywane są następujące topiki:

- `payments.elixir`
- `payments.elixir-express`
- `payments.sorbnet`

Każdy serwis konsumencki posiada własną grupę konsumencką, dzięki czemu wiadomości są przetwarzane niezależnie przez odpowiednie systemy.

> [do-wstawienia] diagram architektury (UML komponentów / Mermaid / draw.io)

## Zachowanie serwisów i statusy płatności

### Elixir

Serwis `elixir` udostępnia REST API do przyjmowania zleceń płatniczych:

- `POST /api/elixir/payments` – przyjmuje dane płatności, nadaje `paymentId` (jeśli nie został podany) i publikuje komunikat na topicu Kafki.

Po przyjęciu zlecenia Elixir zwraca odpowiedź zawierającą co najmniej:

- `paymentId` – identyfikator płatności,
- `status` – np. `QUEUED_FOR_SESSION` dla płatności przyjętych do sesyjnego przetwarzania.

Aktualnie komunikaty są serializowane do **JSON**. W projekcie rozpoczęto migrację do **XML (JAXB)**, ale na obecnym etapie wymaga ona jeszcze uzupełnienia zależności i dopracowania konfiguracji w modułach.

### Express Elixir

Serwis `elixir-express` odbiera komunikaty z topiku `payments.elixir-express` i loguje otrzymane płatności. W projekcie pełni rolę uproszczonej reprezentacji systemu płatności natychmiastowych.

### SORBNET

Serwis `sorbnet` odbiera komunikaty z topiku `payments.sorbnet` i loguje otrzymane płatności. W projekcie reprezentuje system RTGS, czyli rozrachunek pojedynczych zleceń w czasie rzeczywistym.

> [do-wstawienia] diagram procesu BPMN (bank → ELIXIR → Express Elixir / SORBNET)

## Struktura projektu

```text
payment-system/
│
├── elixir/
│   └── ...
├── elixir-express/
│   └── ...
├── sorbnet/
│   └── ...
│
├── docker-compose.yml
├── start.bat
├── stop.bat
└── README.md
```

Każdy moduł to osobna aplikacja Spring Boot z własnym `application.properties` oraz konfiguracją Kafki.

## Uruchomienie projektu

Projekt jest przygotowany do uruchomienia przy pomocy Docker Compose.

1. Upewnij się, że masz zainstalowane:
   - Docker
   - Docker Compose

2. Zbuduj obrazy lub paczki aplikacji:

   ```bash
   mvn clean package -DskipTests
   ```

3. Uruchom cały system:

   ```bash
   ./start.bat
   ```

4. Zatrzymaj wszystkie usługi:

   ```bash
   ./stop.bat
   ```

## Technologie

W projekcie wykorzystano:

- **Java 17 / Spring Boot 3**
- **Apache Kafka**
- **Docker / Docker Compose**
- **REST API**
- **JSON** jako aktualny format komunikatów
- **XML / JAXB** jako planowany kierunek dalszej rozbudowy

## Cel projektu

Celem projektu jest:

- odwzorowanie sposobu komunikacji i rozliczeń pomiędzy systemami ELIXIR, Express Elixir oraz SORBNET,
- pokazanie podziału systemu na niezależne mikroserwisy,
- zaprezentowanie komunikacji asynchronicznej pomiędzy usługami z użyciem Apache Kafka,
- przygotowanie architektury pod dalszą rozbudowę o netting, pełniejsze statusy biznesowe oraz komunikację XML.

## Dokumentacja techniczna

Wiedza domenowa i techniczna została opisana bezpośrednio w pliku `README.md` w postaci notatek, opisu architektury oraz miejsc przeznaczonych na diagramy.

Planowane uzupełnienia dokumentacji:

- [do-wstawienia] diagram UML komponentów
- [do-wstawienia] diagram sekwencji przepływu płatności
- [do-wstawienia] diagram BPMN procesu obsługi płatności
- [do-wstawienia] dokumentacja API / Swagger / OpenAPI