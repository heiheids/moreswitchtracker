package com.heishandashu.tracker.module.persistent;

import com.heishandashu.tracker.TrackerMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the picked-mob list: in chat while it is short, in a text file once it is long.
 *
 * <p>Chat lines are built from translatable components, so the client renders them in whatever
 * language the game is set to. The file is written into the world save directory and its name is a
 * clickable link that opens it with the system's default editor.</p>
 */
public final class PickedMobReport {

    /** At this many records the chat list is replaced by the file. */
    static final int CHAT_LIMIT = 10;
    private static final String FILE_NAME = "tracker-picked-mobs.txt";

    private PickedMobReport() {
    }

    public static void report(MinecraftServer server, List<PickedMobRecord> records) {
        if (server == null || records.isEmpty()) {
            return;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        if (records.size() < CHAT_LIMIT) {
            for (ServerPlayer player : players) {
                player.sendSystemMessage(Component.translatable("tracker.picked.header", records.size()));
                for (PickedMobRecord record : records) {
                    player.sendSystemMessage(line(record));
                }
                player.sendSystemMessage(Component.translatable("tracker.picked.hint")
                        .withStyle(ChatFormatting.GRAY));
            }
            return;
        }

        Path file = writeFile(server, records);
        if (file == null) {
            return;
        }
        for (ServerPlayer player : players) {
            player.sendSystemMessage(Component.translatable("tracker.picked.file_written",
                    records.size(), fileLink(file)));
            player.sendSystemMessage(Component.translatable("tracker.picked.file_path", file.toString())
                    .withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.translatable("tracker.picked.hint")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** One chat line: number, mob, item, position -- all language-neutral until the client draws it. */
    private static Component line(PickedMobRecord record) {
        return Component.translatable("tracker.picked.line",
                Component.literal(record.label()).withStyle(ChatFormatting.GOLD),
                mobName(record),
                record.stack().getHoverName(),
                record.stack().getCount(),
                dimensionName(record.dimension()),
                Component.literal(coords(record.pos())));
    }

    private static Component mobName(PickedMobRecord record) {
        if (!record.customName().isEmpty()) {
            return Component.literal(record.customName());
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(record.typeId());
        return type == null ? Component.literal(record.typeId().toString()) : type.getDescription();
    }

    private static Component dimensionName(ResourceLocation dimension) {
        String key = "tracker.dimension." + dimension.getNamespace() + "." + dimension.getPath();
        return Component.translatableWithFallback(key, dimension.toString());
    }

    private static String coords(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    /** Cyan, underlined, opens the file when clicked. */
    private static Component fileLink(Path file) {
        return Component.literal(FILE_NAME)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.toString()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                Component.translatable("tracker.picked.file_hover"))));
    }

    /** Plain-text record in the world save folder. */
    private static Path writeFile(MinecraftServer server, List<PickedMobRecord> records) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        List<String> lines = new ArrayList<>();
        lines.add("tracker - mobs holding picked-up items (" + records.size() + ")");
        lines.add("type a #id in chat to highlight that mob in game for 10 minutes");
        lines.add("");
        for (PickedMobRecord record : records) {
            lines.add(String.format("%s  %s  |  %s x%d  |  %s  %s",
                    record.label(),
                    mobName(record).getString(),
                    record.stack().getHoverName().getString(),
                    record.stack().getCount(),
                    record.dimension(),
                    coords(record.pos())));
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            TrackerMod.LOGGER.warn("[tracker] picked-mob: could not write {}", file, e);
            return null;
        }
    }
}
