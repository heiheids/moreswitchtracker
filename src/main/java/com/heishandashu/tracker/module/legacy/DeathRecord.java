package com.heishandashu.tracker.module.legacy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything remembered about one death of one player.
 *
 * <p>The record stays alive until every drop it produced has been accounted for, which is
 * also when the client is told to stop highlighting the area.</p>
 */
public final class DeathRecord {

    private final String id;
    private final UUID playerUuid;
    private final String playerName;
    private final ResourceLocation dimension;
    private final BlockPos pos;
    /** Tick the death happened on; ticking time, not wall clock. */
    private final long deathGameTime;
    private final Component cause;
    /** How many ticks after death we keep looking for new drops. */
    private final long collectUntilGameTime;

    private final Map<UUID, TrackedDrop> drops = new LinkedHashMap<>();
    /** Ticks left, as last computed. Frozen while the chunk is unloaded. */
    private int remainingTicks = LegacyTracker.DESPAWN_TICKS;
    /** Set once the client has been told about this death at least once. */
    private boolean announced;
    /** Drops reported as destroyed/picked up, collected into one chat summary at the end. */
    private boolean finished;
    /** Counts down while the death area is loaded; keeps the point clickable after the window. */
    private int retentionTicks = LegacyTracker.RETENTION_TICKS;

    public DeathRecord(String id, UUID playerUuid, String playerName, ResourceLocation dimension, BlockPos pos,
                       long deathGameTime, Component cause, long collectTicks) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.dimension = dimension;
        this.pos = pos;
        this.deathGameTime = deathGameTime;
        this.cause = cause;
        this.collectUntilGameTime = deathGameTime + collectTicks;
    }

    public String id() {
        return this.id;
    }

    public UUID playerUuid() {
        return this.playerUuid;
    }

    public String playerName() {
        return this.playerName;
    }

    public ResourceLocation dimension() {
        return this.dimension;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public long deathGameTime() {
        return this.deathGameTime;
    }

    public Component cause() {
        return this.cause;
    }

    public long collectUntilGameTime() {
        return this.collectUntilGameTime;
    }

    public Map<UUID, TrackedDrop> drops() {
        return this.drops;
    }

    public List<TrackedDrop> dropList() {
        return List.copyOf(this.drops.values());
    }

    public void addDrop(TrackedDrop drop) {
        this.drops.put(drop.uuid(), drop);
    }

    public int remainingTicks() {
        return this.remainingTicks;
    }

    public void remainingTicks(int remainingTicks) {
        this.remainingTicks = remainingTicks;
    }

    public boolean announced() {
        return this.announced;
    }

    public void announced(boolean announced) {
        this.announced = announced;
    }

    public boolean finished() {
        return this.finished;
    }

    public void finished(boolean finished) {
        this.finished = finished;
    }

    /** Ticks of loaded time this death point stays available for highlighting. */
    public int retentionTicks() {
        return this.retentionTicks;
    }

    public void retentionTicks(int retentionTicks) {
        this.retentionTicks = retentionTicks;
    }

    /** True when every drop has left the world, so the death can be forgotten. */
    public boolean allDropsGone() {
        if (this.drops.isEmpty()) {
            return false;
        }
        for (TrackedDrop drop : this.drops.values()) {
            if (drop.stillInWorld()) {
                return false;
            }
        }
        return true;
    }
}
