package com.heishandashu.moreswitches.client.ui;

import com.heishandashu.moreswitches.i18n.ModLang;
import com.heishandashu.tracker.client.module.ClientTrackerModules;
import com.heishandashu.tracker.client.module.coords.CoordState;
import com.heishandashu.tracker.client.ui.CoordinateField;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The “追踪器” tab: one scrollable row per tracker module.
 *
 * <p>Keeps the Tracker half's original row geometry, coordinate field and bottom-left key-binding
 * shortcut. The frame and drawing API come from the 26.3 shell.</p>
 */
final class TrackerSettingsTab implements SettingsTab {

    private static final int LABEL_X = 26;
    private static final int TOGGLE_WIDTH = 42;
    private static final int TOGGLE_HEIGHT = 18;
    private static final int ROW_HEIGHT_MAX = 42;
    private static final int ROW_HEIGHT_MIN = 24;
    private static final int LINE_STEP = 10;
    private static final int COORD_FIELD_WIDTH = 150;
    private static final int COORD_FIELD_MIN_WIDTH = 70;

    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_DESCRIPTION = 0xFF8A8A8A;
    private static final int TEXT_DIVIDER = 0x24FFFFFF;

    private SettingsScreen host;
    private final List<String> moduleIds = new ArrayList<>();
    private final List<ToggleSwitchWidget> toggles = new ArrayList<>();

    private CoordinateField coordField;
    private int coordRow = -1;
    private int coordFieldWidth;

    private int rowHeight = ROW_HEIGHT_MAX;
    private int rowsTop;
    private int descriptionLines;
    private int scrollOffset;
    private int maxScroll;

    @Override
    public String titleKey() {
        return "moreswitchtracker.tab.tracker";
    }

    @Override
    public String hintKey() {
        return "tracker.settings.hint";
    }

    @Override
    public void init(SettingsScreen host) {
        this.host = host;
        this.moduleIds.clear();
        List<TrackerModule> modules = TrackerModules.all();
        for (TrackerModule module : modules) {
            this.moduleIds.add(module.id());
        }

        int top = host.contentTop();
        int bottom = host.contentBottom();
        int rowCount = Math.max(1, modules.size());
        int viewHeight = Math.max(1, bottom - top);
        int available = Math.max(ROW_HEIGHT_MIN * rowCount, viewHeight);
        this.rowHeight = clamp(available / rowCount, ROW_HEIGHT_MIN, ROW_HEIGHT_MAX);
        this.descriptionLines = this.rowHeight >= 42 ? 2 : this.rowHeight >= 32 ? 1 : 0;

        int blockHeight = this.rowHeight * rowCount;
        this.rowsTop = top + Math.max(0, (viewHeight - blockHeight) / 2);
        this.maxScroll = Math.max(0, blockHeight - Math.max(1, bottom - this.rowsTop));
        this.scrollOffset = clamp(this.scrollOffset, 0, this.maxScroll);

        this.toggles.clear();
        for (TrackerModule module : modules) {
            ToggleSwitchWidget toggle = new ToggleSwitchWidget(
                    0, 0, TOGGLE_WIDTH, TOGGLE_HEIGHT,
                    Component.literal(ModLang.text(nameKey(module.id()))),
                    () -> TrackerModules.isEnabled(module),
                    value -> setModuleEnabled(module.id(), value));
            this.toggles.add(toggle);
            host.addTabWidget(toggle);
        }

        this.coordRow = this.moduleIds.indexOf(ModuleIds.CUSTOM_COORD_TRACKING);
        this.coordField = null;
        this.coordFieldWidth = 0;
        if (this.coordRow >= 0) {
            int roomForField = host.guiWidth() - LABEL_X - TOGGLE_WIDTH - LABEL_X - 12;
            int width = Math.min(COORD_FIELD_WIDTH, roomForField);
            if (width >= COORD_FIELD_MIN_WIDTH) {
                this.coordFieldWidth = width;
                this.coordField = new CoordinateField(host.guiFont(), width, this::onCoordinateTyped);
                this.coordField.setTextQuietly(savedCoordinateText());
                host.addTabWidget(this.coordField.widget());
            }
        }

        layoutToggles();
        addKeybindShortcut();
    }

