package com.heishandashu.tracker.client.module.coords;

import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

/**
 * Client half of 自定义坐标追踪.
 *
 * <p>No packets and no server code: a coordinate is a fixed point, so the client draws it from
 * its own saved value. The switch only decides whether the overlay runs.</p>
 *
 * <p>The coordinate is entered with {@code /tracker coord <x> <y> <z>} (absolute or {@code ~}
 * relative to where the player stands). Entering one turns the switch on, so there is no
 * follow-up step to forget. It stays until the switch is turned off or the point is cleared.</p>
 */
public final class CustomCoordTrackingClient implements ClientTrackerModule {

    private boolean registered;

    @Override
    public String id() {
        return ModuleIds.CUSTOM_COORD_TRACKING;
    }

    @Override
    public void registerClient() {
        if (this.registered) {
            return;
        }
        this.registered = true;
        CoordOverlay.register();
    }

    /**
     * Turning the switch on with nothing saved yet is the one case worth explaining: the overlay
     * would otherwise be silently invisible and look broken.
     */
    @Override
    public void onEnabledChanged(boolean enabled) {
        if (enabled && CoordState.point() == null) {
            feedback(Component.translatable("tracker.coord.no_point").withStyle(ChatFormatting.YELLOW));
        }
    }

    /**
     * Sets the tracked coordinate and switches the feature on.
     *
     * @param relative values preceded by {@code ~} are offsets from the player's position
     * @return 1 on success, 0 when there is no world to resolve against
     */
    public static int set(String rawX, String rawY, String rawZ) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (minecraft.player == null || level == null) {
            feedback(Component.translatable("tracker.coord.no_world").withStyle(ChatFormatting.RED));
            return 0;
        }

        double baseX = minecraft.player.getX();
        double baseY = minecraft.player.getY();
        double baseZ = minecraft.player.getZ();
        Double x = axis(rawX, baseX);
        Double y = axis(rawY, baseY);
        Double z = axis(rawZ, baseZ);
        if (x == null || y == null || z == null) {
            feedback(Component.translatable("tracker.coord.bad_number").withStyle(ChatFormatting.RED));
            return 0;
        }

        CoordState.Point point = CoordState.store(
                level.dimension().location().toString(), x, y, z);
        setSwitch(true);
        feedback(Component.translatable("tracker.coord.set",
                Component.literal(point.pretty()).withStyle(ChatFormatting.AQUA)));
        return 1;
    }

    /** Re-enables the switch and reports the point that is already saved. */
    public static int show() {
        CoordState.Point point = CoordState.point();
        if (point == null) {
            feedback(Component.translatable("tracker.coord.no_point").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        setSwitch(true);
        feedback(Component.translatable("tracker.coord.resumed",
                Component.literal(point.pretty()).withStyle(ChatFormatting.AQUA)));
        return 1;
    }

    /** Turns the highlight off without forgetting the point. */
    public static int hide() {
        setSwitch(false);
        feedback(Component.translatable("tracker.coord.off"));
        return 1;
    }

    /** Forgets the saved point and turns the highlight off. */
    public static int clear() {
        boolean had = CoordState.forget();
        setSwitch(false);
        feedback(Component.translatable(had ? "tracker.coord.cleared" : "tracker.coord.no_point"));
        return had ? 1 : 0;
    }

    /**
     * Flips the switch and tells both registries.
     *
     * <p>The settings screen already does both halves; doing only the server half here would look
     * identical for this feature (its overlay polls the id every frame) and then silently skip the
     * client callback for any future module that relies on it. One place, both halves.</p>
     */
    private static void setSwitch(boolean enabled) {
        TrackerModules.setEnabled(ModuleIds.CUSTOM_COORD_TRACKING, enabled);
        com.heishandashu.tracker.client.module.ClientTrackerModules
                .notifyEnabledChanged(ModuleIds.CUSTOM_COORD_TRACKING, enabled);
    }

    /**
     * Parses one axis: {@code ~} / {@code ~d} is relative to {@code base}, anything else absolute.
     *
     * @return the value, or null when the text is not a number
     */
    private static Double axis(String raw, double base) {
        String text = raw.trim();
        try {
            if (text.equals("~")) {
                return base;
            }
            if (text.startsWith("~")) {
                return base + Double.parseDouble(text.substring(1));
            }
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void feedback(Component message) {
        com.heishandashu.tracker.compat.ClientChat.add(message);
    }
}
