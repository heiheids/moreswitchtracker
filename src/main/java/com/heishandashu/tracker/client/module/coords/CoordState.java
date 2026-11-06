package com.heishandashu.tracker.client.module.coords;

import com.heishandashu.tracker.config.TrackerConfig;
import com.heishandashu.tracker.module.coords.CustomCoordTrackingModule;
import net.minecraft.resources.ResourceLocation;

/**
 * The one coordinate 自定义坐标追踪 is currently pointing at, and its persistence.
 *
 * <p>Stored in {@code config/tracker.json} under {@code values} rather than in memory, because
 * the whole point of the feature is to come back to a place: closing the game or walking away
 * must not lose it. Nothing clears it except an explicit {@code /tracker coord clear}.</p>
 *
 * <p>The dimension is recorded with the coordinate. A point entered in the overworld is an
 * overworld point; drawing it in the nether at the same numbers would be actively misleading,
 * so the overlay checks the dimension instead.</p>
 */
public final class CoordState {

    /** A fixed place: dimension id plus double coordinates. */
    public record Point(String dimension, double x, double y, double z) {

        /** {@code [123, 64, -30]} for chat and labels. */
        public String pretty() {
            return "[" + round(x) + ", " + round(y) + ", " + round(z) + "]";
        }

        /** {@code 123 64 -30} for the settings text field (values as entered, no brackets). */
        public String editable() {
            return loose(x) + " " + loose(y) + " " + loose(z);
        }

        String encode() {
            return this.dimension + "|" + this.x + "|" + this.y + "|" + this.z;
        }
    }

    private CoordState() {
    }

    /** The saved point, or null when the player has never entered one. */
    public static Point point() {
        String raw = TrackerConfig.get().getValue(CustomCoordTrackingModule.SAVED_COORD_KEY, "");
        return decode(raw);
    }

    /** Remembers a new point and writes the config file. */
    public static Point store(String dimension, double x, double y, double z) {
        Point point = new Point(dimension, x, y, z);
        TrackerConfig.get().setValue(CustomCoordTrackingModule.SAVED_COORD_KEY, point.encode());
        return point;
    }

    /** Forgets the saved point (only ever from an explicit command). */
    public static boolean forget() {
        return TrackerConfig.get().removeValue(CustomCoordTrackingModule.SAVED_COORD_KEY);
    }

    /**
     * Parses {@code dimension|x|y|z}, returning null for anything malformed.
     *
     * <p>Deliberately tolerant: this string is hand-editable in the config file, so a bad edit
     * must read as "no point yet" instead of throwing during startup.</p>
     */
    static Point decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String[] parts = raw.split("\\|");
        if (parts.length != 4) {
            return null;
        }
        try {
            ResourceLocation dimension = ResourceLocation.tryParse(parts[0]);
            if (dimension == null) {
                return null;
            }
            return new Point(dimension.toString(),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String round(double value) {
        return value == Math.rint(value)
                ? Long.toString((long) value)
                : String.format("%.2f", value);
    }

    /** Dropping a pointless {@code .0} keeps the field readable while still being editable. */
    private static String loose(double value) {
        return round(value);
    }

    /**
     * Parses what the player typed into the settings field: three numbers, separated by spaces or
     * commas.
     *
     * <p>Only absolute numbers are accepted here -- {@code ~} needs a player position to resolve
     * against, which the command already does, and a text field that silently interpreted
     * {@code ~} would be surprising.</p>
     *
     * @param dimension the dimension to attach, or null when there is no world yet
     * @return the point, or null when the text is not (yet) three numbers
     */
    public static Point parseTyped(String text, String dimension) {
        if (text == null || dimension == null) {
            return null;
        }
        String cleaned = text.trim().replace(",", " ").replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            return null;
        }
        String[] parts = cleaned.split(" ");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new Point(dimension,
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
