package com.heishandashu.moreswitches.carpet;

import java.util.ArrayList;
import java.util.List;

/**
 * Edits a Carpet configuration file so the {@code /player} command is usable in survival.
 *
 * <p>Carpet's rule {@code commandPlayer} defaults to {@code ops}, which means only operators may
 * spawn fake players — in a survival world without cheats nobody is an operator, so the command is
 * refused. Setting the rule to {@code true} lets every player use it.</p>
 *
 * <p>The file is a list of {@code rule value} lines separated by whitespace, with {@code #}
 * comments. This class rewrites only that one line and keeps everything else, including comments
 * and rules this mod does not know about, so a player's other Carpet settings survive.</p>
 */
public final class CarpetConfig {

    /** The rule that gates the {@code /player} command. */
    public static final String RULE = "commandPlayer";

    /** Value that allows every player, survival included. */
    public static final String VALUE_ALLOW = "true";

    /** Carpet's own default: only operators. */
    public static final String VALUE_DEFAULT = "ops";

    private CarpetConfig() {
    }

    /**
     * Returns {@code lines} with the {@code commandPlayer} rule set to {@code value}.
     *
     * <p>An existing line is replaced in place so the rest of the file keeps its order; when the
     * rule is absent it is appended. Comment and blank lines pass through untouched.</p>
     */
    public static List<String> withRule(List<String> lines, String value) {
        List<String> out = new ArrayList<>(lines.size() + 1);
        boolean replaced = false;
        for (String line : lines) {
            if (isRuleLine(line)) {
                if (!replaced) {
                    out.add(RULE + " " + value);
                    replaced = true;
                }
                // A duplicated rule line is dropped: Carpet reads the last one, so leaving it
                // would make the written value ambiguous.
                continue;
            }
            out.add(line);
        }
        if (!replaced) {
            out.add(RULE + " " + value);
        }
        return out;
    }

    /** True when this line sets {@link #RULE}, ignoring indentation and extra spaces. */
    public static boolean isRuleLine(String line) {
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return false;
        }
        int space = indexOfWhitespace(trimmed);
        String name = space < 0 ? trimmed : trimmed.substring(0, space);
        return RULE.equals(name);
    }

    /** The value currently written for {@link #RULE}, or null when the rule is absent. */
    public static String currentValue(List<String> lines) {
        for (String line : lines) {
            if (!isRuleLine(line)) {
                continue;
            }
            String trimmed = line.strip();
            int space = indexOfWhitespace(trimmed);
            if (space < 0 || space + 1 >= trimmed.length()) {
                return "";
            }
            return trimmed.substring(space + 1).strip();
        }
        return null;
    }

    private static int indexOfWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
