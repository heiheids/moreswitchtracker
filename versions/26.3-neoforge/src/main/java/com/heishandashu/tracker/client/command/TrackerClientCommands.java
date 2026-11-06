package com.heishandashu.tracker.client.command;

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
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Client-side {@code /tracker ...} command.
 *
 * <p>The clickable coordinate in chat runs {@code /tracker show <id>}. Registering it on the
 * client means the click works in single player and on a server, needs no operator rights, and
 * never reaches the server.</p>
 *
 * <p>Fabric's {@code FabricClientCommandSource} does not exist on NeoForge: a client command uses
 * the vanilla {@link CommandSourceStack} that comes with {@link RegisterClientCommandsEvent}.
 * Feedback goes through the vanilla 26.3 pair {@code sendSuccess(Supplier, boolean)} /
 * {@code sendFailure(Component)}.</p>
 */
public final class TrackerClientCommands {

    private TrackerClientCommands() {
    }

    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("tracker")
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("show")
                                .then(RequiredArgumentBuilder
                                        .<CommandSourceStack, String>argument("id", StringArgumentType.word())
                                        .executes(ctx -> show(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("hide")
                                .executes(ctx -> {
                                    LegacyHighlightState.hide();
                                    feedback(ctx.getSource(), Component.translatable("tracker.command.hidden"));
                                    return 1;
                                }))
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("list")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    if (!LegacyHighlightState.hasAnything()) {
                                        feedback(source, Component.translatable("tracker.command.empty"));
                                        return 0;
                                    }
                                    for (DtpNetworking.DeathSnap snap : LegacyHighlightState.all()) {
                                        feedback(source, Component.translatable("tracker.command.list_line",
                                                snap.id(),
                                                Component.literal(snap.x() + ", " + snap.y() + ", " + snap.z()),
                                                LegacyGizmoRenderer.formatTime(snap.remainingTicks()),
                                                snap.itemIds().size()));
                                    }
                                    return 1;
                                }))
                        // Clicking the wandering trader announcement in chat runs this.
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("trader")
                                .then(RequiredArgumentBuilder
                                        .<CommandSourceStack, String>argument("uuid",
                                                StringArgumentType.word())
                                        .executes(ctx -> TraderTrackingClient.highlight(
                                                StringArgumentType.getString(ctx, "uuid")))))
                        // Lag hot spots: with no argument it prints the list, with one it highlights.
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("lag")
                                .executes(ctx -> LagSourceTrackingClient.printList())
                                .then(RequiredArgumentBuilder
                                        .<CommandSourceStack, Integer>argument("id",
                                                IntegerArgumentType.integer(1, 9999))
                                        .executes(ctx -> LagSourceTrackingClient.highlight(
                                                IntegerArgumentType.getInteger(ctx, "id")))))
                        // Plays the trader arrival alert on demand, so the sound can be tested
                        // without waiting for a trader to spawn.
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("sound")
                                .executes(ctx -> {
                                    TraderSounds.playSpawnAlert();
                                    feedback(ctx.getSource(),
                                            Component.translatable("tracker.trader.sound_test"));
                                    return 1;
                                }))
                        // Elite mobs: no argument lists them, one highlights by uuid (used by the
                        // clickable coordinate in chat).
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("elite")
                                .executes(ctx -> EliteMobTrackingClient.printList())
                                .then(RequiredArgumentBuilder
                                        .<CommandSourceStack, String>argument("uuid",
                                                StringArgumentType.word())
                                        .executes(ctx -> EliteMobTrackingClient.highlight(
                                                StringArgumentType.getString(ctx, "uuid")))))
                        // Re-runs the end gateway search without touching the switch.
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("gateway")
                                .executes(ctx -> EndGatewayTrackingClient.search(Minecraft.getInstance())))
        );
    }

    /**
     * Prints a line to whoever ran the command.
     *
     * <p>{@code false} keeps the reply out of the server log: this is client-side output and the
     * player is the only audience.</p>
     */
    private static void feedback(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
    }

    private static int show(CommandSourceStack source, String id) {
        if (LegacyHighlightState.show(id)) {
            feedback(source, Component.translatable("tracker.command.showing", id));
            return 1;
        }
        source.sendFailure(Component.translatable("tracker.command.unknown_id", id));
        return 0;
    }
}