    private void addKeybindShortcut() {
        int keybindsWidth = Math.min(120, this.host.guiWidth() / 2 - 100 - LABEL_X - 8);
        if (keybindsWidth < 70) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Button button = Button.builder(
                        Component.literal(ModLang.text("tracker.settings.keybinds")),
                        pressed -> minecraft.gui.setScreen(new KeyBindsScreen(this.host, minecraft.options)))
                .bounds(LABEL_X, this.host.guiHeight() - 27, keybindsWidth, 20)
                .build();
        this.host.addTabWidget(button);
    }

    private void setModuleEnabled(String id, boolean enabled) {
        TrackerModules.setEnabled(id, enabled);
        ClientTrackerModules.notifyEnabledChanged(id, enabled);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float partialTick) {
        int top = this.host.contentTop();
        int bottom = this.host.contentBottom();
        int toggleX = this.host.guiWidth() - LABEL_X - TOGGLE_WIDTH;
        int maxRowLines = 2 + this.descriptionLines;

        for (int index = 0; index < this.moduleIds.size(); index++) {
            int rowY = this.rowsTop + index * this.rowHeight - this.scrollOffset;
            if (rowY + this.rowHeight <= top || rowY >= bottom) {
                continue;
            }
            List<RowLine> lines = rowLines(this.moduleIds.get(index));
            int drawn = Math.min(lines.size(), maxRowLines);
            int startY = rowY + Math.max(2, (this.rowHeight - drawn * LINE_STEP) / 2);
            int labelRight = rightEdgeOfRow(index) - 6;
            boolean clip = labelRight > LABEL_X;
            if (clip) {
                graphics.enableScissor(LABEL_X, Math.max(top, rowY), labelRight,
                        Math.min(bottom, rowY + this.rowHeight));
            }
            for (int line = 0; line < drawn; line++) {
                int lineY = startY + line * LINE_STEP;
                if (lineY < top - 4 || lineY > bottom - 8) {
                    continue;
                }
                RowLine row = lines.get(line);
                graphics.text(this.host.guiFont(), row.text(), LABEL_X, lineY, row.color());
            }
            if (clip) {
                graphics.disableScissor();
            }
            if (index < this.moduleIds.size() - 1) {
                int dividerY = rowY + this.rowHeight - 1;
                if (dividerY > top && dividerY < bottom) {
                    graphics.fill(LABEL_X - 6, dividerY, this.host.guiWidth() - LABEL_X + 6,
                            dividerY + 1, TEXT_DIVIDER);
                }
            }
        }

        drawScrollbar(graphics, top, bottom);
        showRowTooltip(graphics, mouseX, mouseY);
    }

