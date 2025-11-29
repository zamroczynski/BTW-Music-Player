# BTW Music Player

## English description

BTW Music Player is a dynamic music engine created as an addon for the Better Than Wolves Community Edition 3.0.0 mod for Minecraft 1.6.4. It replaces the standard, repetitive in-game music, allowing for custom tracks to be played based on the gameplay context.

Players can create and install their own "Music Packs" containing music files and a songs.json file that defines when each track should be played. The mod features smooth transitions between songs and intelligently manages combat music, reacting to actions from both the player and monsters.

### Features

- Full Music Customization: Replace the entire game's soundtrack with your own `.ogg` files.
- Dynamic Playlists: Create playlists for various situations, complete with randomization and looping.
- Smooth Transitions: Music gently fades in and out when the context changes, eliminating abrupt cuts.
- Intelligent Combat System: Combat music is triggered not only when the player is attacked but also when they initiate a fight. The system sustains the combat theme as long as a threat is nearby.
- Context-Aware Playback: Define music playback rules based on a wide range of conditions:
  - Dimension: Overworld, Nether, The End.
  - Time of Day: Day or night.
  - Biome: Any biome in the game (e.g., Forest, Desert, Taiga).
  - Weather: Clear skies or a storm.
  - Location: Play different music on the surface versus in caves.
  - Combat: Dedicated music for encounters with monsters, wolves, or squids.
  - Boss Battles: Unique tracks for fights against the Ender Dragon and the Wither.
  - Victory: A special theme plays after defeating a boss.
- Multi-Music Pack Support: Load tracks from a single, specific music pack or from all installed packs at once.

