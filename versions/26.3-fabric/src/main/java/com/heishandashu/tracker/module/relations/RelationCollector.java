package com.heishandashu.tracker.module.relations;

import com.heishandashu.tracker.net.RelationNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the server-side ties the ray features draw and pushes them to clients once a second.
 *
 * <p>None of this data reaches clients on its own: a villager's claimed bed and workstation live in
 * its brain, and a mob's attack target is a server-side field. So the server reads them and sends a
 * plain list of "from here to there" lines.</p>
 *
 * <p>Only mobs within {@link #RADIUS} blocks of a player are collected, and nothing is collected at
 * all while every ray switch is off.</p>
 */
public final class RelationCollector {

    private static final int SYNC_INTERVAL = 20;
    private static final double RADIUS = 64.0;
    private static final int MAX_RELATIONS = 512;

    private static boolean initialized;
    private static int timer;

    private RelationCollector() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(RelationCollector::tick);
    }

    private static void tick(MinecraftServer server) {
        boolean villager = RelationModules.villagerPoiActive();
        boolean zombie = RelationModules.zombieAggroActive();
        boolean dragon = RelationModules.dragonAggroActive();
        if (!villager && !zombie && !dragon) {
            return; // switches off: no work at all
        }
        if (++timer < SYNC_INTERVAL) {
            return;
        }
        timer = 0;

        List<RelationNetworking.RelationSnap> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ServerLevel level : server.getAllLevels()) {
            if (level.players().isEmpty()) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                AABB box = player.getBoundingBox().inflate(RADIUS);
                if (villager) {
                    for (Villager subject : level.getEntitiesOfClass(Villager.class, box)) {
                        collectVillager(subject, out, seen);
                    }
                }
                if (zombie) {
                    for (Zombie subject : level.getEntitiesOfClass(Zombie.class, box)) {
                        collectZombie(subject, out, seen);
                    }
                }
                if (dragon) {
                    for (EnderDragon subject : level.getEntitiesOfClass(EnderDragon.class, box)) {
                        collectDragon(subject, level, out, seen);
                    }
                }
            }
        }

        if (out.size() > MAX_RELATIONS) {
            out = out.subList(0, MAX_RELATIONS);
        }
        RelationNetworking.RelationListPayload payload = new RelationNetworking.RelationListPayload(out);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /** A villager's claimed bed and workstation, straight out of its brain. */
    private static void collectVillager(Villager villager, List<RelationNetworking.RelationSnap> out,
                                        Set<String> seen) {
        memory(villager, MemoryModuleType.HOME)
                .ifPresent(global -> add(RelationNetworking.VILLAGER_BED, villager, global, "", out, seen));
        memory(villager, MemoryModuleType.JOB_SITE)
                .ifPresent(global -> add(RelationNetworking.VILLAGER_JOB, villager, global, "", out, seen));
    }

    private static Optional<GlobalPos> memory(Villager villager, MemoryModuleType<GlobalPos> type) {
        try {
            return villager.getBrain().getMemory(type);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static void collectZombie(Zombie zombie, List<RelationNetworking.RelationSnap> out,
                                      Set<String> seen) {
        LivingEntity target = zombie.getTarget();
        if (target == null) {
            return;
        }
        addAt(RelationNetworking.ZOMBIE_AGGRO, zombie, target, out, seen);
    }

    /** The dragon's own target when it has one, otherwise the closest player (what it locks onto). */
    private static void collectDragon(EnderDragon dragon, ServerLevel level,
                                      List<RelationNetworking.RelationSnap> out, Set<String> seen) {
        LivingEntity target = dragon.getTarget();
        if (target == null) {
            double best = Double.MAX_VALUE;
            for (ServerPlayer player : level.players()) {
                double distance = player.distanceToSqr(dragon);
                if (distance < best) {
                    best = distance;
                    target = player;
                }
            }
        }
        if (target == null) {
            return;
        }
        addAt(RelationNetworking.DRAGON_AGGRO, dragon, target, out, seen);
    }

    private static void add(String kind, LivingEntity source, GlobalPos target, String targetId,
                            List<RelationNetworking.RelationSnap> out, Set<String> seen) {
        String dimension = source.level().dimension().identifier().toString();
        if (!target.dimension().identifier().toString().equals(dimension)) {
            return; // the claimed block is in another dimension; nothing sensible to draw
        }
        String key = kind + "|" + source.getUUID() + "|" + target.pos().asLong();
        if (!seen.add(key)) {
            return;
        }
        out.add(new RelationNetworking.RelationSnap(kind, source.getUUID(), dimension,
                source.getX(), source.getY(), source.getZ(),
                targetId, target.pos().getX() + 0.5, target.pos().getY() + 0.5, target.pos().getZ() + 0.5));
    }

    private static void addAt(String kind, LivingEntity source, LivingEntity target,
                              List<RelationNetworking.RelationSnap> out, Set<String> seen) {
        String dimension = source.level().dimension().identifier().toString();
        String key = kind + "|" + source.getUUID() + "|" + target.getUUID();
        if (!seen.add(key)) {
            return;
        }
        out.add(new RelationNetworking.RelationSnap(kind, source.getUUID(), dimension,
                source.getX(), source.getY(), source.getZ(),
                target.getUUID().toString(), target.getX(), target.getY(), target.getZ()));
    }
}
