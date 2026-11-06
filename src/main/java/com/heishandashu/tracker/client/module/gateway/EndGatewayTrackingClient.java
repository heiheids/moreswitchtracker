package com.heishandashu.tracker.client.module.gateway;

import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.gateway.EndGatewayTrackingModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Client half of 折跃门追踪器: finds the nearest end gateway when the switch is turned on.
 *
 * <p>Scans the chunks the client already has loaded ({@link ChunkAccess#findBlocks} walks the
 * palette, so a chunk with no gateway costs almost nothing) and remembers the nearest hit for ten
 * minutes. Flipping the switch off and on again searches afresh.</p>
 */
public final class EndGatewayTrackingClient implements ClientTrackerModule {

    /** The marked gateway: a fixed place, so a plain coordinate is enough. */
    record Marked(String dimension, double x, double y, double z, int untilTick) {

        int remaining(int now) {
            return Math.max(0, this.untilTick - now);
        }
    }

    private static Marked marked;
    private static int clientTicks;
    private boolean registered;

    @Override
    public String id() {
        return ModuleIds.END_GATEWAY_TRACKING;
    }

    @Override
    public void registerClient() {
        if (this.registered) {
            return;
        }
        this.registered = true;
        EndGatewayOverlay.register();
        com.heishandashu.tracker.compat.ClientEvents.addTick(() -> clientTicks++);
    }

    /**
     * Called when the switch is flipped. Turning it on (again) searches and marks; turning it off
     * forgets the mark so the next flip starts clean.
     */
    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            marked = null;
            return;
        }
        search(Minecraft.getInstance());
    }

    /** Runs the search and marks the nearest gateway. Also used by {@code /tracker gateway}. */
    public static int search(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        LocalPlayer self = minecraft.player;
        if (level == null || self == null) {
            return 0;
        }
        if (!level.dimension().equals(Level.END)) {
            feedback(minecraft, Component.translatable("tracker.gateway.wrong_dimension")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        BlockPos found = nearestGateway(level, self.blockPosition());
        if (found == null) {
            feedback(minecraft, Component.translatable("tracker.gateway.none",
                    Integer.toString(EndGatewayTrackingModule.SEARCH_CHUNKS * 16)));
            return 0;
        }

        marked = new Marked(level.dimension().location().toString(),
                found.getX() + 0.5, found.getY() + 0.5, found.getZ() + 0.5,
                clientTicks + EndGatewayTrackingModule.HIGHLIGHT_TICKS);
        feedback(minecraft, Component.translatable("tracker.gateway.found",
                Component.literal("[" + found.getX() + ", " + found.getY() + ", " + found.getZ() + "]")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)));
        return 1;
    }

    /** The closest {@code minecraft:end_gateway} block in the loaded chunks around {@code around}. */
    private static BlockPos nearestGateway(ClientLevel level, BlockPos around) {
        int centreChunkX = around.getX() >> 4;
        int centreChunkZ = around.getZ() >> 4;
        int radius = EndGatewayTrackingModule.SEARCH_CHUNKS;

        List<BlockPos> matches = new ArrayList<>();
        for (int cx = centreChunkX - radius; cx <= centreChunkX + radius; cx++) {
            for (int cz = centreChunkZ - radius; cz <= centreChunkZ + radius; cz++) {
                // requireChunk = false: never load anything, only look at what is already here.
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                chunk.findBlocks(state -> state.is(Blocks.END_GATEWAY),
                        (pos, state) -> matches.add(pos.immutable()));
            }
        }

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : matches) {
            double distance = pos.distSqr(around);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }
        return best;
    }

    /** For the overlay. */
    static Marked marked(int now) {
        if (marked == null || now >= marked.untilTick()) {
            return null;
        }
        return marked;
    }

    static int now() {
        return clientTicks;
    }

    static Vec3 position(Marked mark) {
        return new Vec3(mark.x(), mark.y(), mark.z());
    }

    private static void feedback(Minecraft minecraft, Component message) {
        com.heishandashu.tracker.compat.ClientChat.add(message);
    }
}
