package com.heishandashu.moreswitches.client.ui;

import com.heishandashu.moreswitches.client.feature.ClientFeatures;
import com.heishandashu.moreswitches.client.feature.FireworkCooldownModule;
import com.heishandashu.moreswitches.config.SwitchDefinition;
import com.heishandashu.moreswitches.config.SwitchIds;
import com.heishandashu.moreswitches.i18n.ModLang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * The “更多开关” tab: one scrollable row per utility switch.
 *
 * <p>This is the original More Switches list with the window frame removed. The row geometry,
 * switch position and scrolling rules are unchanged; only the title band, language button and Done
 * button belong to {@link SettingsScreen}, because those are shared by both tabs.</p>
 */
final class SwitchesSettingsTab implements SettingsTab {

    private static final int PADDING_X = 24;
    private static final int LABEL_GAP = 8;
    private static final int LINE_STEP = 11;
    private static final int PREFERRED_ROW_HEIGHT = LINE_STEP * 3 + 4;
    private static final int MIN_ROW_HEIGHT = LINE_STEP * 2 + 4;

    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_GAP = 4;
    private static final int SCROLL_ARROW_HEIGHT = 9;

    private static final int TEXT_COLOR_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_COLOR_HINT = 0xFF909090;
    private static final int DIVIDER_COLOR = 0x24FFFFFF;
    private static final int SCROLLBAR_TRACK = 0x30FFFFFF;
    private static final int SCROLLBAR_THUMB = 0xFFC6C6C6;
    private static final int SCROLLBAR_ARROW = 0xFFA0A0A0;

    private SettingsScreen host;
    private final List<String> switchIds = new ArrayList<>();
    private final List<ToggleSwitchWidget> toggles = new ArrayList<>();

    private int rowHeight = PREFERRED_ROW_HEIGHT;
    private int viewportTop;
    private int viewportHeight;
    private int contentHeight;
    private int scrollOffset;

    @Override
    public String titleKey() {
        return "moreswitchtracker.tab.switches";
    }

    @Override
    public String hintKey() {
        return "moreswitchtracker.hint.switches";
    }

    @Override
    public void init(SettingsScreen host) {
        this.host = host;
        this.switchIds.clear();
        for (SwitchDefinition definition : SwitchIds.all()) {
            this.switchIds.add(definition.id());
        }

        this.viewportTop = host.contentTop();
        this.viewportHeight = Math.max(0, host.contentBottom() - this.viewportTop);
        layoutRows();
        clampScroll();

        this.toggles.clear();
        int toggleX = host.guiWidth() - PADDING_X - ToggleSwitchWidget.PREFERRED_WIDTH;
        int toggleHeight = Math.max(10,
                Math.min(ToggleSwitchWidget.PREFERRED_HEIGHT, this.rowHeight - 4));
        for (String switchId : this.switchIds) {
            ToggleSwitchWidget toggle = new ToggleSwitchWidget(
                    toggleX,
                    rowScreenY(this.toggles.size()) + (this.rowHeight - toggleHeight) / 2,
                    ToggleSwitchWidget.PREFERRED_WIDTH,
                    toggleHeight,
                    Component.literal(ModLang.switchName(switchId, false)),
                    () -> ClientFeatures.isEnabled(switchId),
                    value -> ClientFeatures.setEnabled(switchId, value));
            this.toggles.add(toggle);
            host.addTabWidget(toggle);
        }
        positionToggles();
    }

    private void layoutRows() {
        int rowCount = Math.max(1, this.switchIds.size());
        this.rowHeight = PREFERRED_ROW_HEIGHT;
        if (this.rowHeight * rowCount > this.viewportHeight && this.viewportHeight > 0) {
            int shared = Math.max(MIN_ROW_HEIGHT, this.viewportHeight / rowCount);
            this.rowHeight = shared * rowCount <= this.viewportHeight ? shared : MIN_ROW_HEIGHT;
        }
        this.contentHeight = this.rowHeight * rowCount;
    }