    private int rightEdgeOfRow(int row) {
        int toggleX = this.host.guiWidth() - LABEL_X - TOGGLE_WIDTH;
        if (this.coordField != null && row == this.coordRow) {
            return toggleX - 6 - this.coordFieldWidth;
        }
        return toggleX;
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int viewTop, int viewBottom) {
        if (this.maxScroll <= 0) {
            return;
        }
        int x = this.host.guiWidth() - 8;
        int trackHeight = Math.max(1, viewBottom - viewTop);
        graphics.fill(x, viewTop, x + 3, viewBottom, 0x30FFFFFF);
        int thumbHeight = Math.max(16, trackHeight * trackHeight / (trackHeight + this.maxScroll));
        int travel = trackHeight - thumbHeight;
        int thumbY = viewTop + travel * this.scrollOffset / this.maxScroll;
        graphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, 0x90FFFFFF);
    }

    private void layoutToggles() {
        int top = this.host.contentTop();
        int bottom = this.host.contentBottom();
        int toggleX = this.host.guiWidth() - LABEL_X - TOGGLE_WIDTH;
        for (int index = 0; index < this.toggles.size(); index++) {
            int rowY = this.rowsTop + index * this.rowHeight - this.scrollOffset;
            boolean visible = rowY + this.rowHeight > top && rowY < bottom;
            ToggleSwitchWidget toggle = this.toggles.get(index);
            toggle.setX(toggleX);
            toggle.setY(visible ? rowY + (this.rowHeight - TOGGLE_HEIGHT) / 2 : -1000);
        }

        if (this.coordField != null) {
            int rowY = this.rowsTop + this.coordRow * this.rowHeight - this.scrollOffset;
            boolean visible = rowY + this.rowHeight > top && rowY < bottom;
            EditBox box = this.coordField.widget();
            box.setX(toggleX - 6 - this.coordFieldWidth);
            box.setY(visible ? rowY + (this.rowHeight - TOGGLE_HEIGHT) / 2 : -1000);
        }
    }

    private void onCoordinateTyped(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        CoordState.Point parsed = CoordState.parseTyped(text,
                minecraft.level == null ? null : minecraft.level.dimension().identifier().toString());
        if (parsed == null) {
            return;
        }
        CoordState.store(parsed.dimension(), parsed.x(), parsed.y(), parsed.z());
        if (!TrackerModules.isEnabled(ModuleIds.CUSTOM_COORD_TRACKING)) {
            setModuleEnabled(ModuleIds.CUSTOM_COORD_TRACKING, true);
        }
    }

    private static String savedCoordinateText() {
        CoordState.Point point = CoordState.point();
        return point == null ? "" : point.editable();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.maxScroll <= 0) {
            return false;
        }
        int next = clamp(this.scrollOffset - (int) Math.round(scrollY * 14), 0, this.maxScroll);
        if (next != this.scrollOffset) {
            this.scrollOffset = next;
            layoutToggles();
        }
        return true;
    }

    private void showRowTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int top = this.host.contentTop();
        int bottom = this.host.contentBottom();
        if (mouseY < top || mouseY >= bottom) {
            return;
        }
        int toggleX = this.host.guiWidth() - LABEL_X - TOGGLE_WIDTH;
        for (int index = 0; index < this.moduleIds.size(); index++) {
            int rowY = this.rowsTop + index * this.rowHeight - this.scrollOffset;
            if (mouseY < rowY || mouseY >= rowY + this.rowHeight) {
                continue;
            }
            if (mouseX < LABEL_X - 6 || mouseX > toggleX - 6) {
                continue;
            }
            int toggleY = rowY + (this.rowHeight - TOGGLE_HEIGHT) / 2;
            boolean overToggle = mouseX >= toggleX - 4 && mouseX <= toggleX + TOGGLE_WIDTH + 4
                    && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT;
            if (overToggle) {
                return;
            }
            if (this.coordField != null && index == this.coordRow) {
                int fieldX = toggleX - 6 - this.coordFieldWidth;
                if (mouseX >= fieldX - 4 && mouseX <= fieldX + this.coordFieldWidth + 4
                        && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT) {
                    return;
                }
            }

            String id = this.moduleIds.get(index);
            List<Component> tooltip = new ArrayList<>();
            for (RowLine row : rowLines(id)) {
                tooltip.add(tooltip.isEmpty()
                        ? row.text().copy()
                        : row.text().copy().withStyle(ChatFormatting.GRAY));
            }
            graphics.setComponentTooltipForNextFrame(this.host.guiFont(), tooltip, mouseX, mouseY);
            return;
        }
    }

    private record RowLine(Component text, int color) {
    }

    private static List<RowLine> rowLines(String id) {
        List<RowLine> lines = new ArrayList<>();
        String name = ModLang.text(nameKey(id));
        if (!name.equals(nameKey(id))) {
            lines.add(new RowLine(Component.literal(name), TEXT_PRIMARY));
        }
        String description = ModLang.text(descKey(id));
        if (!description.equals(descKey(id)) && !description.isBlank()) {
            lines.add(new RowLine(Component.literal(description), TEXT_DESCRIPTION));
        }
        if (lines.isEmpty()) {
            lines.add(new RowLine(Component.literal(id), TEXT_PRIMARY));
        }
        return lines;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    private static String nameKey(String id) {
        return "tracker.module." + id + ".name";
    }

    private static String descKey(String id) {
        return "tracker.module." + id + ".desc";
    }
}
