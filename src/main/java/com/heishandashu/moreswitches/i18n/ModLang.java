package com.heishandashu.moreswitches.i18n;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.ModSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The mod's own text table for its own screens.
 *
 * <p>Vanilla's {@code Component.translatable} follows the *game* language and gives no way to
 * show Chinese while the game runs in English. The settings screen therefore reads the same
 * {@code assets/<modid>/lang/<code>.json} files through this class instead, so "自动 / 中文 /
 * English" can be a real switch. The JSON files are still the standard format, so the vanilla
 * key-binding names in the same files keep working.</p>
 *
 * <p>Only two languages ship, and both are always loaded into memory (a few KiB). A missing key
 * falls back to English, then to the key itself, so a half-translated file shows readable text
 * instead of blank rows.</p>
 */
public final class ModLang {

    public static final String ZH_CN = "zh_cn";
    public static final String EN_US = "en_us";

    private static final List<String> SUPPORTED = List.of(ZH_CN, EN_US);

    /** The language the game itself is running in, refreshed on every resource reload. */
    private static String gameLanguage = EN_US;

    private static final Map<String, Map<String, String>> TABLES = new HashMap<>();

    private ModLang() {
    }

    /** Called from the resource reload hook; also the point where the tables are (re)read. */
    public static void reload(ResourceManager manager, String gameLanguageCode) {
        gameLanguage = normalise(gameLanguageCode);
        TABLES.clear();
        for (String code : SUPPORTED) {
            TABLES.put(code, readTable(manager, code));
        }
        MoreSwitchesMod.LOGGER.info("[{}] language tables loaded (game language: {}, {} entries)",
                MoreSwitchesMod.MOD_ID, gameLanguage, TABLES.get(EN_US).size());
    }

    /** The language the mod should display right now, honouring the manual override. */
    public static String activeLanguage() {
        String choice = ModSettings.get().language();
        if (ModSettings.LANGUAGE_AUTO.equals(choice)) {
            // Anything that is not Chinese follows English; that matches how the mod ships.
            return ZH_CN.equals(gameLanguage) ? ZH_CN : EN_US;
        }
        return choice;
    }

    public static String gameLanguage() {
        return gameLanguage;
    }

    /** Text for a key in the active language. Never null; unknown keys return the key itself. */
    public static String text(String key) {
        return text(activeLanguage(), key);
    }

    public static String text(String language, String key) {
        Map<String, String> table = TABLES.get(language);
        if (table != null) {
            String value = table.get(key);
            if (value != null) {
                return value;
            }
        }
        Map<String, String> english = TABLES.get(EN_US);
        if (english != null) {
            String value = english.get(key);
            if (value != null) {
                return value;
            }
        }
        return key;
    }

    /**
     * The name of a switch, in the active language.
     *
     * <p>Each language file holds only its own language, so switching the interface language
     * changes every word the player sees: there is never a Chinese line left in an English
     * screen, which is what the previous "both names on one row" layout produced.</p>
     */
    public static String switchName(String switchId, boolean fromGameLanguage) {
        String language = fromGameLanguage ? gameLanguage() : activeLanguage();
        return text(language, switchKey(switchId, "name"));
    }

    /** The description of a switch, in the active language. */
    public static String switchDescription(String switchId, boolean fromGameLanguage) {
        String language = fromGameLanguage ? gameLanguage() : activeLanguage();
        return text(language, switchKey(switchId, "desc"));
    }

    private static String switchKey(String switchId, String field) {
        return "moreswitchtracker.switch." + switchId + "." + field;
    }

    private static String normalise(String code) {
        if (code == null) {
            return EN_US;
        }
        String value = code.trim().toLowerCase();
        return value.startsWith("zh") ? ZH_CN : EN_US;
    }

    private static Map<String, String> readTable(ResourceManager manager, String code) {
        ResourceLocation resourceId = MoreSwitchesMod.id("lang/" + code + ".json");
        Map<String, String> table = new HashMap<>();
        try {
            // getResourceStack keeps every pack's copy, ours last; the last readable copy wins so
            // a resource pack can override a single line without copying the whole file.
            List<Resource> stack = manager.getResourceStack(resourceId);
            for (Resource resource : stack) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    for (String key : root.keySet()) {
                        if (root.get(key).isJsonPrimitive()) {
                            table.put(key, root.get(key).getAsString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            MoreSwitchesMod.LOGGER.error("[{}] could not read language file {}", MoreSwitchesMod.MOD_ID, resourceId, e);
        }
        return table;
    }
}
