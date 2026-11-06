package com.heishandashu.tracker.client.command;

import com.heishandashu.tracker.client.module.coords.CustomCoordTrackingClient;
import com.heishandashu.tracker.client.module.elite.EliteMobTrackingClient;
import com.heishandashu.tracker.client.module.gateway.EndGatewayTrackingClient;
import com.heishandashu.tracker.client.module.lag.LagSourceTrackingClient;
import com.heishandashu.tracker.client.module.legacy.LegacyGizmoRenderer;
import com.heishandashu.tracker.client.module.legacy.LegacyHighlightState;
import com.heishandashu.tracker.client.module.traders.TraderSounds;
import com.heishandashu.tracker.client.module.traders.TraderTrackingClient;
import com.heishandashu.tracker.net.DtpNetworking;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Client-side {@code /tracker ...} command.
 *
 * <p>NeoForge runs client commands in the vanilla dispatcher, so the source type is
 * {@link CommandSourceStack} rather than Fabric's {@code FabricClientCommandSource}. The command
 * is registered as a client command, which keeps the click-to-run chat links working in single
 * player and on a server without operator rights.</p>
 */
public final class TrackerClientCommands {

    private TrackerClientCommands() {
    }

    /** Builds the whole tree; the client-command event adds it to the dispatcher. */
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("tracker")
                .then(Commands.literal("show")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> show(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("hide")
                        .executes(ctx -> {
                            LegacyHighlightState.hide();
                            feedback(Component.translatable("tracker.command.hidden"));
                            return 1;
                        }))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            if (!LegacyHighlightState.hasAnything()) {
                                feedback(Component.translatable("tracker.command.empty"));
                                return 0;
                            }
                            for (DtpNetworking.DeathSnap snap : LegacyHighlightState.all()) {
                                feedback(Component.translatable("tracker.command.list_line",
                                        snap.id(),
                                        Component.literal(snap.x() + ", " + snap.y() + ", " + snap.z()),
                                        LegacyGizmoRenderer.formatTime(snap.remainingTicks()),
                                        snap.itemIds().size()));
                            }
                            return 1;
                        }))
                .then(Commands.literal("trader")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .executes(ctx -> TraderTrackingClient.highlight(
                                        StringArgumentType.getString(ctx, "uuid")))))
                .then(Commands.literal("lag")
                        .executes(ctx -> LagSourceTrackingClient.printList())
                        .then(Commands.argument("id", IntegerArgumentType.integer(1, 9999))
                                .executes(ctx -> LagSourceTrackingClient.highlight(
                                        IntegerArgumentType.getInteger(ctx, "id")))))
                .then(Commands.literal("sound")
                        .executes(ctx -> {
                            TraderSounds.playSpawnAlert();
                            feedback(Component.translatable("tracker.trader.sound_test"));
                            return 1;
                        }))
                .then(Commands.literal("elite")
                        .executes(ctx -> EliteMobTrackingClient.printList())
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .executes(ctx -> EliteMobTrackingClient.highlight(
                                        StringArgumentType.getString(ctx, "uuid")))))
                .then(Commands.literal("gateway")
                        .executes(ctx -> EndGatewayTrackingClient.search(Minecraft.getInstance())))
                // 自定义坐标追踪: no argument shows the saved point, an xyz sets a new one.
                .then(Commands.literal("coord")
                        .executes(ctx -> CustomCoordTrackingClient.show())
                        .then(Commands.literal("off")
                                .executes(ctx -> CustomCoordTrackingClient.hide()))
                        .then(Commands.literal("clear")
                                .executes(ctx -> CustomCoordTrackingClient.clear()))
                        .then(Commands.argument("x", StringArgumentType.word())
                                .then(Commands.argument("y", StringArgumentType.word())
                                        .then(Commands.argument("z", StringArgumentType.word())
                                                .executes(ctx -> CustomCoordTrackingClient.set(
                                                        StringArgumentType.getString(ctx, "x"),
                                                        StringArgumentType.getString(ctx, "y"),
                                                        StringArgumentType.getString(ctx, "z")))))));
    }

    private static int show(CommandSourceStack source, String id) {
        if (LegacyHighlightState.show(id)) {
            feedback(Component.translatable("tracker.command.showing", id));
            return 1;
        }
        feedback(Component.translatable("tracker.command.unknown_id", id));
        return 0;
    }

    private static void feedback(Component message) {
        com.heishandashu.tracker.compat.ClientChat.add(message);
    }
}