    private boolean canScroll() {
        return this.contentHeight > this.viewportHeight;
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - this.viewportHeight);
    }

    private void clampScroll() {
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll()));
    }

    private int rowScreenY(int index) {
        return this.viewportTop + index * this.rowHeight - this.scrollOffset;
    }

    private void positionToggles() {
        int viewportBottom = this.viewportTop + this.viewportHeight;
        for (int index = 0; index < this.toggles.size(); index++) {
            ToggleSwitchWidget toggle = this.toggles.get(index);
            int rowY = rowScreenY(index);
            int toggleY = rowY + (this.rowHeight - toggle.getHeight()) / 2;
            toggle.setY(toggleY);

            boolean fullyInside = rowY >= this.viewportTop
                    && rowY + this.rowHeight <= viewportBottom;
            toggle.visible = fullyInside;
            toggle.active = fullyInside;
        }
    }

    private void scrollBy(int pixels) {
        if (!canScroll()) {
            return;
        }
        int before = this.scrollOffset;
        this.scrollOffset += pixels;
        clampScroll();
        if (before != this.scrollOffset) {
            positionToggles();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!canScroll() || mouseY < this.viewportTop
                || mouseY >= this.viewportTop + this.viewportHeight) {
            return false;
        }
        scrollBy((int) (-scrollY * LINE_STEP));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int barX = scrollbarX();
        int top = this.viewportTop + SCROLL_ARROW_HEIGHT;
        int bottom = this.viewportTop + this.viewportHeight - SCROLL_ARROW_HEIGHT;
        if (!canScroll() || mouseX < barX - SCROLLBAR_GAP
                || mouseX > barX + SCROLLBAR_WIDTH + SCROLLBAR_GAP) {
            return false;
        }
        if (mouseY >= this.viewportTop && mouseY < this.viewportTop + SCROLL_ARROW_HEIGHT) {
            scrollBy(-LINE_STEP);
            return true;
        }
        if (mouseY >= this.viewportTop + this.viewportHeight - SCROLL_ARROW_HEIGHT
                && mouseY < this.viewportTop + this.viewportHeight) {
            scrollBy(LINE_STEP);
            return true;
        }
        if (mouseY >= top && mouseY < bottom) {
            int span = Math.max(1, bottom - top);
            this.scrollOffset = (int) ((mouseY - top) * maxScroll() / span);
            clampScroll();
            positionToggles();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawRows(graphics);
        if (canScroll()) {
            drawScrollbar(graphics);
        }
    }

    private int scrollbarX() {
        return this.host.guiWidth() - PADDING_X + SCROLLBAR_GAP;
    }

    private void drawRows(GuiGraphics graphics) {
        if (this.viewportHeight <= 0) {
            return;
        }
        int labelX = PADDING_X;
        int labelRight = this.host.guiWidth() - PADDING_X
                - ToggleSwitchWidget.PREFERRED_WIDTH - LABEL_GAP;
        if (labelRight <= labelX) {
            return;
        }

        graphics.enableScissor(labelX, this.viewportTop, labelRight,
                this.viewportTop + this.viewportHeight);
        try {
            int viewportBottom = this.viewportTop + this.viewportHeight;
            for (int index = 0; index < this.switchIds.size(); index++) {
                int rowY = rowScreenY(index);
                int visible = Math.min(rowY + this.rowHeight, viewportBottom)
                        - Math.max(rowY, this.viewportTop);
                if (visible < this.rowHeight / 2) {
                    continue;
                }
                drawRowText(graphics, this.switchIds.get(index), labelX,
                        rowY + rowContentOffsetY());

                if (index < this.switchIds.size() - 1) {
                    int rowDividerY = rowY + this.rowHeight - 1;
                    graphics.fill(PADDING_X, rowDividerY, this.host.guiWidth() - PADDING_X,
                            rowDividerY + 1, DIVIDER_COLOR);
                }
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int x = scrollbarX();
        int top = this.viewportTop + SCROLL_ARROW_HEIGHT;
        int bottom = this.viewportTop + this.viewportHeight - SCROLL_ARROW_HEIGHT;
        int trackHeight = Math.max(1, bottom - top);

        graphics.fill(x, top, x + SCROLLBAR_WIDTH, bottom, SCROLLBAR_TRACK);

        int thumbHeight = Math.max(8,
                trackHeight * this.viewportHeight / Math.max(1, this.contentHeight));
        int travel = Math.max(0, trackHeight - thumbHeight);
        int maximum = maxScroll();
        int thumbY = top + (maximum == 0 ? 0 : travel * this.scrollOffset / maximum);
        graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_THUMB);

        drawArrow(graphics, x, this.viewportTop, true);
        drawArrow(graphics, x, this.viewportTop + this.viewportHeight - SCROLL_ARROW_HEIGHT,
                false);
    }

    private void drawArrow(GuiGraphics graphics, int x, int y, boolean up) {
        int centre = x + SCROLLBAR_WIDTH / 2;
        int rows = SCROLL_ARROW_HEIGHT / 2 + 1;
        for (int row = 0; row < rows; row++) {
            int halfWidth = Math.min(row, SCROLLBAR_WIDTH / 2);
            int rowY = up ? y + SCROLL_ARROW_HEIGHT / 2 - row : y + SCROLL_ARROW_HEIGHT / 2 + row;
            graphics.fill(centre - halfWidth, rowY, centre + halfWidth + 1, rowY + 1,
                    SCROLLBAR_ARROW);
        }
    }

    private int lineBudget() {
        return Math.max(1, this.rowHeight / LINE_STEP);
    }

    private int rowContentOffsetY() {
        int block = Math.min(lineBudget(), 3) * LINE_STEP;
        return Math.max(1, (this.rowHeight - block) / 2);
    }

    private void drawRowText(GuiGraphics graphics, String switchId, int x, int y) {
        int budget = lineBudget();
        int maxWidth = this.host.guiWidth() - PADDING_X
                - ToggleSwitchWidget.PREFERRED_WIDTH - LABEL_GAP - x;
        if (maxWidth <= 0) {
            return;
        }

        String name = ModLang.switchName(switchId, false);
        String description = ModLang.switchDescription(switchId, false);
        if (SwitchIds.FIREWORK_COOLDOWN.equals(switchId)) {
            description = description + statusSuffix();
        }

        if (budget >= 3) {
            graphics.drawString(this.host.guiFont(), fit(name, maxWidth), x, y,
                    TEXT_COLOR_PRIMARY);
            List<FormattedCharSequence> lines = this.host.guiFont()
                    .split(Component.literal(description), maxWidth);
            int room = Math.min(budget - 1, 2);
            for (int line = 0; line < Math.min(room, lines.size()); line++) {
                graphics.drawString(this.host.guiFont(), lines.get(line),
                        x, y + LINE_STEP * (1 + line), TEXT_COLOR_HINT);
            }
            return;
        }
        if (budget == 2) {
            graphics.drawString(this.host.guiFont(), fit(name, maxWidth), x, y,
                    TEXT_COLOR_PRIMARY);
            graphics.drawString(this.host.guiFont(), fit(description, maxWidth),
                    x, y + LINE_STEP, TEXT_COLOR_HINT);
            return;
        }
        graphics.drawString(this.host.guiFont(), fit(name, maxWidth), x, y, TEXT_COLOR_PRIMARY);
    }

    private String statusSuffix() {
        int seconds = FireworkCooldownModule.remainingSeconds();
        if (seconds <= 0) {
            return "";
        }
        return " " + ModLang.text("moreswitchtracker.toast.firework_cooldown")
                .replace("%s", Integer.toString(seconds));
    }

    private String fit(String text, int maxWidth) {
        if (this.host.guiFont().width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int budget = maxWidth - this.host.guiFont().width(ellipsis);
        if (budget <= 0) {
            return "";
        }
        return this.host.guiFont().plainSubstrByWidth(text, budget) + ellipsis;
    }
}
