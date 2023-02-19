# Projekt zaliczeniowy

Dokumentacja.pdf - Dokumentacja projektu.

Głowne części aplikacji Azure_Factory1:

- plik Main.java - głowna część aplikacji, wytłumaczona szczegółowo w dokumentacji, dodatkowe komentarze w kodzie.

- plik ConfigProperties.java - konfiguracja połączenia między serwerem, a Azure IoT Hub przy użyciu dwóch ciągów połączeń do uwierzytelniania i autoryzacji. Linia server.url ustawia adres URL serwera, a dwie linie connectionString zawierają niezbędne poświadczenia do połączenia z IoT Hubem.

- plik CloudDevice.java - kod definiujący klasę CloudDevice, która reprezentuje urządzenie Azure IoT i zapewnia konstruktor, który pobiera identyfikator urządzenia i klucz główny oraz generuje ciąg połączenia dla urządzenia. Klasa zapewnia również metody getter i setter dla ciągu połączenia, ID urządzenia i klucza głównego.

- plik Machine.java - kod definiujący klasę Java o nazwie "Machine", która reprezentuje fizyczną maszynę i zawiera informacje o jej statusie produkcyjnym, identyfikatorze zlecenia roboczego, tempie produkcji, liczbie dobrych i złych jednostek, temperaturze, błędzie urządzenia oraz obiekcie CloudDevice, który przechowuje ciąg połączenia urządzenia. Zawiera również obiekt DeviceClient do nawiązania połączenia z Azure IoT Hub.

- plik MachineDto - kod definiujący klasę MachineDTO, która reprezentuje obiekt transferu danych dla maszyny w linii produkcyjnej. Zawiera różne właściwości związane z maszyną, takie jak jej identyfikator urządzenia, identyfikator zlecenia roboczego, tempo produkcji, błąd urządzenia, temperaturę oraz liczbę wyprodukowanych dobrych i złych jednostek. Zawiera również właściwość timestamp wskazującą, kiedy dane zostały zebrane.


- plik factory_analytics - export z Azure Stream Analytics job