### Downloads
- BTW Music Player Mod: [Download Latest Release](https://github.com/zamroczynski/BTW-Music-Player/releases)
- Sample Music Pack (`RadogostMusicPack.zip`): [Download Music Pack](https://github.com/zamroczynski/BTW-Music-Player/releases/tag/RadogostMusicPack)
- Source Code: [Browse on GitHub](https://github.com/zamroczynski/BTW-Music-Player)

### Requirements
- Better Than Wolves Community Edition 3.0.0 installed

### Mod Installation 
1. Download the latest `.jar` file for the mod from the Downloads section.
2. Open your Minecraft directory.
3. In your `.minecraft` directory, find the `mods` folder.
4. Move the downloaded `btw-music-player-x.y.z.jar` file into the `mods` folder.
5. Launch the game.

### Music Pack Installation
After successfully installing the mod, you can add any number of music packs to customize the in-game music.
1. Download a music pack (e.g., RadogostMusicPack.zip) from the Downloads section.
2. Navigate to your main Minecraft game directory (`.minecraft`).
3. In the `.minecraft` directory, find a folder named `musicpacks`. **If it does not exist, you must create it**.
4. Unzip the downloaded `.zip` archive. Inside, you will find a folder with the music pack's name (e.g., `RadogostMusicPack`).
5. **Important**: Move the **entire folder** (e.g., `RadogostMusicPack`) into the `musicpacks` directory. Do not copy individual `.ogg` files or the `songs.json` file directly.

After restarting the game, the music packs will be loaded automatically according to the settings in the configuration file.

#### Example Folder Structure
This is what your file structure should look like after installing the mod and two example music packs (`RadogostMusicPack` and `AnotherCoolMusicPack`):
```
.minecraft/
├── mods/
│   ├── btw-fabric-3.0.0-beta-snapshot-7.jar
│   └── btw-music-player-x.y.z.jar
│
├── musicpacks/
│   ├── RadogostMusicPack/
│   │   ├── songs.json
│   │   ├── day/
│   │   │   └── kam_01_spirit.ogg
│   │   └── combat/
│   │       └── witcher_52_monster_battle.ogg
│   │
│   └── AnotherCoolMusicPack/
│       ├── songs.json
│       └── ... (music files)
│
└── ... (other minecraft files and folders)``````
```
---

### Configuration
The configuration file is created automatically the first time you run the game with the mod installed. You can find it at the following location:
`.minecraft/config/btw-music-player.cfg`

You can open this file with any text editor (e.g., Notepad, VS Code).

#### Available Options
- `musicpack_loading_mode`
    - **Description:** Determines how the mod should load music packs.
    - **Values:**
        - `ALL` (default) - The mod loads music from all music packs found in the `musicpacks` folder.
        - `SINGLE` - The mod loads music from only one specific music pack.
    - **Example:** To use only one music pack, change this value to SINGLE.
- `single_musicpack_name`
    - **Description:** The name of the music pack to be activated when `musicpack_loading_mode` is set to `SINGLE`.
    - **Values:** The exact name of the music pack folder (e.g., `RadogostMusicPack`). This is case-sensitive.
    - **Example:** `single_musicpack_name=RadogostMusicPack`
- `enable_debug_logging`
    - **Description:** Enables or disables detailed mod messages in the game console. This is useful for diagnosing issues or when creating your own music pack.
    - **Values:**
        - `false` (default) - Logging is disabled.
        - `true` - Logging is enabled.
- `context_change_delay_seconds`
    - **Description:** Sets a delay (in seconds) that must pass before the music changes after the game context (e.g., biome, entering a cave) is altered. This prevents annoying, rapid music switching when moving between areas. High-priority music (combat, boss battles) will always play immediately, ignoring this delay.
    - **Values:**
      - `7` (default) - Music will change after the new context has been stable for 7 seconds.
      - `0` - Music changes are immediate.
#### Example Configuration
This is what the `btw-music-player.cfg` file would look like to load only a single music pack named `RadogostMusicPack`, with a 5-second music change delay and logging enabled:
```
musicpack_loading_mode=SINGLE
single_musicpack_name=RadogostMusicPack
enable_debug_logging=true
context_change_delay_seconds=5
```
### Creating Your Own Music Pack
Anyone can create their own music pack! The process involves preparing your music files in `.ogg` format and defining playback rules in a special file called `songs.json`.

#### 0. Important Tip: Volume Normalization
Before you begin, **we strongly recommend normalizing the volume** for all your tracks. This will prevent situations where one track is too quiet and the next is too loud, forcing you to constantly adjust the in-game music slider.

You can do this using a free program like **Audacity**. For my music pack, I used the "Normalize" effect to a perceived loudness of **-22 LUFS**. This step ensures a professional and consistent listening experience.

#### 1. Folder Structure
First, create a main folder for your music pack, e.g., `MyFirstMusicPack`. Inside it, you must place the `songs.json` file. You can place your music files alongside it or, for better organization, group them into subfolders (e.g., `music`, `combat`, `boss`).

```
musicpacks/
  └── MyFirstMusicPack/
      ├── songs.json
      ├── ambient/
      │   ├── day_forest.ogg
      │   └── night_plains.ogg
      └── combat/
          └── regular_fight.ogg
```

#### 2. The `songs.json` File
This is the heart of your music pack. It's a text file containing a list of "rules" that tell the mod when to play a specific track. Each rule consists of three parts:

- `file`: The path to the music file.
- `priority`: A number that determines how important the track is.
- `conditions`: A set of conditions that must be met for the track to be played.

Here is an example `songs.json` file with two rules:
```
[
  {
    "file": "ambient/day_forest.ogg",
    "priority": 10,
    "conditions": {
      "dimension": "overworld",
      "time_of_day": "day",
      "biome": "forest"
    }
  },
  {
    "file": "combat/regular_fight.ogg",
    "priority": 100,
    "conditions": {
      "is_in_combat": true
    }
  }
]
```

#### 3. Priority
Priority decides which playlist takes precedence if more than one rule's conditions are met at the same time. **The rule with the highest priority number wins.**

**Example:** You are in a forest during the day (matching the rule with `priority: 10`), but a zombie suddenly attacks you (also matching the combat rule with `priority: 100`). Since 100 is greater than 10, the music will immediately switch to the combat track.
**Tip:** Use low priorities (e.g., 1-20) for general background music (biomes, time of day) and high priorities (e.g., 100+) for special events like combat, boss battles, or victory themes.

#### 4. Available Conditions
The following table lists all the conditions you can use in the `conditions` section to define your rules.

| Condition Key     | Accepted Values                          | Description                                                                       |
| ------------------ |-------------------------------------------------| ----------------------------------------------------------------------------- |
| dimension          | "overworld", "the_nether", "the_end"            | Specifies the dimension the player must be in.                           |
| biome              | ocean, plains, desert, extreme_hills, forest, taiga, swampland, river, hell, sky, frozenocean, frozenriver, ice_plains, ice_mountains, mushroomisland, mushroomislandshore, beach, deserthills, foresthills, taigahills, extreme_hills_edge, jungle, junglehills | Specifies the biome. Use lowercase letters and replace spaces with `_`.      |
| time_of_day        | "day", "night"                                  | Specifies the current time of day in the game.                                       |
| weather            | "clear", "storm"                                | Checks if the weather is calm or if there is a storm.                          |
| is_in_cave         | true, false                                     | Checks if the player is underground with no view of the sky.                      |
| is_in_combat       | true, false                                     | Checks if the player is currently in combat.                                   |
| boss_type          | "wither", "ender_dragon"                        | Checks if a specific boss is nearby                          |
| victory_after_boss | true                                            | A special condition that is true for a short time after defeating a boss. |

#### 5. Tips and Tools

- File Format: Ensure all your music files are in the .ogg format.
- Use Logs: In the config file, `enable enable_debug_logging=true`. The game console will show what the mod is trying to do, which makes it easier to find errors.
- JSON Validator: If the game isn't loading your music pack, make sure your songs.json file is free of syntax errors. You can check it by pasting its content into an online JSON validator.
- Managing Large Music Packs (CSV to JSON):
If you plan to add dozens or hundreds of tracks, editing the `songs.json` file by hand becomes tedious. In this case, you can prepare a simple spreadsheet with columns in the following order:
`file,priority,dimension,biome,time_of_day,is_in_combat,weather,is_in_cave,boss_type,victory_after_boss`

Save the spreadsheet as a `.csv` file, and then use my helper tool, which will automatically convert it into a ready-to-use `songs.json` file.
- Download the tool: [BTW Music Player CSV to JSON](https://github.com/zamroczynski/BTW-Music-Player-CSV-to-JSON)
- Detailed instructions on how to use the tool are available in its repository.

## Polski opis

BTW Music Player to dynamiczny silnik muzyczny stworzony jako addon do modyfikacji Better Than Wolves Community Edition 3.0.0 dla Minecraft 1.6.4. Zastępuje on standardową, powtarzalną muzykę w grze, pozwalając na odtwarzanie własnych utworów w zależności od kontekstu rozgrywki.

Gracze mogą tworzyć i instalować własne "musicpacki" zawierające muzykę oraz plik `songs.json`, który definiuje, kiedy dany utwór ma być odtwarzany. Mod pozwala na płynne przejścia między piosenkami i inteligentnie zarządza muzyką walki, reagując na działania gracza i potworów.

### Funkcjonalności
- Pełna Personalizacja Muzyki: Zastąp całą ścieżkę dźwiękową gry własnymi plikami `.ogg`.
- Dynamiczne Playlisty: Twórz playlisty dla różnych sytuacji, z losową kolejnością odtwarzania i zapętlaniem.
- Płynne Przejścia: Muzyka łagodnie cichnie i pojawia się na nowo (Fade In/Fade Out) przy zmianie kontekstu, eliminując nagłe cięcia.
- Inteligentny System Walki: Muzyka walki jest aktywowana nie tylko, gdy gracz zostanie zaatakowany, ale również, gdy sam zainicjuje walkę. System podtrzymuje muzykę tak długo, jak długo w pobliżu znajduje się zagrożenie.
- Kontekstowe Odtwarzanie: Definiuj zasady odtwarzania muzyki w oparciu o szeroki wachlarz warunków:
  - Wymiar: Overworld, Nether, The End.
  - Pora dnia: Dzień lub noc.
  - Biom: Każdy biom w grze (np. Las, Pustynia, Tajga).
  - Pogoda: Czyste niebo lub burza.
  - Lokalizacja: Odtwarzaj inną muzykę na powierzchni i w jaskiniach.
  - Walka: Dedykowana muzyka podczas starć z potworami, wilkami czy kałamarnicami.
  - Walki z Bossami: Unikalne utwory podczas walki z Ender Dragonem i Witherem.
  - Zwycięstwo: Specjalna muzyka odtwarzana po pokonaniu bossa.
- Wsparcie dla Wielu Musicpacków: Wczytuj utwory z jednego, wybranego musicpacka lub ze wszystkich zainstalowanych jednocześnie.

### Pobieranie
- Modyfikacja BTW Music Player: [Pobierz najnowszą wersję](https://github.com/zamroczynski/BTW-Music-Player/releases)
- Przykładowy Musicpack (`RadogostMusicPack.zip`): [Pobierz musicpack](https://github.com/zamroczynski/BTW-Music-Player/releases/tag/RadogostMusicPack)
- Kod źródłowy: [Przeglądaj na GitHubie](https://github.com/zamroczynski/BTW-Music-Player)

### Wymagania
- Zainstalowana modyfikacja Better Than Wolves Community Edition 3.0.0

### Instalacja Modyfikacji
1. Pobierz najnowszy plik `.jar` modyfikacji z sekcji Pobieranie.
2. Otwórz folder gry Minecraft.
3. W folderze `.minecraft` znajdź katalog `mods`.
4. Przenieś pobrany plik `btw-music-player-x.y.z.jar` do folderu `mods`.
5. Uruchom grę.

### Instalacja Musicpacków
Po pomyślnym zainstalowaniu modyfikacji możesz dodać dowolną liczbę musicpacków, aby spersonalizować muzykę w grze

1. Pobierz wybrany musicpack (np. RadogostMusicPack.zip) z sekcji Pobieranie.
2. Przejdź do głównego folderu gry Minecraft (`.minecraft`).
3. W folderze `.minecraft` znajdź katalog o nazwie `musicpacks`. **Jeśli nie istnieje, musisz go utworzyć**.
4. Wypakuj pobrane archiwum `.zip`. W środku znajdziesz folder z właściwą nazwą musicpacka (np. `RadogostMusicPack`).
5. **Ważne**: Przenieś **cały folder** (np. `RadogostMusicPack`) do katalogu `musicpacks`. Nie kopiuj pojedynczych plików `.ogg` ani pliku `songs.json` luzem.

Po ponownym uruchomieniu gry, musicpacki zostaną automatycznie wczytane zgodnie z ustawieniami w pliku konfiguracyjnym.

### Przykładowa struktura folderów
Tak powinna wyglądać Twoja struktura plików po zainstalowaniu modyfikacji i dwóch przykładowych musicpacków (`RadogostMusicPack` oraz `InnySuperMusicpack`):
```
.minecraft/
  ├── mods/
  │   ├── btw-fabric-3.0.0-beta-snapshot-7.jar
  │   └── btw-music-player-x.y.z.jar
  │
  ├── musicpacks/
  │   ├── RadogostMusicPack/
  │   │   ├── songs.json
  │   │   ├── day/
  │   │   │   └── kam_01_spirit.ogg
  │   │   └── combat/
  │   │       └── witcher_52_monster_battle.ogg
  │   │
  │   └── InnySuperMusicpack/
  │       ├── songs.json
  │       └── ... (pliki muzyczne)
  │
  └── ... (inne pliki i foldery minecrafta)
```

### Konfiguracja
Plik konfiguracyjny jest tworzony automatycznie podczas pierwszego uruchomienia gry z zainstalowaną modyfikacją. Znajdziesz go w następującej lokalizacji:
`.minecraft/config/btw-music-player.cfg`

Możesz go otworzyć za pomocą dowolnego edytora tekstu (np. Notatnik, VS Code).

#### Dostępne Opcje
- `musicpack_loading_mode`
  - **Opis:** Określa, w jaki sposób modyfikacja ma ładować musicpacki.
  - **Wartości:**
    - `ALL` (domyślna) - Modyfikacja ładuje muzykę ze wszystkich musicpacków znalezionych w folderze musicpacks.
    - `SINGLE` - Modyfikacja ładuje muzykę tylko z jednego, wybranego musicpacka.
  - Przykład: Aby używać tylko jednego musicpacka, zmień tę wartość na SINGLE.
- `single_musicpack_name`
  - **Opis:** Nazwa musicpacka, który ma być aktywny, gdy `musicpack_loading_mode` jest ustawiony на `SINGLE`.
  - **Wartość:** Dokładna nazwa folderu z musicpackiem (np. `RadogostMusicPack`). Wielkość liter ma znaczenie.
  - **Przykład:** `single_musicpack_name=RadogostMusicPack`
- `enable_debug_logging`
  - **Opis:** Włącza lub wyłącza szczegółowe komunikaty modyfikacji w konsoli gry. Jest to przydatne do diagnozowania problemów lub podczas tworzenia własnego musicpacka.
  - **Wartość:**
    - `false` (domyślna) - Logi są wyłączone
    - `true` - Logi są włączone.
- `context_change_delay_seconds`
  - **Opis:** Ustawia opóźnienie (w sekundach), które musi upłynąć, zanim muzyka zmieni się po zmianie kontekstu w grze (np. zmiana biomu, wejście do jaskini). Zapobiega to irytującym, szybkim zmianom utworów podczas przemieszczania się między strefami. Muzyka o wysokim priorytecie (walka, walka z bossem) zawsze włączy się natychmiast, ignorując to opóźnienie.
  - **Wartość:**
    - `7` (domyślna) - Muzyka zmieni się, gdy nowy kontekst będzie stabilny przez 7 sekundy.
    - `0` - Zmiany muzyki są natychmiastowe.
#### Przykładowa konfiguracja
Tak wygląda zawartość pliku `btw-music-player.cfg`, który ładuje tylko jeden musicpack o nazwie `RadogostMusicPack`, z 5-sekundowym opóźnieniem zmiany muzyki i włączonym logowaniem:
```
musicpack_loading_mode=SINGLE
single_musicpack_name=RadogostmusicPack
enable_debug_logging=true
context_change_delay_seconds=5
```
### Tworzenie Własnego Musicpacka
Każdy może stworzyć własny musicpack! Proces polega na przygotowaniu plików muzycznych w formacie `.ogg` i zdefiniowaniu zasad ich odtwarzania w specjalnym pliku `songs.json`.

#### 0. Ważna Wskazówka: Normalizacja Głośności
Zanim zaczniesz, **zdecydowanie zalecamy wykonanie normalizacji głośności** dla wszystkich utworów. Zapobiegnie to sytuacji, w której jeden utwór jest za cichy, a następny za głośny, co zmuszałoby do ciągłego regulowania suwaka muzyki w grze.

Możesz to zrobić w darmowym programie **Audacity**. W moim musicpacku użyłem opcji "Normalizuj głośność" do postrzeganej głośności **-22 LUFS**. Ten krok gwarantuje profesjonalne i spójne wrażenia odsłuchowe.

#### 1. Struktura folderów
Na początek stwórz główny folder dla swojego musicpacka, np. `MojPierwszyMusicpack`. Wewnątrz niego musi znaleźć się plik `songs.json`. Pliki muzyczne możesz umieścić obok niego lub dla porządku pogrupować je w podfolderach (np. `music`, `combat`, `boss`).
```
musicpacks/
  └── MojPierwszyMusicpack/
      ├── songs.json
      ├── ambient/
      │   ├── day_forest.ogg
      │   └── night_plains.ogg
      └── combat/
          └── regular_fight.ogg
```
#### 2. Plik `songs.json`
To serce Twojego musicpacka. Jest to plik tekstowy zawierający listę "reguł" (rules), które mówią modyfikacji, kiedy odtwarzać dany utwór. Każda reguła składa się z trzech części:
- `file`: Ścieżka do pliku muzycznego.
- `priority`: Liczba określająca, jak ważny jest dany utwór.
- `conditions`: Zestaw warunków, które muszą zostać spełnione, aby utwór mógł być odtworzony.

Oto przykład pliku `songs.json` z dwiema regułami:
```
[
  {
    "file": "ambient/day_forest.ogg",
    "priority": 10,
    "conditions": {
      "dimension": "overworld",
      "time_of_day": "day",
      "biome": "forest"
    }
  },
  {
    "file": "combat/regular_fight.ogg",
    "priority": 100,
    "conditions": {
      "is_in_combat": true
    }
  }
]
```
#### 3. Priorytety (Priority)
Priorytet decyduje, która playlista ma pierwszeństwo, jeśli w danym momencie pasuje więcej niż jedna reguła. **Wygrywa reguła z wyższym priorytetem.**
- **Przykład:** Jesteś w lesie w ciągu dnia (pasuje reguła z `priority: 10`), ale nagle atakuje Cię zombie (pasuje też reguła walki z `priority: 100`). Ponieważ 100 jest większe od 10, muzyka natychmiast zmieni się na utwór walki.
- **Wskazówka:** Używaj niskich priorytetów (np. 1-20) dla ogólnej muzyki tła (biomy, pora dnia), a wysokich (np. 100+) dla specjalnych wydarzeń, takich jak walka, starcia z bossami czy motyw zwycięstwa.

#### 4. Dostępne Warunki (Conditions)
| Klucz warunku      | Akceptowane wartości                            | Opis                                                                          |
| ------------------ |-------------------------------------------------|-------------------------------------------------------------------------------|
| dimension          | "overworld", "the_nether", "the_end"            | Określa wymiar, w którym musi znajdować się gracz.                            |
| biome              | ocean, plains, desert, extreme_hills, forest, taiga, swampland, river, hell, sky, frozenocean, frozenriver, ice_plains, ice_mountains, mushroomisland, mushroomislandshore, beach, deserthills, foresthills, taigahills, extreme_hills_edge, jungle, junglehills | Określa biom. Nazwę biomu pisz małymi literami, a spacje zamieniaj na `_`.      |
| time_of_day        | "day", "night"                                  | Określa aktualną porę dnia w grze.                                            |
| weather            | "clear", "storm"                                | Sprawdza, czy pogoda jest spokojna, czy jest burza.                           |
| is_in_cave         | true, false                                     | Sprawdza, czy gracz jest pod ziemią i nie widzi nieba.                        |
| is_in_combat       | true, false                                     | Sprawdza, czy gracz jest aktualnie w walce.                                   |
| boss_type          | "wither", "ender_dragon"                        | Sprawdza, czy w pobliżu znajduje się określony boss.                          |
| victory_after_boss | true                                            | Specjalny warunek, który jest spełniony przez krótki czas po pokonaniu bossa. |

#### 5. Wskazówki i Narzędzia
- **Format plików:** Upewnij się, że wszystkie Twoje pliki muzyczne są w formacie `.ogg`.
- **Użyj logów:** W pliku konfiguracyjnym włącz `enable_debug_logging=true`. W konsoli gry zobaczysz, co modyfikacja próbuje zrobić, co ułatwi znajdowanie błędów.

- **Walidator JSON:** Jeśli gra nie wczytuje Twojego musicpacka, upewnij się, że plik `songs.json` nie ma błędów składni. Możesz go sprawdzić wklejając jego treść do internetowego walidatora JSON.
- **Zarządzanie Dużymi Musicpackami (CSV to JSON):**  
  Jeśli planujesz dodać dziesiątki lub setki utworów, ręczne edytowanie pliku `songs.json`staje się niewygodne. W takim przypadku możesz przygotować prosty arkusz kalkulacyjny z kolumnami w następującej kolejności:  
  `file,priority,dimension,biome,time_of_day,is_in_combat,weather,is_in_cave,boss_type,victory_after_boss`
  Zapisz arkusz jako plik `.csv`, a następnie użyj mojego pomocniczego narzędzia, które automatycznie zamieni go na gotowy do użycia plik `songs.json`.
    - **Pobierz narzędzie:** [BTW Music Player CSV to JSON](https://github.com/zamroczynski/BTW-Music-Player-CSV-to-JSON)
    - Szczegółowe instrukcje, jak używać narzędzia, znajdziesz w jego repozytorium.
