package com.heishandashu.tracker.client.ui;

import com.heishandashu.tracker.client.module.ClientTrackerModules;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
            for (int line = 0; line < drawn; line++) {
                int lineY = startY + line * LINE_STEP;
                if (lineY < viewTop - 4 || lineY > viewBottom - 8) {
                    continue;
                }
                RowLine row = lines.get(line);
                g.text(this.font, row.text(), LABEL_X, lineY, row.color());
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
