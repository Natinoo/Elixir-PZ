## Elixir / Express Elixir / SORBNET

Projekt ma na celu odtworzenie architektury polskiego systemu rozliczeniowego w modelu 1:1:

- **Elixir** – system sesyjnego rozliczania przelewów detalicznych
- **Express Elixir** – system płatności natychmiastowych
- **SORBNET** – system RTGS do rozrachunku wysokokwotowego i final settlement

Projekt realizowany jest w architekturze **niemonomolitycznej (microservices/distributed services)**.

---

Aby odpalic projekt, w konsoli należy wpisać ./start.bat

---
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