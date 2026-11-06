package com.heishandashu.tracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heishandashu.tracker.TrackerMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain JSON settings file: {@code config/tracker.json}.
 *
 * <p>Every feature switch is stored as {@code "<module id>": true/false}, so adding a new
 * feature module needs no changes here -- the module declares its own default and this
 * class fills in anything the file does not have yet.</p>
 *
 * <p>The file is a normal, human-editable JSON document on purpose: server owners and
 * modpack authors can flip switches without opening the game.</p>
 */
public final class TrackerConfig {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String FILE_NAME = "tracker.json";

    private static TrackerConfig instance;

    /** module id -> enabled. Kept insertion-ordered so the file reads in module order. */
    private Map<String, Boolean> features = new LinkedHashMap<>();

    /**
     * Small text values a module wants to remember between sessions.
     *
     * <p>The switch table is booleans only, but 自定义坐标追踪 has to persist the last coordinate
     * the player typed. Keeping it in the same file means one place to look and one place to edit
     * by hand.</p>
     */
    private Map<String, String> values = new LinkedHashMap<>();

    /** Bumped only when the file layout itself changes and a migration is needed. */
    private int configVersion = 1;

    private TrackerConfig() {
    }

    public static TrackerConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static TrackerConfig load() {
        TrackerConfig config = new TrackerConfig();
        Path path = file();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("configVersion")) {
                    config.configVersion = root.get("configVersion").getAsInt();
                }
                if (root.has("features") && root.get("features").isJsonObject()) {
                    JsonObject features = root.getAsJsonObject("features");
                    for (String key : features.keySet()) {
                        if (features.get(key).isJsonPrimitive()) {
                            config.features.put(key, features.get(key).getAsBoolean());
                        }
                    }
                }
                if (root.has("values") && root.get("values").isJsonObject()) {
                    JsonObject values = root.getAsJsonObject("values");
                    for (String key : values.keySet()) {
                        if (values.get(key).isJsonPrimitive()) {
                            config.values.put(key, values.get(key).getAsString());
                        }
                    }
                }
            } catch (Exception e) {
                TrackerMod.LOGGER.warn("[tracker] could not read {} -- using defaults", path, e);
            }
        }
        instance = config;
        return config;
    }

    public void save() {
        Path path = file();
        JsonObject root = new JsonObject();
        root.addProperty("configVersion", configVersion);
        JsonObject features = new JsonObject();
        for (Map.Entry<String, Boolean> entry : this.features.entrySet()) {
            features.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("features", features);
        // Only written when something actually uses it, so an untouched config stays as short as
        // it always was.
        if (!this.values.isEmpty()) {
            JsonObject values = new JsonObject();
            for (Map.Entry<String, String> entry : this.values.entrySet()) {
                values.addProperty(entry.getKey(), entry.getValue());
            }
            root.add("values", values);
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            TrackerMod.LOGGER.error("[tracker] could not write {}", path, e);
        }
    }

    /** @return the stored value, or {@code fallback} when the feature has never been written. */
    public boolean get(String id, boolean fallback) {
        Boolean value = this.features.get(id);
        return value != null ? value : fallback;
    }

    /** Stores a value and writes the file when it actually changed. */
    public boolean set(String id, boolean value) {
        Boolean previous = this.features.put(id, value);
        if (previous != null && previous == value) {
            return false;
        }
        save();
        return true;
    }

    /** Used by the settings screen; makes sure every known feature has a stored value. */
    public void ensureKnown(String id, boolean fallback) {
        if (!this.features.containsKey(id)) {
            this.features.put(id, fallback);
            save();
        }
    }

    /** @return the stored text, or {@code fallback} when it has never been written. */
    public String getValue(String key, String fallback) {
        String value = this.values.get(key);
        return value != null ? value : fallback;
    }

    /** Stores text and writes the file when it actually changed. */
    public boolean setValue(String key, String value) {
        String previous = this.values.put(key, value);
        if (previous != null && previous.equals(value)) {
            return false;
        }
        save();
        return true;
    }

    /** Removes a stored text value (used by "clear the saved coordinate"). */
    public boolean removeValue(String key) {
        if (this.values.remove(key) == null) {
            return false;
        }
        save();
        return true;
    }
}
