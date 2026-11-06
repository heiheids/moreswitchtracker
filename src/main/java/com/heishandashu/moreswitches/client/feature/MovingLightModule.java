package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.client.ClientTickHook;
import com.heishandashu.moreswitches.client.light.ClientHeldLight;
import com.heishandashu.moreswitches.config.SwitchIds;
import com.heishandashu.moreswitches.light.LightFalloff;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps the simulated light baked into the nearby section meshes, smoothly following the player.
 *
 * <p>Minecraft bakes block light into a section's vertex data when that section is meshed, and only
 * re-meshes when something marks the area dirty. {@code MixinLightCoordsUtil} changes what the mesher
 * samples, but on its own that never shows up: those sections were already meshed before the light
 * moved, so the picture stays as it was.</p>
 *
 * <p>The area is marked with {@code setBlocksDirty} rather than by dirtying whole sections. That
 * matters for smoothness: a section is 16 blocks wide, so dirtying sections only makes the light jump
 * once every 16 blocks of walking, which reads as a stutter. A block range is exact, so the light
 * follows the player continuously. The extractor expands the range to the sections it touches
 * itself.</p>
 *
 * <p>To keep that affordable, a rebuild is queued at most once every {@link #REBUILD_INTERVAL_TICKS}
 * ticks. At 20 ticks per second the light catches up within 100 ms of walking, which looks
 * continuous, while the re-mesh cost stays bounded no matter how fast the player moves.</p>
 */
public final class MovingLightModule implements ClientFeature {

    /** Rebuild at most this often; 2 ticks is 100 ms and reads as instant. */
    private static final int REBUILD_INTERVAL_TICKS = 2;

    /** Opt-in diagnostics: how far the effect got, printed every few seconds. */
    private static final String DEBUG_PROPERTY = "moreswitches.debugLight";

    private static final long REPORT_INTERVAL_MILLIS = 3_000L;

    private static int bakedMeshes;

    private static int dirtyRanges;

    private static long lastReport;

    private boolean active;

    private int cooldown;

    /** Block the light was centred on last time a rebuild was queued, or null. */
    private BlockPos lastCentre;

    /**
     * Counts one mesh that baked the simulated light.
     *
     * <p>Called from the light-coords injection. A growing number means the mesher is seeing the
     * simulated brightness; a number stuck at zero means the rebuilt sections hold no block the light
     * could brighten.</p>
     */
    public static void noteBakedMesh() {
        bakedMeshes++;
    }

    @Override
    public void register(String switchId) {
        this.active = enabled(switchId);
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' starts {}", MoreSwitchesMod.MOD_ID,
                SwitchIds.MOVING_LIGHT, this.active ? "on" : "off");
        ClientTickHook.onEndTick(this::tick);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (this.active == enabled) {
            return;
        }
        this.active = enabled;
        // Turning the switch off has to refresh the area too, otherwise the glow stays baked in
        // until the player happens to walk far enough for those sections to be rebuilt.
        this.lastCentre = null;
        this.cooldown = 0;
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' -> {}",
                MoreSwitchesMod.MOD_ID, SwitchIds.MOVING_LIGHT, enabled);
    }

    private void tick(Minecraft client) {
        if (!this.active || client.level == null || client.player == null) {
            return;
        }
        Vec3 centre = ClientHeldLight.centre(client);
        int emission = ClientHeldLight.emission(client);

        if (centre == null || emission <= 0) {
            if (this.lastCentre != null) {
                // The player put the light away: clear the glow that was baked in.
                invalidate(client, this.lastCentre, LightFalloff.reach(14));
                this.lastCentre = null;
            }
            return;
        }

        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }

        BlockPos current = BlockPos.containing(centre);
        if (current.equals(this.lastCentre)) {
            return;
        }
        invalidate(client, current, LightFalloff.reach(emission));
        this.lastCentre = current;
        this.cooldown = REBUILD_INTERVAL_TICKS;
        report(emission, centre);
    }

    /**
     * Marks every block the light can reach so the extractor rebuilds the sections that contain them.
     *
     * <p>The range is the light's reach in each direction, which is exactly the volume whose baked
     * brightness can change when the light moves by one block. Marking less would leave stale light at
     * the edge of the radius; marking more would rebuild sections nothing changed in.</p>
     */
    private static void invalidate(Minecraft client, BlockPos centre, int reach) {
        if (client.level == null) {
            return;
        }
        // 1.21.1 schedules section rebuilds through the level renderer, which takes a block range
        // and works out the sections itself. The newer extract-pipeline entry point does not exist
        // on this version.
        client.levelRenderer.setBlocksDirty(
                centre.getX() - reach, centre.getY() - reach, centre.getZ() - reach,
                centre.getX() + reach, centre.getY() + reach, centre.getZ() + reach);
        dirtyRanges++;
    }

    private static void report(int emission, Vec3 centre) {
        if (!Boolean.getBoolean(DEBUG_PROPERTY)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastReport < REPORT_INTERVAL_MILLIS) {
            return;
        }
        lastReport = now;
        MoreSwitchesMod.LOGGER.info(
                "[light-move] emission={} centre={},{},{} dirtyRanges={} bakedMeshes={}",
                emission,
                String.format("%.1f", centre.x),
                String.format("%.1f", centre.y),
                String.format("%.1f", centre.z),
                dirtyRanges, bakedMeshes);
    }
}
