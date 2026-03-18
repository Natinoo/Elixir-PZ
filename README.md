# Elixir
Elixir + Elixir Express + Sorbnet
Pozyskiwanie wiedzy domenowej 

 

System Elixir + Elixir Express + system SORBNET3 

ELIXIR 

System obsługujący detaliczne rozliczenia międzybankowe w Polsce. Banki mogą ustalać godziny przyjmowania zleceń płatniczych oraz manipulować terminami księgowania przychodzących płatności lub korzystać z gotowych terminów przygotowanych przez krajową izbę rozliczeń (KIR) która utrzymuje Elixir od 1994 roku. 
Elixir działa tylko w dni robocze i jest podzielony na 3 sesje w ciągu doby, izba rozliczeniowa podsumowuje przychodzące i wychodzące z każdego z banków płatności i ustala tzw. pozycję netto każdego z uczestników. Jest to kwota, którą bank powinien przekazać (jeśli suma wychodzących płatności jest wyższa niż przychodzących) lub otrzymać (w odwrotnej sytuacji) od innych banków. 

Bank nadawcy (obciążenie) ->KIR (pośredniczenie/rozliczanie w sesji) -> bank odbiorcy 
 

Zakres i warunki użycia 
ELIXIR obsłuży operację, jeśli jest w PLN i oba banki (nadawcy i odbiorcy) uczestniczą w systemie.   Poza zwykłymi przelewami system bywa wykorzystywany także do zwrotów płatności, poleceń zapłaty oraz realizacji opłat podatkowych i składek ZUS. 
Jeśli bank ma "za dużo" zleceń w danej sesji, nie zablokuje przelewu, lecz zadba o jego realizację w ramach sesji rozliczeniowych KIR (o 9:30, 13:30, 16:00), zapewniając płynność na rachunku powierniczym w NBP.  
Co się dzieje, gdy bank ma dużo zleceń/nadmiar środków? 
Realizacja w sesji: Przelewy są przetwarzane grupowo. Jeśli bank ma bardzo dużo zleceń, sesja może trwać dłużej (maksymalnie 1,5 godziny), ale finalnie środki trafiają do odbiorcy zgodnie z harmonogramem.
Bezpieczeństwo płynności: Banki uczestniczące w ELIXIR muszą utrzymywać odpowiednie środki na rachunkach w NBP, aby rozliczać przelewy, co gwarantuje, że "za dużo" transakcji nie sparaliżuje systemu. 
Ograniczenia systemowe: W przypadku nadzwyczajnego obciążenia, techniczne limity mogą opóźnić przetworzenie, ale przelewy nie są odrzucane z powodu ich "nadmiaru".  
Sytuacje, gdy bank "nie przyjmuje" więcej (limity): 
 
Jeśli "za dużo" oznacza zbyt wysoką kwotę pojedynczego przelewu, banki stosują limity (najczęściej 100 000 zł dla Express Elixir, chociaż banki mogą ustalać własne niższe limity, np. 5 000 - 20 000 zł dla niektórych transakcji). W przypadku przelewów powyżej 15 000 euro (ok. 60-65 tys. PLN), bank ma obowiązek zgłosić transakcję do GIIF (Generalnego Inspektora Informacji Finansowej), ale nie oznacza to zablokowania przelewu. 


SORBNET3 

System płatności wysokokwotowych (uruchomiony 08.09.2025 przez NBP) typu RTGS (Real Time Gross Settlement) - dla płatności w złotych, w którym zlecenia realizowane są zgodnie z zasadą rozrachunku brutto, tj. przetwarzane pojedynczo w czasie rzeczywistym. 

SORBNET3 ma budowę modułową, wykorzystuje komunikaty płatnicze MX w standardzie XML przesyłanych poprzez sieć SWIFT i MPLS NBP 

Rodzaje operacji  

W systemie SORBNET3 przeprowadzane są następujące rodzaje operacji:  

rozrachunek zleceń międzybankowych i klientowskich,  

operacje z bankiem centralnym, w tym: zasilenia i odsilenia gotówkowe, pobieranie opłat, odsetek i prowizji, rozliczanie instrumentów polityki pieniężnej, operacje depozytowo-kredytowe,  

operacje wynikające z obowiązku utrzymywania rezerwy obowiązkowej, ▪ przelewy płynności,  

rozrachunek zobowiązań i należności wynikających z rozliczeń w systemach zewnętrznych,  

rozliczenia pieniężne między bankami lub ich klientami bądź klientami banku, dla którego bank jest bankiem-korespondentem, a posiadaczami rachunków w NBP (poza systemem SORBNET3),  

operacje własne NBP.  

 

Uczestnictwo  

Podmiotami uprawnionymi do uczestnictwa w systemie SORBNET3 są:  
-NBP,  
-banki inne niż NBP, 
-podmioty zarządzające systemami zewnętrznymi,  
-banki centralne innego państwa,  
-Krajowa Spółdzielcza Kasa Oszczędnościowo-Kredytowa,  
-Bankowy Fundusz Gwarancyjny,  
-Skarb Państwa reprezentowany przez ministra właściwego do spraw finansów. 

