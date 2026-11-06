package com.heishandashu.tracker.client.module.players;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Pure logic for the {@code @} mention list: which mention is being typed, which names match it,
 * and which one is selected.
 *
 * <p>No rendering and no input handling live here, so both the popup and the key handling can ask
 * the same questions without either owning the other. Nothing here ever consumes or rewrites a
 * typed {@code @} -- the character always reaches the chat box; this only describes what could be
 * completed.</p>
 */
public final class MentionSuggestions {

    /** Rows shown at most. */
    public static final int MAX_ROWS = 8;

    private static int selected;
    private static String lastQuery = "";

    private MentionSuggestions() {
    }

    /** A candidate: an online player. */
    public record Suggestion(UUID uuid, String name) {
    }

    /**
     * The mention being typed.
     *
     * @param atIndex index of the {@code @}
     * @param query   text typed after it, up to the cursor
     */
    public record Token(int atIndex, String query) {
    }

    /** The mention at the cursor, or null when the cursor is not inside one. */
    public static Token tokenAt(String text, int cursor) {
        if (text == null || text.isEmpty() || cursor <= 0) {
            return null;
        }
        int limit = Math.min(cursor, text.length());
        int at = text.lastIndexOf('@', limit - 1);
        if (at < 0) {
            return null;
        }
        // must start a word: "a@b" is not a mention
        if (at > 0 && !Character.isWhitespace(text.charAt(at - 1))) {
            return null;
        }
        String query = text.substring(at + 1, limit);
        if (query.length() > 16) {
            return null;
        }
        for (int i = 0; i < query.length(); i++) {
            if (!isNameChar(query.charAt(i))) {
                return null; // a space or any other character ends the mention
            }
        }
        return new Token(at, query);
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** Online players whose name starts with what has been typed, sorted by name. */
    public static List<Suggestion> matches(String query) {
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
        List<Suggestion> found = new ArrayList<>();
        for (PlayerInfo info : online()) {
            String name = info.getProfile().name();
            if (needle.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(needle)) {
                found.add(new Suggestion(info.getProfile().id(), name));
            }
        }
        return found;
    }

    private static List<PlayerInfo> online() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }
        List<PlayerInfo> players = new ArrayList<>(connection.getOnlinePlayers());
        players.sort(Comparator.comparing(info -> info.getProfile().name(), String.CASE_INSENSITIVE_ORDER));
        return players;
    }

    /** Resets the selection whenever the typed text changes, and keeps it inside the list. */
    public static void syncSelection(String query, int matchCount) {
        if (!query.equals(lastQuery)) {
            lastQuery = query;
            selected = 0;
        }
        selected = matchCount <= 0 ? 0 : Math.floorMod(selected, matchCount);
    }

    public static int selected() {
        return selected;
    }

    public static void move(int delta, int matchCount) {
        if (matchCount <= 0) {
            selected = 0;
            return;
        }
        selected = Math.floorMod(selected + delta, matchCount);
    }

    public static void reset() {
        selected = 0;
        lastQuery = "";
    }

    /**
     * The chat text with the mention replaced by the full name.
     *
     * @return the new text, or null when nothing should change
     */
    public static String completedText(String text, Token token, Suggestion suggestion) {
        if (text == null || token == null || suggestion == null) {
            return null;
        }
        int insertEnd = token.atIndex() + 1 + token.query().length();
        if (insertEnd > text.length()) {
            insertEnd = text.length();
        }
        String before = text.substring(0, Math.min(token.atIndex(), text.length()));
        String after = text.substring(insertEnd);
        return before + "@" + suggestion.name() + " " + after;
    }

    /** Cursor position that belongs right after the completed mention. */
    public static int completedCursor(Token token, Suggestion suggestion) {
        return token.atIndex() + 1 + suggestion.name().length() + 1;
    }
}
