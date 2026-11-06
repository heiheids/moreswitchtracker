package com.heishandashu.moreswitches.client.ui;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.client.gui.FreeGuiScale;
import com.heishandashu.moreswitches.client.gui.GuiScaleApplier;
import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.i18n.ModLang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;

/**
 * The 自由界面 tab: one stepless slider, the vanilla levels, and an exact number.
 *
 * <p>Three ways in, one state out. Dragging the slider or typing a number turns the free scale on;
 * picking a vanilla level turns it off and hands the size back to vanilla's own option, so the
 * video settings page keeps telling the truth after the player leaves this one.</p>
 *
 * <p>Applying a scale resizes the interface, which rebuilds this screen. That is why the slider
 * commits on release rather than on every step: resizing mid-drag would replace the widget the
 * mouse is holding.</p>
 */
final class FreeGuiScaleTab implements SettingsTab {

    private static final int PADDING_X = 24;
    private static final int LABEL_STEP = 10;
    private static final int LABEL_PAD = 2;
    private static final int ROW_HEIGHT = 20;
    private static final int PREFERRED_GAP = 12;
    private static final int MIN_GAP = 4;
    private static final int PRESET_GAP = 4;
    private static final int APPLY_WIDTH = 56;
    private static final int MIN_ROW_WIDTH = 140;

    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_HINT = 0xFF909090;
    private static final int TEXT_ACTIVE = 0xFF7FD66B;
    private static final int TEXT_BAD = 0xFFFF6B6B;
    private static final int TEXT_TYPED = 0xFFE0E0E0;

    /** Vanilla's own levels, in the order the video settings page offers them. */
    private static final int[] VANILLA_LEVELS = {0, 1, 2, 3, 4};

    private SettingsScreen host;
    private GuiScaleSlider slider;
    private EditBox field;

    private int titleY;
    private int sliderY;
    private int presetLabelY;
    private int presetY;
    private int inputLabelY;
    private int inputY;
    private int hintY;

    /** False when the page is too short for the little captions above each row. */
    private boolean showRowLabels;

    /** Set while the widgets are being positioned, so their listeners do not re-enter. */
    private boolean quiet;

    @Override
    public String titleKey() {
        return "moreswitchtracker.tab.display";
    }

    @Override
    public String hintKey() {
        return "moreswitchtracker.hint.display";
    }

    @Override
    public void init(SettingsScreen host) {
        this.host = host;
        this.quiet = true;
        try {
            buildWidgets(host);
        } finally {
            this.quiet = false;
        }
    }

    private void buildWidgets(SettingsScreen host) {
        int left = PADDING_X;
        int right = host.guiWidth() - PADDING_X;
        int width = right - left;
        if (width < MIN_ROW_WIDTH) {
            return;
        }

        // Three rows of controls, optionally preceded by a caption each. The captions are the first
        // thing dropped when the interface is scaled up, because the rows are what the page is for.
        int rowsHeight = ROW_HEIGHT * 3;
        int labelBlock = (LABEL_STEP + LABEL_PAD) * 3;
        int available = host.contentBottom() - host.contentTop() - 8;

        this.showRowLabels = rowsHeight + labelBlock + MIN_GAP * 2 <= available;
        int gaps;
        if (this.showRowLabels) {
            gaps = Math.max(MIN_GAP, (available - rowsHeight - labelBlock) / 2);
        } else {
            gaps = Math.max(MIN_GAP, (available - rowsHeight) / 2);
        }
        gaps = Math.min(gaps, PREFERRED_GAP);

        int y = host.contentTop() + 4;
        this.titleY = this.showRowLabels ? y : -1000;
        if (this.showRowLabels) {
            y += LABEL_STEP + LABEL_PAD;
        }
        this.sliderY = y;
        y += ROW_HEIGHT + gaps;

        this.presetLabelY = this.showRowLabels ? y : -1000;
        if (this.showRowLabels) {
            y += LABEL_STEP + LABEL_PAD;
        }
        this.presetY = y;
        y += ROW_HEIGHT + gaps;

        this.inputLabelY = this.showRowLabels ? y : -1000;
        if (this.showRowLabels) {
            y += LABEL_STEP + LABEL_PAD;
        }
        this.inputY = y;
        y += ROW_HEIGHT;

        this.hintY = y + 8;

        addSlider(left, width);
        addPresets(left, width);
        addManualEntry(left, width);
    }