Możliwy jest jeden typ uczestnictwa w systemie SORBNET3, tj. uczestnictwo bezpośrednie. Banki nieposiadające rachunku bieżącego w systemie SORBNET3 mogą rozliczać się przez inny bank, który posiada rachunek bieżący pełniąc dla tego banku rolę banku-korespondenta w systemie SORBNET3. Dostęp do systemu SORBNET3 mają również banki, dla których NBP prowadzi rachunek rezerwy obowiązkowej. 

Architektura systemu  
modułu centralnego systemu SORBNET3 (MCS3), za pośrednictwem którego obsługiwane są rachunki uczestników systemu oraz inne rachunki (w tym rachunki rezerwy obowiązkowej dla banków nieposiadających rachunku bieżącego oraz rachunki własne NBP),  
modułu monitorującego systemu SORBNET3 (MMS3), umożliwiającego monitorowanie rachunku własnego uczestnika systemu SORBNET3, zarządzanie zleceniami płatniczymi i wysyłanie w trybie awaryjnym międzybankowych zleceń płatniczych, dokonywanie przelewów płynności w trybie awaryjnym oraz wysyłanie zleceń płatniczych zmniejszających saldo rachunku rezerwy obowiązkowej przez banki posiadające taki rachunek, które zawarły z NBP stosowną umowę,  

-modułu opłat (MOP), w którym wyliczane są opłaty i prowizje,  
-modułu odsetek (MOD), w którym wyliczane są odsetki (z wyjątkiem odsetek dotyczących rezerwy obowiązkowej), 
-modułu rezerwy obowiązkowej (MRO), który służy do obsługi utrzymywanej przez banki rezerwy obowiązkowej i odsetek z tym związanych,  
-modułu raportowego (MRA), który jest wykorzystywany przez NBP do generowania raportów,  
-modułu hurtowni danych (PAR-SORBNET3), który pozwala na przechowywanie i przetwarzanie danych historycznych,  
-modułu księgowego (MKS), który realizuje zadania księgi pomocniczej pełnionej przez system SORBNET3,  
-modułu archiwalnego (MAR), służącego do przechowywania danych starszych niż określony parametr,  
-modułu awaryjnego (MAS3), który pozwala na realizację najpilniejszych płatności oraz zaspokojenie zapotrzebowania banków na pieniądz gotówkowy w przypadku dłuższej przerwy w funkcjonowaniu systemu SORBNET3 

System SORBNET3 działa we wszystkie dni operacyjne, tj. od poniedziałku do piątku, w godzinach 7:30 – 18:00, poza dniami ustawowo wolnymi od pracy. 
 
Elixir Express  

„Elixir Express” traktujemy jako hipotetyczny system natychmiastowych rozliczeń międzybankowych (RTGS – Real-Time Gross Settlement), będący rozszerzeniem klasycznego systemu Krajowa Izba Rozliczeniowa, analogicznie do relacji między ELIXIR a Express Elixir. 

<img width="576" height="247" alt="image" src="https://github.com/user-attachments/assets/bbad9d07-8c6b-4508-8bf8-52346be331b6" />

Architektura systemu 

Uczestnicy: 
-Bank nadawcy 
-Bank odbiorcy 
-Operator systemu (np. KIR) 
-Bank centralny (rachunki rozliczeniowe banków) 

Model rozrachunku: 
-Każdy bank posiada rachunek techniczny (prefunding) w banku centralnym. 
-W momencie inicjacji przelewu: 
  * Sprawdzana jest dostępność środków klienta. 
  * Sprawdzany jest limit płynności banku w systemie. 
  * Następuje natychmiastowy rozrachunek brutto między bankami. 
  * Bank odbiorcy uznaje rachunek klienta. 

 <img width="630" height="200" alt="image" src="https://github.com/user-attachments/assets/b6c48f7d-b83a-4bf4-874b-33cab864fd88" />


Rozliczenia i płynność 

Mechanizm: 

Każdy bank utrzymuje określony limit środków w systemie (prefunding). 

Transakcja jest realizowana wyłącznie, jeśli: 

klient ma środki, 

bank ma środki rozrachunkowe w systemie. 

Jeżeli limit zostanie wyczerpany: 

przelew jest odrzucany, 

klient otrzymuje komunikat o braku możliwości realizacji. 

Sytuacje brzegowe - błędów 

W systemie natychmiastowym typu RTGS transakcja jest realizowana tylko wtedy, gdy zarówno klient, jak i bank posiadają wystarczające środki – w przeciwnym razie przelew zostaje odrzucony przed rozrachunkiem. Po wykonaniu rozrachunku operacja jest nieodwołalna. 

Jeżeli bank stanie się niewypłacalny przed rozrachunkiem, przelew nie zostanie zrealizowany; jeżeli po rozrachunku, środki pozostają w banku odbiorcy i podlegają standardowym procedurom upadłościowym. 

Wysłanie przelewu na nieistniejący rachunek skutkuje jego odrzuceniem przez bank odbiorcy. System może również narzucać maksymalne limity kwotowe, a w przypadku awarii technicznej transakcje są odrzucane lub kolejkowane. 
