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
    public static final String ROOT_DIR_NAME = "musicpacks";
    public static final String CONFIG_FILENAME = "songs.json";
    private static final Gson GSON = new Gson();
    private static final List<SongRule> allSongRules = new ArrayList<>();
    private static boolean isLoaded = false;

    /**
     * Main loading method, called once at addon startup.
     */
    public static void load() {
        if (isLoaded) return;

        ModConfig config = ModConfig.getInstance();
        Path gameDir = FabricLoader.getInstance().getGameDir();

        MusicLogger.always("--- Starting to load Music Packs... ---");
        MusicLogger.trace("Using game directory: " + gameDir.toAbsolutePath());

        Path musicPacksDir = gameDir.resolve(ROOT_DIR_NAME);

        if (!Files.isDirectory(musicPacksDir)) {
            MusicLogger.always("Folder '" + musicPacksDir.toAbsolutePath() + "' does not exist. Creating it...");
            try {
                Files.createDirectories(musicPacksDir);
                MusicLogger.always("Created empty musicpacks folder. Please add music packs.");
            } catch (IOException e) {
                MusicLogger.error("Failed to create musicpacks folder!");
                e.printStackTrace();
            }
            return;
        }

        boolean loadAll = config.loadingMode.equalsIgnoreCase("ALL");
        String targetPack = config.singleMusicPackName;

        MusicLogger.trace("Loading Mode: " + config.loadingMode + (loadAll ? "" : " (Target: " + targetPack + ")"));

        Type songRuleListType = new TypeToken<ArrayList<SongRule>>(){}.getType();

        try (Stream<Path> paths = Files.list(musicPacksDir)) {
            paths.filter(Files::isDirectory).forEach(musicPackPath -> {
                String packName = musicPackPath.getFileName().toString();
                MusicLogger.trace("Found directory: " + packName);

                if (!loadAll && !packName.equalsIgnoreCase(targetPack)) {
                    MusicLogger.trace(" -> Skipping (Mode is SINGLE and this is not '" + targetPack + "')");
                    return;
                }

                Path songsJsonFile = musicPackPath.resolve(CONFIG_FILENAME);

                if (Files.exists(songsJsonFile)) {
                    MusicLogger.always("Processing Music Pack: " + packName);
                    try (Reader reader = Files.newBufferedReader(songsJsonFile)) {
                        List<SongRule> rules = GSON.fromJson(reader, songRuleListType);

                        if (rules != null && !rules.isEmpty()) {
                            for (SongRule rule : rules) {
                                rule.musicPackPath = musicPackPath.toAbsolutePath().toString();
                            }
                            allSongRules.addAll(rules);
                            MusicLogger.log(" -> Loaded " + rules.size() + " rules from " + packName);
                        } else {
                            MusicLogger.error(" -> " + CONFIG_FILENAME + " is empty or invalid in " + packName);
                        }

                    } catch (Exception e) {
                        MusicLogger.error(" -> Error parsing " + CONFIG_FILENAME + " in " + packName);
                        e.printStackTrace();
                    }
                } else {
                    MusicLogger.error(" -> No " + CONFIG_FILENAME + " found in " + packName + ". Skipping.");
                }
            });
        } catch (IOException e) {
            MusicLogger.error("Could not access " + ROOT_DIR_NAME + " directory.");
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