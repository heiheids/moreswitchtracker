package com.heishandashu.tracker.client.command;

import com.heishandashu.tracker.client.module.coords.CustomCoordTrackingClient;
import com.heishandashu.tracker.client.module.legacy.LegacyGizmoRenderer;
import com.heishandashu.tracker.client.module.legacy.LegacyHighlightState;
import com.heishandashu.tracker.client.module.elite.EliteMobTrackingClient;
import com.heishandashu.tracker.client.module.gateway.EndGatewayTrackingClient;
import com.heishandashu.tracker.client.module.lag.LagSourceTrackingClient;
import com.heishandashu.tracker.client.module.traders.TraderSounds;
import com.heishandashu.tracker.client.module.traders.TraderTrackingClient;
import com.heishandashu.tracker.net.DtpNetworking;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/**
 * Client-side {@code /tracker ...} command.
 *
 * <p>The clickable coordinate in chat runs {@code /tracker show <id>}. Registering it on the
 * client means the click works in single player and on a server, needs no operator rights, and
 * never reaches the server.</p>
 */
public final class TrackerClientCommands {

    private TrackerClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("tracker")
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("show")
                                .then(RequiredArgumentBuilder
                                        .<FabricClientCommandSource, String>argument("id", StringArgumentType.word())
                                        .executes(ctx -> show(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("hide")
                                .executes(ctx -> {
                                    LegacyHighlightState.hide();
                                    ctx.getSource().sendFeedback(Component.translatable("tracker.command.hidden"));
                                    return 1;
                                }))
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("list")
                                .executes(ctx -> {
                                    FabricClientCommandSource source = ctx.getSource();
                                    if (!LegacyHighlightState.hasAnything()) {
                                        source.sendFeedback(Component.translatable("tracker.command.empty"));
                                        return 0;
                                    }
                                    for (DtpNetworking.DeathSnap snap : LegacyHighlightState.all()) {
                                        source.sendFeedback(Component.translatable("tracker.command.list_line",
                                                snap.id(),
                                                Component.literal(snap.x() + ", " + snap.y() + ", " + snap.z()),
                                                LegacyGizmoRenderer.formatTime(snap.remainingTicks()),
                                                snap.itemIds().size()));
                                    }
                                    return 1;
                                }))
                        // Clicking the wandering trader announcement in chat runs this.
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("trader")
                                .then(RequiredArgumentBuilder
                                        .<FabricClientCommandSource, String>argument("uuid",
                                                StringArgumentType.word())
                                        .executes(ctx -> TraderTrackingClient.highlight(
                                                StringArgumentType.getString(ctx, "uuid")))))
                        // Lag hot spots: with no argument it prints the list, with one it highlights.
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("lag")
                                .executes(ctx -> LagSourceTrackingClient.printList())
                                .then(RequiredArgumentBuilder
                                        .<FabricClientCommandSource, Integer>argument("id",
                                                IntegerArgumentType.integer(1, 9999))
                                        .executes(ctx -> LagSourceTrackingClient.highlight(
                                                IntegerArgumentType.getInteger(ctx, "id")))))
                        // Plays the trader arrival alert on demand, so the sound can be tested
                        // without waiting for a trader to spawn.
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("sound")
                                .executes(ctx -> {
                                    TraderSounds.playSpawnAlert();
                                    ctx.getSource().sendFeedback(
                                            Component.translatable("tracker.trader.sound_test"));
                                    return 1;
                                }))
                        // Elite mobs: no argument lists them, one highlights by uuid (used by the
                        // clickable coordinate in chat).
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("elite")
                                .executes(ctx -> EliteMobTrackingClient.printList())
                                .then(RequiredArgumentBuilder
                                        .<FabricClientCommandSource, String>argument("uuid",
                                                StringArgumentType.word())
                                        .executes(ctx -> EliteMobTrackingClient.highlight(
                                                StringArgumentType.getString(ctx, "uuid")))))
                        // Re-runs the end gateway search without touching the switch.
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("gateway")
                                .executes(ctx -> EndGatewayTrackingClient.search(
                                        net.minecraft.client.Minecraft.getInstance())))
                        // 自定义坐标追踪: no argument shows the saved point, an xyz sets a new one.
                        // The coordinates are free-form strings so "~" and "~5" work like vanilla.
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("coord")
                                .executes(ctx -> CustomCoordTrackingClient.show())
                                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("off")
                                        .executes(ctx -> CustomCoordTrackingClient.hide()))
                                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("clear")
                                        .executes(ctx -> CustomCoordTrackingClient.clear()))
                                .then(RequiredArgumentBuilder
                                        .<FabricClientCommandSource, String>argument("x",
                                                StringArgumentType.word())
                                        .then(RequiredArgumentBuilder
                                                .<FabricClientCommandSource, String>argument("y",
                                                        StringArgumentType.word())
                                                .then(RequiredArgumentBuilder
                                                        .<FabricClientCommandSource, String>argument("z",
                                                                StringArgumentType.word())
                                                        .executes(ctx -> CustomCoordTrackingClient.set(
                                                                StringArgumentType.getString(ctx, "x"),
                                                                StringArgumentType.getString(ctx, "y"),
                                                                StringArgumentType.getString(ctx, "z")))))))
        ));
    }

    private static int show(FabricClientCommandSource source, String id) {
        if (LegacyHighlightState.show(id)) {
            source.sendFeedback(Component.translatable("tracker.command.showing", id));
            return 1;
        }
        source.sendError(Component.translatable("tracker.command.unknown_id", id));
        return 0;
    }
}