    private void addSlider(int left, int width) {
        float max = Math.max(FreeGuiScale.MIN_SCALE + FreeGuiScale.STEP,
                FreeGuiScale.maxUsableScale());
        this.slider = new GuiScaleSlider(left, this.sliderY, width, ROW_HEIGHT,
                FreeGuiScale.MIN_SCALE, max, currentScale(), this::sliderLabel);
        this.slider.setTooltip(Tooltip.create(Component.literal(
                ModLang.text("moreswitchtracker.display.slider.tooltip"))));
        this.host.addTabWidget(this.slider);
    }

    private void addPresets(int left, int width) {
        int count = VANILLA_LEVELS.length;
        int buttonWidth = Math.max(24, (width - PRESET_GAP * (count - 1)) / count);
        int x = left;
        for (int level : VANILLA_LEVELS) {
            final int target = level;
            Button button = Button.builder(
                            Component.literal(presetLabel(level)),
                            pressed -> useVanillaLevel(target))
                    .bounds(x, this.presetY, buttonWidth, ROW_HEIGHT)
                    .tooltip(Tooltip.create(Component.literal(level == 0
                            ? ModLang.text("moreswitchtracker.display.preset.auto.tooltip")
                            : ModLang.text("moreswitchtracker.display.preset.tooltip")
                                    .replace("%s", Integer.toString(level)))))
                    .build();
            this.host.addTabWidget(button);
            x += buttonWidth + PRESET_GAP;
        }
    }

    private void addManualEntry(int left, int width) {
        int fieldWidth = Math.max(40, width - APPLY_WIDTH - PRESET_GAP);
        this.field = new EditBox(this.host.guiFont(), left, this.inputY, fieldWidth, ROW_HEIGHT,
                Component.literal(ModLang.text("moreswitchtracker.display.field")));
        this.field.setMaxLength(8);
        this.field.setHint(Component.literal(ModLang.text("moreswitchtracker.display.field.hint")));
        this.field.setValue(formatScale(currentScale()));
        this.field.setResponder(text -> {
            if (!this.quiet) {
                this.field.setTextColor(isParsable(text) ? TEXT_TYPED : TEXT_BAD);
            }
        });
        this.host.addTabWidget(this.field);

        Button apply = Button.builder(
                        Component.literal(ModLang.text("moreswitchtracker.display.apply")),
                        pressed -> applyTypedValue())
                .bounds(left + fieldWidth + PRESET_GAP, this.inputY, APPLY_WIDTH, ROW_HEIGHT)
                .build();
        this.host.addTabWidget(apply);
    }

    /** Reads the field and applies it; also what Enter in the field does. */
    private void applyTypedValue() {
        if (this.field == null) {
            return;
        }
        String text = this.field.getValue().trim().replace(',', '.');
        float parsed;
        try {
            parsed = Float.parseFloat(text);
        } catch (NumberFormatException e) {
            MoreSwitchesMod.LOGGER.info("[{}] not a GUI scale: '{}'", MoreSwitchesMod.MOD_ID, text);
            this.field.setTextColor(TEXT_BAD);
            return;
        }
        if (!Float.isFinite(parsed) || parsed <= 0.0F) {
            this.field.setTextColor(TEXT_BAD);
            return;
        }
        applyFreeScale(parsed);
    }

    /**
     * Vanilla level 0 means "hand sizing back to vanilla"; 1-4 pin that exact size.
     *
     * <p>The vanilla option is deliberately not written here. Changing it fires its own resize
     * callback, and doing that from inside a button press rebuilt this screen twice in a row, once
     * from the callback and once from the explicit resize. Pinning the free scale to the same
     * integer renders identically and touches nothing else.</p>
     */
    private void useVanillaLevel(int level) {
        if (this.quiet) {
            return;
        }
        if (level <= 0) {
            FreeGuiScale.disable();
            ModSettings.get().setGuiScale(0.0F);
            GuiScaleApplier.requestApply();
            MoreSwitchesMod.LOGGER.info("[{}] GUI scale: back to vanilla", MoreSwitchesMod.MOD_ID);
            return;
        }
        applyFreeScale(level);
    }

