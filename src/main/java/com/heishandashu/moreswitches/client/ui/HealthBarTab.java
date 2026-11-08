package com.heishandashu.moreswitches.client.ui;

import com.heishandashu.moreswitches.config.HealthBarCategory;
import com.heishandashu.moreswitches.config.HealthBarDisplay;
import com.heishandashu.moreswitches.config.HealthBarSize;
import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.i18n.ModLang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The 血条 tab for the 1.21.1 build: what to draw above a creature, how big, and which kinds of
 * creature get one.
 *
 * <p>Two cycling buttons (display mode and size) followed by one switch per category. Row geometry
 * follows the tracker page: the block is centred when it fits and scrolls when the window is too
 * short for six rows.</p>
 */
final class HealthBarTab implements SettingsTab {

    private static final int LABEL_X = 26;
    private static final int TOGGLE_WIDTH = 42;
    private static final int TOGGLE_HEIGHT = 18;
    private static final int CYCLE_WIDTH = 116;
    private static final int CYCLE_HEIGHT = 20;
    private static final int ROW_HEIGHT_MAX = 34;
    private static final int ROW_HEIGHT_MIN = 20;
    private static final int LINE_STEP = 10;

    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_DESCRIPTION = 0xFF8A8A8A;
    private static final int TEXT_DIVIDER = 0x24FFFFFF;

    private static final int ROW_DISPLAY = 0;
    private static final int ROW_SIZE = 1;
    private static final int ROW_COUNT = 2 + 4;

    private static final HealthBarCategory[] CATEGORIES = {
            HealthBarCategory.MONSTER,
            HealthBarCategory.VILLAGER,
            HealthBarCategory.GOLEM,
            HealthBarCategory.ANIMAL
    };

    private SettingsScreen host;
    private Button displayButton;
    private Button sizeButton;
    private final List<ToggleSwitchWidget> categoryToggles = new ArrayList<>();

    private int rowHeight = ROW_HEIGHT_MAX;
    private int rowsTop;
    private int descriptionLines;
    private int scrollOffset;
    private int maxScroll;

    @Override
    public String titleKey() {
        return "moreswitchtracker.tab.health";
    }

    @Override
    public String hintKey() {
        return "moreswitchtracker.hint.health";
    }

    @Override
    public void init(SettingsScreen host) {
        this.host = host;

        int top = host.contentTop();
        int bottom = host.contentBottom();
        int viewHeight = Math.max(1, bottom - top);
        int available = Math.max(ROW_HEIGHT_MIN * ROW_COUNT, viewHeight);
        this.rowHeight = clamp(available / ROW_COUNT, ROW_HEIGHT_MIN, ROW_HEIGHT_MAX);
        this.descriptionLines = this.rowHeight >= 32 ? 1 : 0;

        int blockHeight = this.rowHeight * ROW_COUNT;
        this.rowsTop = top + Math.max(0, (viewHeight - blockHeight) / 2);
        this.maxScroll = Math.max(0, blockHeight - Math.max(1, bottom - this.rowsTop));
        this.scrollOffset = clamp(this.scrollOffset, 0, this.maxScroll);

        this.displayButton = Button.builder(
                        Component.literal(displayLabel()),
                        pressed -> cycleDisplay())
                .bounds(0, 0, CYCLE_WIDTH, CYCLE_HEIGHT)
                .build();
        host.addTabWidget(this.displayButton);

        this.sizeButton = Button.builder(
                        Component.literal(sizeLabel()),
                        pressed -> cycleSize())
                .bounds(0, 0, CYCLE_WIDTH, CYCLE_HEIGHT)
                .build();
        host.addTabWidget(this.sizeButton);

        this.categoryToggles.clear();
        for (HealthBarCategory category : CATEGORIES) {
            ToggleSwitchWidget toggle = new ToggleSwitchWidget(
                    0, 0, TOGGLE_WIDTH, TOGGLE_HEIGHT,
                    Component.literal(categoryName(category)),
                    () -> ModSettings.get().isHealthBarEnabled(category),
                    value -> ModSettings.get().setHealthBarEnabled(category, value));
            this.categoryToggles.add(toggle);
            host.addTabWidget(toggle);
        }

        layoutControls();
    }

    private void cycleDisplay() {
        ModSettings.get().setHealthBarDisplay(ModSettings.get().healthBarDisplay().next());
        this.displayButton.setMessage(Component.literal(displayLabel()));
    }

    private void cycleSize() {
        ModSettings.get().setHealthBarSize(ModSettings.get().healthBarSize().next());
        this.sizeButton.setMessage(Component.literal(sizeLabel()));
    }

