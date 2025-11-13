// NOWA KLASA
package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MusicManager {

    private static final List<SongRule> allSongRules = new ArrayList<>();
    private static boolean isLoaded = false;

    /**
     * Główna metoda ładująca, wywoływana raz przy starcie addona.
     */
    public static void load() {
        if (isLoaded) return; // Zabezpieczenie przed podwójnym ładowaniem

        ModConfig.getInstance(); // Upewnij się, że config jest załadowany
        File gameDir = FabricLoader.getInstance().getGameDir().toFile();

        MusicLogger.always("--- Rozpoczynam ładowanie Sound Packów... ---");
        MusicLogger.always("Używam katalogu gry: " + gameDir.getAbsolutePath());

        File soundPacksDir = new File(gameDir, "soundpacks");

        ModConfig config = ModConfig.getInstance();
        boolean loadAll = config.loadingMode.equalsIgnoreCase("ALL");

        if (!soundPacksDir.exists() || !soundPacksDir.isDirectory()) {
            MusicLogger.always("Folder '" + soundPacksDir.getAbsolutePath() + "' nie istnieje. Ładowanie przerwane.");
            return;
        }

        Gson gson = new Gson();
        Type songRuleListType = new TypeToken<ArrayList<SongRule>>(){}.getType();

        File[] soundPacks = soundPacksDir.listFiles();
        if (soundPacks == null) {
            MusicLogger.error("Nie można odczytać zawartości folderu soundpacks.");
            return;
        }

        for (File soundPack : soundPacks) {
            if (soundPack.isDirectory()) {
                if (!loadAll && !soundPack.getName().equalsIgnoreCase(config.singlePackName)) {
                    MusicLogger.log("Pomijam pack: " + soundPack.getName() + " (Single mode: " + config.singlePackName + ")");
                    continue;
                }
                File songsJsonFile = new File(soundPack, "songs.json");
                if (songsJsonFile.exists()) {
                    MusicLogger.always("Znaleziono sound pack: " + soundPack.getName());
                    try (FileReader reader = new FileReader(songsJsonFile)) {
                        List<SongRule> rules = gson.fromJson(reader, songRuleListType);
                        for (SongRule rule : rules) {
                            rule.soundPackPath = soundPack.getAbsolutePath();
                        }
                        allSongRules.addAll(rules);
                        MusicLogger.always("Załadowano " + rules.size() + " reguł.");
                    } catch (Exception e) {
                        MusicLogger.error("Błąd podczas ładowania sound packa: " + soundPack.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
        MusicLogger.always("--- Zakończono ładowanie. Całkowita liczba reguł: " + allSongRules.size() + " ---");
        isLoaded = true;
    }

    /**
     * Zwraca listę wszystkich załadowanych reguł piosenek.
     * @return Lista reguł.
     */
    public static List<SongRule> getSongRules() {
        return allSongRules;
    }
}