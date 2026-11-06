package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.carpet.CarpetConfig;
import com.heishandashu.moreswitches.config.SwitchIds;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes Carpet's {@code commandPlayer} rule so fake players can be used in survival.
 *
 * <p>Two files matter. Carpet reads the world's own {@code carpet.conf} when it exists, and falls
 * back to {@code config/carpet/default_carpet.conf} otherwise, so both are updated: the global file
 * so it applies to worlds that have no file of their own, and the current save's file because that
 * one wins.</p>
 *
 * <p>Carpet reads these files when a world loads, so the change takes effect on the next load of
 * that world rather than instantly. That is a property of Carpet, not something this mod can
 * avoid without writing into Carpet's runtime state, which it deliberately does not do.</p>
 */
public final class CarpetFakePlayerModule implements ClientFeature {

    private boolean active;

    @Override
    public void register(String switchId) {
        this.active = enabled(switchId);
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' starts {}", MoreSwitchesMod.MOD_ID,
                SwitchIds.CARPET_FAKE_PLAYER, this.active ? "on" : "off");
        if (this.active) {
            apply(true);
        }
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (this.active == enabled) {
            return;
        }
        this.active = enabled;
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' -> {}",
                MoreSwitchesMod.MOD_ID, SwitchIds.CARPET_FAKE_PLAYER, enabled);
        apply(enabled);
    }

    /**
     * Applies the desired state to Carpet's configuration files.
     *
     * <p>Failure is logged with the path rather than thrown: a missing or read-only file must not
     * take the game down, and the player can still see why the switch had no effect.</p>
     */
    private static void apply(boolean allow) {
        String value = allow ? CarpetConfig.VALUE_ALLOW : CarpetConfig.VALUE_DEFAULT;
        Path global = FMLPaths.CONFIGDIR.get().resolve("carpet").resolve("default_carpet.conf");
        int changed = 0;
        if (writeRule(global, value)) {
            changed++;
        }
        for (Path world : currentWorldConfigs()) {
            if (writeRule(world, value)) {
                changed++;
            }
        }
        if (changed == 0) {
            MoreSwitchesMod.LOGGER.warn(
                    "[{}] carpet: could not update any config file; is Carpet installed? Looked at {}",
                    MoreSwitchesMod.MOD_ID, global);
        } else {
            MoreSwitchesMod.LOGGER.info(
                    "[{}] carpet: {} file(s) set to '{} {}'; takes effect next time a world loads",
                    MoreSwitchesMod.MOD_ID, changed, CarpetConfig.RULE, value);
        }
    }

    /**
     * Config files of the save that is currently open, when this is a single-player world.
     *
     * <p>A dedicated server keeps its own files outside the client, so nothing is returned there.</p>
     */
    private static List<Path> currentWorldConfigs() {
        List<Path> paths = new ArrayList<>(1);
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (server == null) {
            return paths;
        }
        try {
            paths.add(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("carpet.conf"));
        } catch (RuntimeException e) {
            // A server that is still starting has no world path yet; the global file still applies.
            MoreSwitchesMod.LOGGER.warn("[{}] carpet: could not resolve the save directory yet",
                    MoreSwitchesMod.MOD_ID, e);
        }
        return paths;
    }

    /** Rewrites one file; returns true when it was written. */
    private static boolean writeRule(Path file, String value) {
        try {
            List<String> lines = Files.exists(file)
                    ? Files.readAllLines(file, StandardCharsets.UTF_8)
                    : List.of();
            List<String> updated = CarpetConfig.withRule(lines, value);
            if (updated.equals(lines)) {
                return false;
            }
            Files.createDirectories(file.getParent());
            Files.write(file, updated, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            MoreSwitchesMod.LOGGER.warn("[{}] carpet: could not write {}", MoreSwitchesMod.MOD_ID, file, e);
            return false;
        }
    }
}