    /**
     * Records a free scale and asks for it to be applied on the next tick.
     *
     * <p>Applying it here would rebuild this screen from inside the control the player is using,
     * which is the one thing that must not happen mid-interaction.</p>
     */
    private void applyFreeScale(float requested) {
        if (this.quiet) {
            return;
        }
        float snapped = FreeGuiScale.snap(requested);
        float max = FreeGuiScale.maxUsableScale();
        float effective = Math.min(snapped, max);
        FreeGuiScale.setScale(snapped);
        ModSettings.get().setGuiScale(FreeGuiScale.requested());
        GuiScaleApplier.requestApply();
        MoreSwitchesMod.LOGGER.info("[{}] GUI scale: free {}x (applied {}x, window allows up to {}x)",
                MoreSwitchesMod.MOD_ID, formatScale(snapped), formatScale(effective),
                formatScale(max));
    }

    private Component sliderLabel(float scale) {
        String caption = FreeGuiScale.active()
                ? ModLang.text("moreswitchtracker.display.slider.value")
                : ModLang.text("moreswitchtracker.display.slider.vanilla");
        return Component.literal(caption.replace("%s", formatScale(scale)));
    }

    private static String presetLabel(int level) {
        return level == 0
                ? ModLang.text("moreswitchtracker.display.preset.auto")
                : ModLang.text("moreswitchtracker.display.preset.level")
                        .replace("%s", Integer.toString(level));
    }

    /** The scale actually in force, whether it came from this page or from vanilla. */
    private static float currentScale() {
        if (FreeGuiScale.active()) {
            return FreeGuiScale.scale();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return 1.0F;
        }
        return (float) minecraft.getWindow().getGuiScale();
    }

    private static String formatScale(float scale) {
        return String.format(Locale.ROOT, "%.2f", scale);
    }

    private static boolean isParsable(String text) {
        try {
            float value = Float.parseFloat(text.trim().replace(',', '.'));
            return Float.isFinite(value) && value > 0.0F;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.slider == null) {
            return;
        }
        int left = PADDING_X;
        int right = this.host.guiWidth() - PADDING_X;

        if (this.showRowLabels) {
            graphics.drawString(this.host.guiFont(),
                    ModLang.text("moreswitchtracker.display.slider.label"),
                    left, this.titleY, TEXT_PRIMARY, false);
            graphics.drawString(this.host.guiFont(),
                    ModLang.text("moreswitchtracker.display.presets.label"),
                    left, this.presetLabelY, TEXT_HINT, false);
            graphics.drawString(this.host.guiFont(),
                    ModLang.text("moreswitchtracker.display.field.label"),
                    left, this.inputLabelY, TEXT_HINT, false);
        }

        drawHint(graphics, left, right);
    }

    private void drawHint(GuiGraphics graphics, int left, int right) {
        if (this.hintY + LABEL_STEP > this.host.contentBottom()) {
            return;
        }
        int maxWidth = right - left;
        String status = ModLang.text(FreeGuiScale.active()
                        ? "moreswitchtracker.display.status.free"
                        : "moreswitchtracker.display.status.vanilla")
                .replace("%s", formatScale(currentScale()));
        graphics.drawString(this.host.guiFont(), fit(status, maxWidth), left, this.hintY,
                TEXT_ACTIVE, false);

        String note = ModLang.text("moreswitchtracker.display.note")
                .replace("%s", formatScale(FreeGuiScale.maxUsableScale()));
        List<FormattedCharSequence> lines = this.host.guiFont()
                .split(Component.literal(note), Math.max(1, maxWidth));
        int y = this.hintY + LABEL_STEP + 2;
        for (FormattedCharSequence line : lines) {
            if (y + LABEL_STEP > this.host.contentBottom()) {
                break;
            }
            graphics.drawString(this.host.guiFont(), line, left, y, TEXT_HINT, false);
            y += LABEL_STEP;
        }
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.field != null && this.field.isFocused()
                && (keyCode == 257 || keyCode == 335)) {
            applyTypedValue();
            return true;
        }
        return false;
    }

    /** Called by the shell after every mouse release, so a finished drag is committed once. */
    @Override
    public void mouseReleased() {
        if (this.quiet || this.slider == null || !this.slider.consumeRelease()) {
            return;
        }
        applyFreeScale(this.slider.scale());
    }
}
