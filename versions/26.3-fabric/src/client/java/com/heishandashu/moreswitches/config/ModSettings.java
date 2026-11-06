package com.heishandashu.moreswitches.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heishandashu.moreswitches.MoreSwitchesMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * More Switches settings: {@code config/moreswitches.json}.
 *
 * <p>The file name is unchanged from the 1.21.1 build, so switch states and the manual language
 * choice survive the 26.3 upgrade. Tracker keeps its own {@code config/tracker.json}.</p>
 *
 * <p>{@code guiScale} belongs to the 自由界面 page. It is written only when a free scale is
 * chosen, so a file from an older version simply has no such key and vanilla sizing stays in
 * charge.</p>
 */
public final class ModSettings {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String FILE_NAME = "moreswitches.json";

    public static final String LANGUAGE_AUTO = "auto";

    private static ModSettings instance;

    private final Map<String, Boolean> switches = new LinkedHashMap<>();
    private String language = LANGUAGE_AUTO;

    /** Free GUI scale from the 自由界面 page; 0 means "use vanilla's own scale". */
    private float guiScale;

    private ModSettings() {
    }

    public static ModSettings get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static ModSettings load() {
        ModSettings settings = new ModSettings();
        for (SwitchDefinition definition : SwitchIds.all()) {
            settings.switches.put(definition.id(), definition.defaultEnabled());
        }

        Path path = file();
        if (!Files.exists(path)) {
            MoreSwitchesMod.LOGGER.info("[{}] no settings file yet at {} -- using defaults",
                    MoreSwitchesMod.MOD_ID, path);
            instance = settings;
            return settings;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("switches") && root.get("switches").isJsonObject()) {
                JsonObject stored = root.getAsJsonObject("switches");
                for (String key : stored.keySet()) {
                    if (stored.get(key).isJsonPrimitive()) {
                        settings.switches.put(key, stored.get(key).getAsBoolean());
                    }
                }
            }
            if (root.has("language") && root.get("language").isJsonPrimitive()) {
                settings.language = normaliseLanguage(root.get("language").getAsString());
            }
            if (root.has("guiScale") && root.get("guiScale").isJsonPrimitive()) {
                settings.guiScale = normaliseGuiScale(root.get("guiScale").getAsFloat());
            }
        } catch (Exception e) {
            MoreSwitchesMod.LOGGER.warn("[{}] could not read {} -- defaults kept",
                    MoreSwitchesMod.MOD_ID, path, e);
        }
        instance = settings;
        return settings;
    }

    private static String normaliseLanguage(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (value.equals(LANGUAGE_AUTO)) {
            return LANGUAGE_AUTO;
        }
        if (value.equals("zh_cn") || value.startsWith("zh")) {
            return "zh_cn";
        }
        if (value.equals("en_us") || value.startsWith("en")) {
            return "en_us";
        }
        MoreSwitchesMod.LOGGER.warn("[{}] unknown language '{}' in the settings file -- using auto",
                MoreSwitchesMod.MOD_ID, raw);
        return LANGUAGE_AUTO;
    }

    /**
     * Keeps a stored free scale inside the range the client understands.
     *
     * <p>The bounds are repeated here rather than imported from the client's
     * {@code FreeGuiScale}: this class is also loaded on a dedicated server, where no client class
     * may be touched. A value outside the range can only come from a hand-edited file, and is
     * treated as "feature off" rather than as a reason to refuse to start.</p>
     */
    private static float normaliseGuiScale(float raw) {
        if (!Float.isFinite(raw) || raw <= 0.0F) {
            return 0.0F;
        }
        return Math.max(0.5F, Math.min(raw, 8.0F));
    }

    public boolean isEnabled(String switchId) {
        return this.switches.getOrDefault(switchId, false);
    }

    public void setEnabled(String switchId, boolean enabled) {
        boolean previous = isEnabled(switchId);
        this.switches.put(switchId, enabled);
        if (previous != enabled) {
            save();
        }
    }

    public String language() {
        return this.language;
    }

    public void setLanguage(String value) {
        String normalised = normaliseLanguage(value);
        if (!normalised.equals(this.language)) {
            this.language = normalised;
            save();
        }
    }

    /** Free GUI scale; 0 means "feature off, vanilla's own scale applies". */
    public float guiScale() {
        return this.guiScale;
    }

    public void setGuiScale(float value) {
        float normalised = normaliseGuiScale(value);
        if (normalised != this.guiScale) {
            this.guiScale = normalised;
            save();
        }
    }

    public void save() {
        Path path = file();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        JsonObject root = new JsonObject();
        root.addProperty("language", this.language);
        if (this.guiScale > 0.0F) {
            root.addProperty("guiScale", this.guiScale);
        }
        JsonObject stored = new JsonObject();
        for (Map.Entry<String, Boolean> entry : this.switches.entrySet()) {
            stored.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("switches", stored);

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            MoreSwitchesMod.LOGGER.error("[{}] could not write {}", MoreSwitchesMod.MOD_ID, path, e);
        }
    }
}
