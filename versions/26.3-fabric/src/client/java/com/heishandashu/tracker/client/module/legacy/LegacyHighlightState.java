package com.heishandashu.tracker.client.module.legacy;

import com.heishandashu.tracker.net.DtpNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side state for 遗产追踪: what the server last told us about, and which death the
 * player has actually clicked.
 *
 * <p>Nothing is computed here -- the remaining time comes straight from the server, because
 * only the server can see an item's real age. The client just displays it.</p>
 */
public final class LegacyHighlightState {

    private static final Map<String, DtpNetworking.DeathSnap> SNAPSHOTS = new LinkedHashMap<>();

    private static String activeId;

    private LegacyHighlightState() {
    }

    /** Called on the client thread when a sync arrives. */
    public static void accept(DtpNetworking.LegacySyncPayload payload) {
        SNAPSHOTS.clear();
        for (DtpNetworking.DeathSnap snap : payload.deaths()) {
            SNAPSHOTS.put(snap.id(), snap);
        }
        // The server decides when a death is over -- it owns the real item age and knows when a
        // countdown is merely paused. When it stops listing the death, the highlight goes too.
        if (activeId != null && !SNAPSHOTS.containsKey(activeId)) {
            activeId = null;
        }
    }

    /** Called once per client tick. */
    public static void tick() {
        // nothing to age out: the countdown belongs to the server
    }

    public static boolean show(String id) {
        if (!SNAPSHOTS.containsKey(id)) {
            return false;
        }
        activeId = id;
        return true;
    }

    public static void hide() {
        activeId = null;
    }

    public static boolean hasAnything() {
        return !SNAPSHOTS.isEmpty();
    }

    /** The death the player clicked, or {@code null}. */
    public static DtpNetworking.DeathSnap active() {
        return activeId == null ? null : SNAPSHOTS.get(activeId);
    }

    public static boolean isVisible(DtpNetworking.DeathSnap snap) {
        return snap != null && snap.id().equals(activeId);
    }

    /** All deaths the server is currently tracking, oldest first. */
    public static Collection<DtpNetworking.DeathSnap> all() {
        return List.copyOf(SNAPSHOTS.values());
    }

    public static List<String> ids() {
        return new ArrayList<>(SNAPSHOTS.keySet());
    }

    /** True when the snapshot belongs to the dimension the player is in. */
    public static boolean isCurrentDimension(DtpNetworking.DeathSnap snap) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || snap == null) {
            return false;
        }
        Identifier here = level.dimension().identifier();
        return here.toString().equals(snap.dimension());
    }
}
