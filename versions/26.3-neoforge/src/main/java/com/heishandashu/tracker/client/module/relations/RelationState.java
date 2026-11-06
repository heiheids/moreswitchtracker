package com.heishandashu.tracker.client.module.relations;

import com.heishandashu.tracker.net.RelationNetworking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the client knows about the ray features: the lines the server last sent.
 *
 * <p>The lines are not remembered beyond the newest packet -- a mob that stops chasing something
 * should stop having a line drawn to it, and the server re-sends everything once a second.</p>
 */
public final class RelationState {

    private static final Map<String, RelationNetworking.RelationSnap> LINES = new LinkedHashMap<>();

    private RelationState() {
    }

    public static void accept(RelationNetworking.RelationListPayload payload) {
        LINES.clear();
        for (RelationNetworking.RelationSnap snap : payload.relations()) {
            LINES.put(key(snap), snap);
        }
    }

    public static List<RelationNetworking.RelationSnap> lines() {
        return new ArrayList<>(LINES.values());
    }

    public static void clear() {
        LINES.clear();
    }

    /** One line per source and kind: a mob can be tied to one bed and one workstation. */
    private static String key(RelationNetworking.RelationSnap snap) {
        return snap.kind() + "|" + snap.source() + "|" + snap.targetId();
    }

    /** A colour per relation kind, matching the request: workstations and zombies red, beds and the dragon blue. */
    public static int color(String kind) {
        return switch (kind) {
            case RelationNetworking.VILLAGER_JOB, RelationNetworking.ZOMBIE_AGGRO -> 0xFFFF3B30;
            case RelationNetworking.VILLAGER_BED, RelationNetworking.DRAGON_AGGRO -> 0xFF3B82F6;
            default -> 0xFFFFFFFF;
        };
    }

    /** Guards against a payload arriving before any level is joined. */
    public static boolean isLocal(UUID uuid) {
        return uuid != null;
    }
}
