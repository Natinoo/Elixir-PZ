# Elixir / Express Elixir / SORBNET

Projekt ma na celu odtworzenie architektury polskiego systemu rozliczeniowego w modelu 1:1.

## Opis systemów

- **Elixir** – system sesyjnego rozliczania przelewów detalicznych
- **Express Elixir** – system płatności natychmiastowych
- **SORBNET** – system RTGS przeznaczony do rozrachunku wysokokwotowego oraz final settlement

Projekt został zrealizowany w architekturze **niemonomolitycznej** (*microservices / distributed services*), w której poszczególne moduły działają jako niezależne usługi. 

## Architektura projektu

System składa się z trzech głównych modułów:
- `elixir`
- `express-elixir`
- `sorbnet`

Komunikacja pomiędzy wybranymi modułami została oparta na **Apache Kafka** uruchamianej w środowisku **Docker**.  W aktualnej wersji projektu potwierdzono poprawne działanie komunikacji pomiędzy modułami **Elixir** i **SORBNET**, gdzie Elixir wysyła wiadomości do topicu `payments`, a SORBNET je odbiera. [file:507]

## Uruchomienie projektu

Aby uruchomić projekt, należy w konsoli wpisać:

```bash
./start.bat
```

Aby zatrzymać wszystkie usługi, należy uruchomić:

```bash
./stop.bat
```

## Struktura projektu

```text
payment-system/
│
├── elixir/
├── express-elixir/
├── sorbnet/
│
├── docker-compose.yml
├── start.bat
├── stop.bat
└── README.md
```

## Technologie

W projekcie wykorzystano:
- **Java / Spring Boot**
- **Apache Kafka**
- **Docker / Docker Compose**

## Cel projektu

Celem projektu jest odwzorowanie sposobu komunikacji i rozliczeń pomiędzy systemami płatniczymi, z uwzględnieniem podziału na niezależne moduły oraz komunikacji asynchronicznej pomiędzy usługami. 
