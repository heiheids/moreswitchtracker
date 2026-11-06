package com.heishandashu.tracker.client.module.elite;

import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.module.elite.EliteMobTrackingModule;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client half of 精英怪追踪 / elite mob tracking: finds high-health mobs, announces them once, and
 * remembers which ones the player asked to highlight.
 *
 * <p>The scan runs once a second over the entities the client already has, so it costs nothing
 * while the switch is off and needs no server round trip. A mob counts as elite when it is hostile
 * and either has {@link EliteMobTrackingModule#ELITE_MIN_HEALTH} health or
 * {@link EliteMobTrackingModule#ELITE_HEALTH_RATIO} times the usual health for its kind.</p>
 */
public final class EliteMobTrackingClient implements ClientTrackerModule {

    /** Ten minutes, in client ticks. */
    static final int HIGHLIGHT_TICKS = 12000;
    private static final int SCAN_INTERVAL = 20;
    /** Announcements are capped per session so a crowded area cannot flood chat. */
    private static final int ANNOUNCE_LIMIT = 30;

    private static final Map<UUID, Elite> ELITES = new LinkedHashMap<>();
    private static final Map<UUID, Highlight> HIGHLIGHTS = new LinkedHashMap<>();

    private static int scanTimer;
    private static int clientTicks;
    private static int announced;

    private boolean registered;

    /** One elite mob as last seen. */
    record Elite(UUID uuid, String name, float health, float maxHealth, String dimension,
                 double x, double y, double z) {
    }

    /** A marked elite; kept for ten minutes even if the mob dies or leaves. */
    record Highlight(String name, float health, float maxHealth, String dimension,
                     double x, double y, double z, int untilTick) {

        int remaining() {
            return Math.max(0, this.untilTick - clientTicks);
        }
    }

    @Override
    public String id() {
        return ModuleIds.ELITE_MOB_TRACKING;
    }

    @Override
    public void registerClient() {
        if (this.registered) {
            return;
        }
        this.registered = true;
        TrackerClientEvents.onClientTick(() -> tick(Minecraft.getInstance()));
        EliteMobOverlay.register();
    }

    private static void tick(Minecraft minecraft) {
        clientTicks++;
        HIGHLIGHTS.entrySet().removeIf(entry -> clientTicks >= entry.getValue().untilTick());

        if (!TrackerModules.isEnabled(ModuleIds.ELITE_MOB_TRACKING)) {
            ELITES.clear();
            return;
        }
        if (++scanTimer < SCAN_INTERVAL) {
            refreshHighlights(minecraft);
            return;
        }
        scanTimer = 0;
        scan(minecraft);
        refreshHighlights(minecraft);
    }

    private static void scan(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        LocalPlayer self = minecraft.player;
        if (level == null || self == null) {
            ELITES.clear();
            return;
        }
        String dimension = level.dimension().identifier().toString();
        Map<UUID, Elite> found = new LinkedHashMap<>();

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Mob mob) || !(mob instanceof Enemy)
                    || !(entity instanceof LivingEntity living)) {
                continue;
            }
            float maxHealth = living.getMaxHealth();
            if (!isElite(mob, maxHealth)) {
                continue;
            }
            Elite elite = new Elite(mob.getUUID(), mob.getName().getString(),
                    living.getHealth(), maxHealth, dimension,
                    mob.getX(), mob.getY(), mob.getZ());
            found.put(elite.uuid(), elite);

            if (!ELITES.containsKey(elite.uuid())) {
                announce(elite, self);
            }
        }
        ELITES.clear();
        ELITES.putAll(found);
    }

    /** A hostile mob is elite when it is simply huge, or far tougher than its kind normally is. */
    private static boolean isElite(Mob mob, float maxHealth) {
        if (maxHealth >= EliteMobTrackingModule.ELITE_MIN_HEALTH) {
            return true;
        }
        AttributeSupplier supplier;
        try {
            // Entity#getType() is EntityType<?>; DefaultAttributes wants the LivingEntity bound.
            @SuppressWarnings("unchecked")
            EntityType<? extends LivingEntity> type =
                    (EntityType<? extends LivingEntity>) mob.getType();
            supplier = DefaultAttributes.getSupplier(type);
        } catch (RuntimeException e) {
            return false; // a type without default attributes cannot be compared
        }
        if (supplier == null) {
            return false;
        }
        double usual = supplier.getValue(Attributes.MAX_HEALTH);
        return usual > 0 && maxHealth >= usual * EliteMobTrackingModule.ELITE_HEALTH_RATIO;
    }

    private static void announce(Elite elite, LocalPlayer self) {
        if (announced >= ANNOUNCE_LIMIT || elite.dimension() == null) {
            return;
        }
        announced++;

        MutableComponent coordinates = Component.literal(
                        "[" + (int) elite.x() + ", " + (int) elite.y() + ", " + (int) elite.z() + "]")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/tracker elite " + elite.uuid()))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("tracker.elite.click_hint"))));

        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(
                Component.translatable("tracker.elite.found",
                        Component.literal(elite.name()).withStyle(ChatFormatting.RED),
                        String.format("%.0f/%.0f", elite.health(), elite.maxHealth()),
                        coordinates));
    }

    /** Follows marked mobs with their live position and health while they are loaded. */
    private static void refreshHighlights(Minecraft minecraft) {
        if (HIGHLIGHTS.isEmpty() || minecraft.level == null) {
            return;
        }
        for (Map.Entry<UUID, Highlight> entry : HIGHLIGHTS.entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity instanceof LivingEntity living) {
                Highlight old = entry.getValue();
                entry.setValue(new Highlight(old.name(), living.getHealth(), living.getMaxHealth(),
                        old.dimension(), entity.getX(), entity.getY(), entity.getZ(), old.untilTick()));
            }
        }
    }

    /** Called by the clickable coordinate and by {@code /tracker elite <uuid>}. */
    public static int highlight(String rawUuid) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID uuid;
        try {
            uuid = UUID.fromString(rawUuid);
        } catch (IllegalArgumentException e) {
            feedback(minecraft, Component.translatable("tracker.elite.unknown"));
            return 0;
        }
        Elite elite = ELITES.get(uuid);
        if (elite == null) {
            feedback(minecraft, Component.translatable("tracker.elite.unknown"));
            return 0;
        }
        HIGHLIGHTS.put(uuid, new Highlight(elite.name(), elite.health(), elite.maxHealth(),
                elite.dimension(), elite.x(), elite.y(), elite.z(), clientTicks + HIGHLIGHT_TICKS));
        feedback(minecraft, Component.translatable("tracker.elite.highlighted", elite.name()));
        return 1;
    }

    /** Prints the elites currently in range. */
    public static int printList() {
        Minecraft minecraft = Minecraft.getInstance();
        if (ELITES.isEmpty()) {
            feedback(minecraft, Component.translatable("tracker.elite.empty"));
            return 0;
        }
        feedback(minecraft, Component.translatable("tracker.elite.header"));
        for (Elite elite : ELITES.values()) {
            feedback(minecraft, Component.translatable("tracker.elite.line",
                    Component.literal(elite.name()),
                    String.format("%.0f/%.0f", elite.health(), elite.maxHealth()),
                    Component.literal(String.format("%.0f, %.0f, %.0f",
                            elite.x(), elite.y(), elite.z()))));
        }
        feedback(minecraft, Component.translatable("tracker.elite.hint").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    static List<Highlight> highlights() {
        return new ArrayList<>(HIGHLIGHTS.values());
    }

    static List<Elite> elites() {
        return new ArrayList<>(ELITES.values());
    }

    /** Marks elites within range of the player, for the overhead health readout. */
    static boolean withinRange(Highlight highlight, Vec3 eye) {
        return eye.distanceTo(new Vec3(highlight.x(), highlight.y(), highlight.z()))
                <= EliteMobTrackingModule.OVERHEAD_RANGE;
    }

    private static void feedback(Minecraft minecraft, Component message) {
        minecraft.gui.hud.getChat().addClientSystemMessage(message);
    }

    /** mm:ss, matching the other countdowns in the mod. */
    static String clock(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            ELITES.clear();
            HIGHLIGHTS.clear();
        }
    }
}
