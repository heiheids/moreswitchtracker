package com.heishandashu.moreswitches.i18n;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.ModSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The mod's own bilingual text table.
 *
 * <p>26.3 changed {@code ResourceLocation} to {@link Identifier}; the table loader follows that
 * rename. Loading is lazy: the first screen access asks the client's current resource manager,
 * which is already populated by the time a screen can open. This also avoids a Fabric resource
 * reload listener, whose registration order differs from NeoForge.</p>
 */
public final class ModLang {

    public static final String ZH_CN = "zh_cn";
    public static final String EN_US = "en_us";

    private static final List<String> SUPPORTED = List.of(ZH_CN, EN_US);
    private static final Map<String, Map<String, String>> TABLES = new HashMap<>();

    private static String gameLanguage = EN_US;
    private static boolean loaded;

    private ModLang() {
    }

    /** Loads once, on first use. A screen cannot open before the resource manager exists. */
    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager manager = minecraft.getResourceManager();
        if (manager == null) {
            return;
        }
        reload(manager, minecraft.getLanguageManager().getSelected());
    }

    public static void reload(ResourceManager manager, String gameLanguageCode) {
        gameLanguage = normalise(gameLanguageCode);
        TABLES.clear();
        for (String code : SUPPORTED) {
            TABLES.put(code, readTable(manager, code));
        }
        loaded = true;
        MoreSwitchesMod.LOGGER.info("[{}] language tables loaded (game language: {}, {} entries)",
                MoreSwitchesMod.MOD_ID, gameLanguage, TABLES.get(EN_US).size());
    }

    public static String activeLanguage() {
        ensureLoaded();
        String choice = ModSettings.get().language();
        if (ModSettings.LANGUAGE_AUTO.equals(choice)) {
            return ZH_CN.equals(gameLanguage) ? ZH_CN : EN_US;
        }
        return choice;
    }

    public static String gameLanguage() {
        ensureLoaded();
        return gameLanguage;
    }

    public static String text(String key) {
        return text(activeLanguage(), key);
    }

    public static String text(String language, String key) {
        ensureLoaded();
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

    public static String switchName(String switchId, boolean fromGameLanguage) {
        return text(fromGameLanguage ? gameLanguage() : activeLanguage(), switchKey(switchId, "name"));
    }

    public static String switchDescription(String switchId, boolean fromGameLanguage) {
        return text(fromGameLanguage ? gameLanguage() : activeLanguage(), switchKey(switchId, "desc"));
    }

    private static String switchKey(String switchId, String field) {
        return "moreswitchtracker.switch." + switchId + "." + field;
    }

    private static String normalise(String code) {
        if (code == null) {
            return EN_US;
        }
        return code.trim().toLowerCase().startsWith("zh") ? ZH_CN : EN_US;
    }

    private static Map<String, String> readTable(ResourceManager manager, String code) {
        Identifier resourceId = MoreSwitchesMod.id("lang/" + code + ".json");
        Map<String, String> table = new HashMap<>();
        try {
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
            MoreSwitchesMod.LOGGER.error("[{}] could not read language file {}",
                    MoreSwitchesMod.MOD_ID, resourceId, e);
        }
        return table;
    }
}
