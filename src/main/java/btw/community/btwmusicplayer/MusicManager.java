package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.MusicPackStatus;
import btw.community.btwmusicplayer.data.SongRule;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class MusicManager {
    public static final String ROOT_DIR_NAME = "musicpacks";
    public static final String CONFIG_FILENAME = "songs.json";
    private static final Gson GSON = new Gson();
    private static final List<SongRule> allSongRules = new ArrayList<>();
    private static boolean isLoaded = false;

    /**
     * Clears current rules and reloads everything from disk based on current Config.
     */
    public static void reload() {
        MusicLogger.always("--- Reloading Music System ---");
        allSongRules.clear();
        isLoaded = false;

        ModConfig.getInstance().loadConfig();

        load();

        MusicLogger.always("--- Reload Complete. Rules active: " + allSongRules.size() + " ---");
    }

    /**
     * Main loading method.
     */
    public static void load() {
        if (isLoaded) {
            MusicLogger.log("MusicManager is already loaded. Skipping.");
            return;
        }

        ModConfig config = ModConfig.getInstance();
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path musicPacksDir = gameDir.resolve(ROOT_DIR_NAME);

        MusicLogger.always("--- Starting to load Music Packs... ---");

        if (!ensureDirectoryExists(musicPacksDir)) {
            return;
        }

        boolean loadAll = config.loadingMode.equalsIgnoreCase("ALL");
        String targetPack = config.singleMusicPackName;

        MusicLogger.log("Loading Mode: " + config.loadingMode + (loadAll ? "" : " (Target: " + targetPack + ")"));

        Type songRuleListType = new TypeToken<ArrayList<SongRule>>(){}.getType();

        try (Stream<Path> paths = Files.list(musicPacksDir)) {
            paths.filter(Files::isDirectory).forEach(musicPackPath -> {
                String packName = musicPackPath.getFileName().toString();

                if (!loadAll && !packName.equals(targetPack)) {
                    return;
                }

                Path songsJsonFile = musicPackPath.resolve(CONFIG_FILENAME);

                if (Files.exists(songsJsonFile)) {
                    MusicLogger.log("Reading rules from: " + packName);
                    try (Reader reader = Files.newBufferedReader(songsJsonFile)) {
                        List<SongRule> rules = GSON.fromJson(reader, songRuleListType);

                        if (rules != null && !rules.isEmpty()) {
                            for (SongRule rule : rules) {
                                rule.musicPackPath = musicPackPath.toAbsolutePath().toString();
                            }
                            allSongRules.addAll(rules);
                            MusicLogger.log(" -> Added " + rules.size() + " rules from " + packName);
                        } else {
                            MusicLogger.error(" -> " + CONFIG_FILENAME + " is empty or invalid in " + packName);
                        }

                    } catch (Exception e) {
                        MusicLogger.error(" -> CRITICAL ERROR parsing " + CONFIG_FILENAME + " in " + packName + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    MusicLogger.error(" -> No " + CONFIG_FILENAME + " found in " + packName);
                }
            });
        } catch (IOException e) {
            MusicLogger.error("Could not list files in " + ROOT_DIR_NAME);
            e.printStackTrace();
        }

        MusicLogger.always("--- Loading finished. Total rules loaded: " + allSongRules.size() + " ---");
        isLoaded = true;
    }

    /**
     * Scans the /musicpacks directory and validates each pack WITHOUT loading it into the game.
     * Checks JSON syntax AND file existence.
     * Used by the Configuration GUI to display status list.
     * @return List of MusicPackStatus objects.
     */
    public static List<MusicPackStatus> scanAvailablePacks() {
        List<MusicPackStatus> statuses = new ArrayList<>();
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path musicPacksDir = gameDir.resolve(ROOT_DIR_NAME);

        if (!ensureDirectoryExists(musicPacksDir)) {
            return statuses;
        }

        MusicLogger.log("[Scanner] Scanning for available music packs...");
        Type songRuleListType = new TypeToken<ArrayList<SongRule>>(){}.getType();

        try (Stream<Path> paths = Files.list(musicPacksDir)) {
            paths.filter(Files::isDirectory).forEach(musicPackPath -> {
                String packName = musicPackPath.getFileName().toString();
                Path songsJsonFile = musicPackPath.resolve(CONFIG_FILENAME);

                boolean isValid = false;
                String message = "OK";

                if (!Files.exists(songsJsonFile)) {
                    message = "Missing " + CONFIG_FILENAME;
                } else {
                    try (Reader reader = Files.newBufferedReader(songsJsonFile)) {
                        List<SongRule> rules = GSON.fromJson(reader, songRuleListType);
                        if (rules == null || rules.isEmpty()) {
                            message = "JSON is empty";
                        } else {
                            // Deep Validation
                            int missingFiles = 0;
                            int invalidFormats = 0;

                            for (SongRule rule : rules) {
                                if (rule.file == null || rule.file.trim().isEmpty()) {
                                    MusicLogger.error("[Scanner] Pack '" + packName + "': Found rule with empty file path.");
                                    missingFiles++;
                                    continue;
                                }

                                // Check 1: Format
                                if (!rule.file.toLowerCase().endsWith(".ogg")) {
                                    MusicLogger.error("[Scanner] Pack '" + packName + "': Invalid format (not .ogg): " + rule.file);
                                    invalidFormats++;
                                }

                                // Check 2: Existence
                                Path songPath = musicPackPath.resolve(rule.file);
                                if (!Files.exists(songPath)) {
                                    MusicLogger.error("[Scanner] Pack '" + packName + "': Missing file: " + rule.file);
                                    missingFiles++;
                                }
                            }

                            if (missingFiles > 0 || invalidFormats > 0) {
                                isValid = false;
                                if (missingFiles > 0 && invalidFormats > 0) {
                                    message = "Errors: Missing & Invalid files";
                                } else if (missingFiles > 0) {
                                    message = "Error: Missing " + missingFiles + " files";
                                } else {
                                    message = "Error: " + invalidFormats + " invalid formats";
                                }
                                MusicLogger.log("[Scanner] " + packName + " failed validation. Missing: " + missingFiles + ", Bad Format: " + invalidFormats);
                            } else {
                                isValid = true;
                                message = rules.size() + " songs verified";
                            }
                        }
                    } catch (JsonSyntaxException e) {
                        message = "JSON Syntax Error";
                        MusicLogger.error("[Scanner] Syntax error in " + packName + ": " + e.getMessage());
                    } catch (Exception e) {
                        message = "Error: " + e.getClass().getSimpleName();
                        MusicLogger.error("[Scanner] Unknown error in " + packName + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                statuses.add(new MusicPackStatus(packName, musicPackPath.toAbsolutePath().toString(), isValid, message));
                MusicLogger.log("[Scanner] Checked: " + packName + " [" + (isValid ? "VALID" : "INVALID") + "] - " + message);
            });
        } catch (IOException e) {
            MusicLogger.error("[Scanner] Disk IO Error: " + e.getMessage());
        }

        Collections.sort(statuses, (a, b) -> a.folderName.compareToIgnoreCase(b.folderName));

        return statuses;
    }

    private static boolean ensureDirectoryExists(Path dir) {
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
                MusicLogger.always("Created missing folder: " + dir);
                return true;
            } catch (IOException e) {
                MusicLogger.error("Failed to create folder: " + dir);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the list of all loaded song rules.
     * @return A list of rules.
     */
    public static List<SongRule> getSongRules() {
        return allSongRules;
    }

    public interface MusicSoundManager {
        MusicNotificationRenderer getNotificationRenderer();
    }
}