package com.heishandashu.tracker.client.ui;

import com.heishandashu.tracker.client.module.ClientTrackerModules;
import com.heishandashu.tracker.client.module.coords.CoordState;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The 追踪器 settings screen.
 *
 * <p>Layout (all in GUI units, so it follows the player's GUI scale):</p>
 * <ul>
 *   <li>title in two lines -- localised name first, fixed English second</li>
 *   <li>one row per feature module, each with a two-line name on the left
 *       (localised + fixed English) and a {@link ToggleSwitchWidget} on the right</li>
 *   <li>the row height also reserves space for a per-feature description in both
 *       languages; those lines appear automatically once the {@code .desc} /
 *       {@code .desc.en} language keys are filled in</li>
 *   <li>footer: a shortcut to the vanilla key-bindings screen, plus the vanilla Done button</li>
 * </ul>
 *
 * <p>Every switch is independent: flipping one notifies only that module.</p>
 */
public class TrackerSettingsScreen extends Screen {

    /** Height kept free for the title block. */
    private static final int TITLE_HEIGHT = 46;
    /** Height kept free for the footer buttons. */
    private static final int FOOTER_HEIGHT = 44;
    private static final int ROW_HEIGHT_MAX = 42;
    private static final int ROW_HEIGHT_MIN = 24;
    private static final int LABEL_X = 26;
    private static final int TOGGLE_WIDTH = 42;
    private static final int TOGGLE_HEIGHT = 18;
    /** Vertical distance between the lines of one row. */
    private static final int LINE_STEP = 10;
    /** Preferred width of the coordinate field, and the narrowest worth showing at all. */
    private static final int COORD_FIELD_WIDTH = 150;
    private static final int COORD_FIELD_MIN_WIDTH = 70;

    /**
     * The coordinate entry field, only on the 自定义坐标追踪 row.
     *
     * <p>It sits immediately left of that row's switch so the two read as one control: type a
     * coordinate, and the switch next to it says whether the highlight is running. Null when the
     * feature module is not registered (nothing to edit then).</p>
     */
    private CoordinateField coordField;
    private int coordRow = -1;
    /** Width of the field; 0 when the window is too narrow to give it a usable size. */
    private int coordFieldWidth;

    private final Screen parent;
    private final List<String> moduleIds = new ArrayList<>();

    private int rowHeight = ROW_HEIGHT_MAX;
    private int rowsTop = TITLE_HEIGHT;
    private int descriptionLines;

    /** Scrolling: how far the feature list is scrolled, and how far it may go. */
    private int scrollOffset;
    private int maxScroll;
    /** The switches, kept so they can follow the scroll. */
    private final List<ToggleSwitchWidget> toggles = new ArrayList<>();

    public TrackerSettingsScreen(Screen parent) {
        super(Component.translatable("tracker.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.moduleIds.clear();
        List<TrackerModule> modules = TrackerModules.all();
        for (TrackerModule module : modules) {
            this.moduleIds.add(module.id());
        }

        int rowCount = Math.max(1, modules.size());
        int available = Math.max(ROW_HEIGHT_MIN * rowCount, this.height - TITLE_HEIGHT - FOOTER_HEIGHT);
        this.rowHeight = clamp(available / rowCount, ROW_HEIGHT_MIN, ROW_HEIGHT_MAX);
        this.descriptionLines = this.rowHeight >= 42 ? 2 : this.rowHeight >= 32 ? 1 : 0;
        int blockHeight = this.rowHeight * rowCount;
        this.rowsTop = TITLE_HEIGHT + Math.max(0, (this.height - TITLE_HEIGHT - FOOTER_HEIGHT - blockHeight) / 2);

        // Once there are more features than fit, the list scrolls instead of shrinking further.
        int viewHeight = Math.max(1, this.height - FOOTER_HEIGHT - this.rowsTop);
        this.maxScroll = Math.max(0, blockHeight - viewHeight);
        this.scrollOffset = clamp(this.scrollOffset, 0, this.maxScroll);

        this.toggles.clear();
        for (int i = 0; i < modules.size(); i++) {
            TrackerModule module = modules.get(i);
            ToggleSwitchWidget toggle = new ToggleSwitchWidget(
                    0, 0, TOGGLE_WIDTH, TOGGLE_HEIGHT,
                    Component.translatable(nameKey(module.id())),
                    () -> TrackerModules.isEnabled(module),
                    value -> setModuleEnabled(module.id(), value));
            this.toggles.add(toggle);
            this.addRenderableWidget(toggle);
        }

        // The custom-coordinate row also gets a text field left of its switch. Sized from the
        // space the switch leaves, and skipped entirely when that would be unusably narrow.
        this.coordRow = this.moduleIds.indexOf(ModuleIds.CUSTOM_COORD_TRACKING);
        this.coordField = null;
        this.coordFieldWidth = 0;
        if (this.coordRow >= 0) {
            int roomForField = this.width - LABEL_X - TOGGLE_WIDTH - LABEL_X - 12;
            int width = Math.min(COORD_FIELD_WIDTH, roomForField);
            if (width >= COORD_FIELD_MIN_WIDTH) {
                this.coordFieldWidth = width;
                this.coordField = new CoordinateField(this.font, width, this::onCoordinateTyped);
                this.coordField.setTextQuietly(savedCoordinateText());
                this.addRenderableWidget(this.coordField.widget());
            }
        }

        layoutToggles();

        int buttonY = this.height - 27;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, buttonY, 200, 20)
                .build());

        // Bottom-left shortcut into the vanilla key-bindings screen; skipped when the
        // window is too narrow to fit it next to the Done button.
        int keybindsWidth = Math.min(120, this.width / 2 - 100 - LABEL_X - 8);
        if (keybindsWidth >= 70) {
            Minecraft minecraft = Minecraft.getInstance();
            this.addRenderableWidget(Button.builder(
                            Component.translatable("tracker.settings.keybinds"),
                            button -> minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options)))
                    .bounds(LABEL_X, buttonY, keybindsWidth, 20)
                    .build());
        }
    }

    private void setModuleEnabled(String id, boolean enabled) {
        TrackerModules.setEnabled(id, enabled);          // config file + server-side modules
        ClientTrackerModules.notifyEnabledChanged(id, enabled);  // client-side modules
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int centre = this.width / 2;
        g.centeredText(this.font, Component.translatable("tracker.settings.title"), centre, 16, 0xFFFFFFFF);
        g.centeredText(this.font, Component.translatable("tracker.settings.title.en"), centre, 28, 0xFFAAAAAA);
        g.fill(centre - 140, 38, centre + 140, 39, 0x40FFFFFF);

        int viewTop = this.rowsTop;
        int viewBottom = this.height - FOOTER_HEIGHT;
        int maxRowLines = 2 + this.descriptionLines;
        for (int i = 0; i < this.moduleIds.size(); i++) {
            String id = this.moduleIds.get(i);
            int rowY = this.rowsTop + i * this.rowHeight - this.scrollOffset;
            if (rowY + this.rowHeight <= viewTop || rowY >= viewBottom) {
                continue; // scrolled out of view
            }
            List<RowLine> lines = rowLines(id);
            int drawn = Math.min(lines.size(), maxRowLines);
            // Centre the text block inside the row instead of using fixed offsets: with a short row
            // the old fixed offsets pushed the last line past the row, straight through the divider.
            int startY = rowY + Math.max(2, (this.rowHeight - drawn * LINE_STEP) / 2);
            // The label must stop before whatever control sits on the right of this row, otherwise
            // a long description runs underneath the coordinate field / switch.
            int labelRight = rightEdgeOfRow(i) - 6;
            boolean clip = labelRight > LABEL_X;
            if (clip) {
                g.enableScissor(LABEL_X, Math.max(viewTop, rowY), labelRight,
                        Math.min(viewBottom, rowY + this.rowHeight));
            }
            for (int line = 0; line < drawn; line++) {
                int lineY = startY + line * LINE_STEP;
                if (lineY < viewTop - 4 || lineY > viewBottom - 8) {
                    continue;
                }
                RowLine row = lines.get(line);
                g.text(this.font, row.text(), LABEL_X, lineY, row.color());
            }
            if (clip) {
                g.disableScissor();
            }
            // Hairline in the empty gap at the bottom of the row, never across its text.
            if (i < this.moduleIds.size() - 1) {
                int dividerY = rowY + this.rowHeight - 1;
                if (dividerY > viewTop && dividerY < viewBottom) {
                    g.fill(LABEL_X - 6, dividerY, this.width - LABEL_X + 6, dividerY + 1, 0x24FFFFFF);
                }
            }
        }

        drawScrollbar(g, viewTop, viewBottom);

        Component hint = Component.translatable("tracker.settings.hint",
                Component.keybind("key.tracker.open_settings"));
        g.centeredText(this.font, hint, centre, this.height - 40, 0xFF909090);

        showRowTooltip(g, mouseX, mouseY);
    }

    /** Where the control on the right of a row starts, so the label knows where to stop. */
    private int rightEdgeOfRow(int row) {
        int toggleX = this.width - LABEL_X - TOGGLE_WIDTH;
        if (this.coordField != null && row == this.coordRow) {
            return toggleX - 6 - this.coordFieldWidth;
        }
        return toggleX;
    }

    /** A plain track-and-thumb bar, drawn only when the list actually overflows. */
    private void drawScrollbar(GuiGraphicsExtractor g, int viewTop, int viewBottom) {
        if (this.maxScroll <= 0) {
            return;
        }
        int x = this.width - 6;
        int trackHeight = Math.max(1, viewBottom - viewTop);
        g.fill(x, viewTop, x + 3, viewBottom, 0x30FFFFFF);
        int thumbHeight = Math.max(16, trackHeight * trackHeight / (trackHeight + this.maxScroll));
        int travel = trackHeight - thumbHeight;
        int thumbY = viewTop + (this.maxScroll == 0 ? 0 : travel * this.scrollOffset / this.maxScroll);
        g.fill(x, thumbY, x + 3, thumbY + thumbHeight, 0x90FFFFFF);
    }

    /** Moves the switches with the scroll, hiding the ones scrolled out of the list area. */
    private void layoutToggles() {
        int viewTop = this.rowsTop;
        int viewBottom = this.height - FOOTER_HEIGHT;
        int toggleX = this.width - LABEL_X - TOGGLE_WIDTH;
        for (int i = 0; i < this.toggles.size(); i++) {
            int rowY = this.rowsTop + i * this.rowHeight - this.scrollOffset;
            boolean visible = rowY + this.rowHeight > viewTop && rowY < viewBottom;
            ToggleSwitchWidget toggle = this.toggles.get(i);
            toggle.setX(toggleX);
            // Park off-screen when hidden so it cannot be hovered or clicked through the list edge.
            toggle.setY(visible ? rowY + (this.rowHeight - TOGGLE_HEIGHT) / 2 : -1000);
        }

        if (this.coordField != null) {
            int rowY = this.rowsTop + this.coordRow * this.rowHeight - this.scrollOffset;
            boolean visible = rowY + this.rowHeight > viewTop && rowY < viewBottom;
            EditBox box = this.coordField.widget();
            box.setX(toggleX - 6 - this.coordFieldWidth);
            box.setY(visible ? rowY + (this.rowHeight - TOGGLE_HEIGHT) / 2 : -1000);
        }
    }

    /**
     * Called on every keystroke in the coordinate field.
     *
     * <p>Parsing happens here rather than on a separate confirm button: a partly typed coordinate
     * merely fails to parse and is ignored, and the moment the text becomes a valid coordinate it
     * is saved and the highlight is switched on. That is what "type it and it tracks" implies,
     * and it removes a step the player would otherwise have to discover.</p>
     */
    private void onCoordinateTyped(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        CoordState.Point parsed = CoordState.parseTyped(text,
                minecraft.level == null ? null : minecraft.level.dimension().identifier().toString());
        if (parsed == null) {
            return; // not a complete coordinate yet
        }
        CoordState.store(parsed.dimension(), parsed.x(), parsed.y(), parsed.z());
        if (!TrackerModules.isEnabled(ModuleIds.CUSTOM_COORD_TRACKING)) {
            setModuleEnabled(ModuleIds.CUSTOM_COORD_TRACKING, true);
        }
    }

    /** The saved coordinate as editable text ({@code x y z}), or "" when there is none. */
    private static String savedCoordinateText() {
        CoordState.Point point = CoordState.point();
        return point == null ? "" : point.editable();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int next = clamp(this.scrollOffset - (int) Math.round(scrollY * 14), 0, this.maxScroll);
        if (next != this.scrollOffset) {
            this.scrollOffset = next;
            layoutToggles();
        }
        return true;
    }

    /**
     * Explains a feature while the pointer is over its row.
     *
     * <p>Deliberately skipped over the switch itself: the tooltip would sit right on top of the
     * control the player is about to click. The row still gets its explanation everywhere else,
     * so nothing is lost.</p>
     */
    private void showRowTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int toggleX = this.width - LABEL_X - TOGGLE_WIDTH;
        for (int i = 0; i < this.moduleIds.size(); i++) {
            int rowY = this.rowsTop + i * this.rowHeight - this.scrollOffset;
            if (mouseY < rowY || mouseY >= rowY + this.rowHeight) {
                continue;
            }
            if (mouseX < LABEL_X - 6 || mouseX > toggleX - 6) {
                continue; // not on the label part of the row
            }
            int toggleY = rowY + (this.rowHeight - TOGGLE_HEIGHT) / 2;
            boolean overToggle = mouseX >= toggleX - 4 && mouseX <= toggleX + TOGGLE_WIDTH + 4
                    && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT;
            if (overToggle) {
                return; // leave the switch alone
            }
            // Same for the coordinate field: it has its own hint, and a tooltip over an input box
            // is in the way of both reading and clicking it.
            if (this.coordField != null && i == this.coordRow) {
                int fieldX = toggleX - 6 - this.coordFieldWidth;
                if (mouseX >= fieldX - 4 && mouseX <= fieldX + this.coordFieldWidth + 4
                        && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT) {
                    return;
                }
            }

            String id = this.moduleIds.get(i);
            List<Component> tooltip = new ArrayList<>();
            for (RowLine row : rowLines(id)) {
                // The row itself colours its lines; a tooltip reads better in one shade.
                // copy() first: withStyle(ChatFormatting) only exists on MutableComponent.
                tooltip.add(tooltip.size() == 0
                        ? row.text().copy()
                        : row.text().copy().withStyle(ChatFormatting.GRAY));
            }
            g.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }
    }

    /** One rendered line: its text and the colour the row draws it in. */
    private record RowLine(Component text, int color) {
    }

    /**
     * The lines describing one feature, in the player's own language first.
     *
     * <p>The fixed English line is only appended when the player's language produced something
     * different. In an English game {@code .name} and {@code .name.en} resolve to the same string,
     * so adding it unconditionally would print every feature twice.</p>
     */
    private static List<RowLine> rowLines(String id) {
        List<RowLine> lines = new ArrayList<>();

        Component name = Component.translatable(nameKey(id));
        Component nameEn = Component.translatable(nameKeyEn(id));
        lines.add(new RowLine(name, 0xFFFFFFFF));
        if (!nameEn.getString().equals(name.getString())) {
            lines.add(new RowLine(nameEn, 0xFFAAAAAA));
        }

        Component description = Component.translatable(descKey(id));
        Component descriptionEn = Component.translatable(descKeyEn(id));
        if (!description.getString().isEmpty()) {
            lines.add(new RowLine(description, 0xFF8A8A8A));
            if (!descriptionEn.getString().equals(description.getString())) {
                lines.add(new RowLine(descriptionEn, 0xFF6E6E6E));
            }
        }
        return lines;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    /** Configuration screens should not freeze a single-player world. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    private static String nameKey(String id) {
        return "tracker.module." + id + ".name";
    }

    private static String nameKeyEn(String id) {
        return "tracker.module." + id + ".name.en";
    }

    private static String descKey(String id) {
        return "tracker.module." + id + ".desc";
    }

    private static String descKeyEn(String id) {
        return "tracker.module." + id + ".desc.en";
    }
}
