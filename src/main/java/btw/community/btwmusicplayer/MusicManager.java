package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MusicManager {
    private static final Gson GSON = new Gson();
    private static final List<SongRule> allSongRules = new ArrayList<>();
    private static boolean isLoaded = false;

    /**
     * Main loading method, called once at addon startup.
     */
    public static void load() {
        if (isLoaded) return; // Prevent double loading

        ModConfig.getInstance(); // Ensure config is loaded
        Path gameDir = FabricLoader.getInstance().getGameDir();

        MusicLogger.always("--- Starting to load Sound Packs... ---");
        MusicLogger.always("Using game directory: " + gameDir.toAbsolutePath());

        Path soundPacksDir = gameDir.resolve("soundpacks");

        if (!Files.isDirectory(soundPacksDir)) {
            MusicLogger.always("Folder '" + soundPacksDir.toAbsolutePath() + "' does not exist. Loading aborted.");
            return;
        }

        ModConfig config = ModConfig.getInstance();
        boolean loadAll = config.loadingMode.equalsIgnoreCase("ALL");
        Type songRuleListType = new TypeToken<ArrayList<SongRule>>(){}.getType();

        try (Stream<Path> soundPackPaths = Files.list(soundPacksDir)) {
            soundPackPaths
                    .filter(Files::isDirectory)
                    .forEach(soundPackPath -> {
                        String soundPackName = soundPackPath.getFileName().toString();
                        if (!loadAll && !soundPackName.equalsIgnoreCase(config.singlePackName)) {
                            MusicLogger.log("Skipping pack: " + soundPackName + " (Single mode active for: " + config.singlePackName + ")");
                            return;
                        }

                        Path songsJsonFile = soundPackPath.resolve("songs.json");
                        if (Files.exists(songsJsonFile)) {
                            MusicLogger.always("Found sound pack: " + soundPackName);
                            try (Reader reader = Files.newBufferedReader(songsJsonFile)) {
                                List<SongRule> rules = GSON.fromJson(reader, songRuleListType);
                                for (SongRule rule : rules) {
                                    rule.soundPackPath = soundPackPath.toAbsolutePath().toString();
                                }
                                allSongRules.addAll(rules);
                                MusicLogger.always("Loaded " + rules.size() + " rules.");
                            } catch (Exception e) {
                                MusicLogger.error("Error loading sound pack: " + soundPackName);
                                e.printStackTrace();
                            }
                        }
                    });
        } catch (IOException e) {
            MusicLogger.error("Could not read contents of the soundpacks folder.");
            e.printStackTrace();
        }

        MusicLogger.always("--- Loading finished. Total rules loaded: " + allSongRules.size() + " ---");
        isLoaded = true;
    }

    /**
     * Returns the list of all loaded song rules.
     * @return A list of rules.
     */
    public static List<SongRule> getSongRules() {
        return allSongRules;
    }
}