    private static String displayLabel() {
        return ModLang.text("moreswitchtracker.health.display."
                + ModSettings.get().healthBarDisplay().id());
    }

    private static String sizeLabel() {
        return ModLang.text("moreswitchtracker.health.size."
                + ModSettings.get().healthBarSize().id());
    }

    private static String categoryName(HealthBarCategory category) {
        return ModLang.text("moreswitchtracker.health." + category.id());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int top = this.host.contentTop();
        int bottom = this.host.contentBottom();
        int listRight = this.host.guiWidth() - LABEL_X;

        // A row that only half fits must be cut at the content edge rather than drawn over the hint.
        graphics.enableScissor(LABEL_X - 6, top, listRight + 6, bottom);

        for (int row = 0; row < ROW_COUNT; row++) {
            int rowY = this.rowsTop + row * this.rowHeight - this.scrollOffset;
            if (rowY + this.rowHeight <= top || rowY >= bottom) {
                continue;
            }
            List<RowLine> lines = rowLines(row);
            int startY = rowY + Math.max(2, (this.rowHeight - lines.size() * LINE_STEP) / 2);
            for (int line = 0; line < lines.size(); line++) {
                RowLine rowLine = lines.get(line);
                graphics.drawString(this.host.guiFont(), rowLine.text(), LABEL_X,
                        startY + line * LINE_STEP, rowLine.color(), false);
            }
            if (row < ROW_COUNT - 1) {
                int dividerY = rowY + this.rowHeight - 1;
                graphics.fill(LABEL_X - 6, dividerY, listRight + 6, dividerY + 1, TEXT_DIVIDER);
            }
        }

        graphics.disableScissor();

        drawScrollbar(graphics, top, bottom);
    }

    private List<RowLine> rowLines(int row) {
        List<RowLine> lines = new ArrayList<>();
        if (row == ROW_DISPLAY) {
            lines.add(new RowLine(Component.literal(
                    ModLang.text("moreswitchtracker.health.display.label")), TEXT_PRIMARY));
            addDescription(lines, "moreswitchtracker.health.display.desc");
        } else if (row == ROW_SIZE) {
            lines.add(new RowLine(Component.literal(
                    ModLang.text("moreswitchtracker.health.size.label")), TEXT_PRIMARY));
            addDescription(lines, "moreswitchtracker.health.size.desc");
        } else {
            HealthBarCategory category = CATEGORIES[row - 2];
            lines.add(new RowLine(Component.literal(categoryName(category)), TEXT_PRIMARY));
            addDescription(lines, "moreswitchtracker.health.category.desc");
        }
        return lines;
    }

    private void addDescription(List<RowLine> lines, String key) {
        if (this.descriptionLines <= 0) {
            return;
        }
        String text = ModLang.text(key);
        if (!text.equals(key) && !text.isBlank()) {
            lines.add(new RowLine(Component.literal(text), TEXT_DESCRIPTION));
        }
    }

    private void drawScrollbar(GuiGraphics graphics, int viewTop, int viewBottom) {
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

    private void layoutControls() {
        int top = this.host.contentTop();
        int bottom = this.host.contentBottom();
        int rightX = this.host.guiWidth() - LABEL_X;
        int cycleX = rightX - CYCLE_WIDTH;
        int toggleX = rightX - TOGGLE_WIDTH;

        for (int row = 0; row < ROW_COUNT; row++) {
            int rowY = this.rowsTop + row * this.rowHeight - this.scrollOffset;
            boolean visible = rowY + this.rowHeight > top && rowY < bottom;
            if (row == ROW_DISPLAY) {
                place(this.displayButton, cycleX, rowY, CYCLE_HEIGHT, visible);
            } else if (row == ROW_SIZE) {
                place(this.sizeButton, cycleX, rowY, CYCLE_HEIGHT, visible);
            } else {
                place(this.categoryToggles.get(row - 2), toggleX, rowY, TOGGLE_HEIGHT, visible);
            }
        }
    }

    private void place(AbstractWidget widget, int x, int rowY, int height, boolean visible) {
        widget.setX(x);
        widget.setY(visible ? rowY + (this.rowHeight - height) / 2 : -1000);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.maxScroll <= 0) {
            return false;
        }
        int next = clamp(this.scrollOffset - (int) Math.round(scrollY * 14), 0, this.maxScroll);
        if (next != this.scrollOffset) {
            this.scrollOffset = next;
            layoutControls();
        }
        return true;
    }

    private record RowLine(Component text, int color) {
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }
}
