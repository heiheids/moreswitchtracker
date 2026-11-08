package com.heishandashu.moreswitches.client.ui;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.i18n.ModLang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The settings screen: a title row, three tabs and a Done button.
 *
 * <p>Layout, in GUI units:</p>
 * <pre>
 * [ 更多开关 ] [ 追踪器 ]                         [ 语言：自动 ]
 * ----------------------------------------------------------------
 *                  active tab's content
 * ----------------------------------------------------------------
 *                       hint line
 *                       [ 完成 ]
 * </pre>
 *
 * <p>The top bar is the only shared chrome. Each tab owns its own list, controls, scrolling and
 * spacing; the tab bar is a navigation frame, not a redesign of either page.</p>
 *
 * <p>The screen does not pause the game, so a single-player world keeps ticking while the player
 * flips switches.</p>
 */
public final class SettingsScreen extends Screen {

    private static final int PADDING_X = 20;
    private static final int TAB_TOP = 10;
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_GAP = 4;
    private static final int LANGUAGE_GAP = 12;
    private static final int HEADER_BOTTOM = 44;
    private static final int FOOTER_HEIGHT = 44;
    private static final int DONE_WIDTH = 200;
    private static final int DONE_HEIGHT = 20;
    private static final int PREFERRED_TAB_WIDTH = 120;
    private static final int MIN_TAB_WIDTH = 36;
    private static final int PREFERRED_LANGUAGE_WIDTH = 112;
    private static final int MIN_LANGUAGE_WIDTH = 80;

    private static final int TEXT_COLOR_HINT = 0xFF909090;
    private static final int TAB_ACTIVE_COLOR = 0xFFFFFFFF;
    private static final int DIVIDER_COLOR = 0x30FFFFFF;

    /** Property values {@code moreswitchtracker.debugTab} accepts, in tab order. */
    private static final String[] DEBUG_TAB_KEYS = {"switches", "tracker", "display", "health"};

    private final Screen parent;
    private final SettingsTab[] tabs = {
            new SwitchesSettingsTab(),
            new TrackerSettingsTab(),
            new FreeGuiScaleTab(),
            new HealthBarTab()
    };

    private int activeIndex = initialTab();
    private int contentTop = HEADER_BOTTOM;
    private int contentBottom;
    private int tabWidth = PREFERRED_TAB_WIDTH;
    private int languageWidth = PREFERRED_LANGUAGE_WIDTH;

    public SettingsScreen(Screen parent) {
        super(Component.literal(ModLang.text("moreswitchtracker.screen.title")));
        this.parent = parent;
    }

    /** Which page opens first; a JVM property picks another one for verification runs. */
    private static int initialTab() {
        String requested = System.getProperty("moreswitchtracker.debugTab");
        if (requested == null) {
            return 0;
        }
        String trimmed = requested.trim();
        for (int index = 0; index < DEBUG_TAB_KEYS.length; index++) {
            if (DEBUG_TAB_KEYS[index].equalsIgnoreCase(trimmed)) {
                return index;
            }
        }
        return 0;
    }

    @Override
    protected void init() {
        this.contentTop = HEADER_BOTTOM;
        this.contentBottom = Math.max(this.contentTop + 1, this.height - FOOTER_HEIGHT);

        this.languageWidth = Math.max(MIN_LANGUAGE_WIDTH,
                Math.min(PREFERRED_LANGUAGE_WIDTH, Math.max(1, this.width / 4)));
        int tabGaps = TAB_GAP * (this.tabs.length - 1);
        int available = this.width - PADDING_X * 2 - this.languageWidth - LANGUAGE_GAP - tabGaps;
        this.tabWidth = Math.max(MIN_TAB_WIDTH,
                Math.min(PREFERRED_TAB_WIDTH, Math.max(1, available / this.tabs.length)));

        int x = PADDING_X;
        for (int index = 0; index < this.tabs.length; index++) {
            final int target = index;
            Button button = Button.builder(
                            Component.literal(ModLang.text(this.tabs[index].titleKey())),
                            pressed -> selectTab(target))
                    .bounds(x, TAB_TOP, this.tabWidth, TAB_HEIGHT)
                    .build();
            this.addRenderableWidget(button);
            x += this.tabWidth + TAB_GAP;
        }

        addLanguageButton();

        int buttonY = this.height - DONE_HEIGHT - 8;
        this.addRenderableWidget(Button.builder(
                        Component.literal(ModLang.text("moreswitchtracker.screen.done")),
                        button -> this.onClose())
                .bounds(this.width / 2 - DONE_WIDTH / 2, buttonY, DONE_WIDTH, DONE_HEIGHT)
                .build());

        this.tabs[this.activeIndex].init(this);
    }

    /** Switches page without rebuilding the whole shell's persistent state. */
    private void selectTab(int index) {
        if (index == this.activeIndex || index < 0 || index >= this.tabs.length) {
            return;
        }
        this.activeIndex = index;
        this.rebuildWidgets();
    }

    private void addLanguageButton() {
        String full = ModLang.text("moreswitchtracker.screen.language")
                .replace("%s", languageLabel(ModSettings.get().language()));
        Button button = Button.builder(Component.literal(fit(full, this.languageWidth - 8)),
                        pressed -> cycleLanguage())
                .bounds(this.width - PADDING_X - this.languageWidth, TAB_TOP,
                        this.languageWidth, TAB_HEIGHT)
                .tooltip(Tooltip.create(Component.literal(
                        ModLang.text("moreswitchtracker.screen.language.tooltip"))))
                .build();
        this.addRenderableWidget(button);
    }

    private static String languageLabel(String choice) {
        return switch (choice) {
            case ModLang.ZH_CN -> ModLang.text("moreswitchtracker.screen.language.zh_cn");
            case ModLang.EN_US -> ModLang.text("moreswitchtracker.screen.language.en_us");
            default -> ModLang.text("moreswitchtracker.screen.language.auto");
        };
    }

    /** Cycles 自动 → 中文 → English → 自动; only this mod's own text changes. */
    private void cycleLanguage() {
        String next = switch (ModSettings.get().language()) {
            case ModLang.ZH_CN -> ModLang.EN_US;
            case ModLang.EN_US -> ModSettings.LANGUAGE_AUTO;
            default -> ModLang.ZH_CN;
        };
        ModSettings.get().setLanguage(next);
        MoreSwitchesMod.LOGGER.info("[{}] language set to {}", MoreSwitchesMod.MOD_ID, next);
        this.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int activeX = PADDING_X + this.activeIndex * (this.tabWidth + TAB_GAP);
        graphics.fill(activeX, TAB_TOP + TAB_HEIGHT - 2, activeX + this.tabWidth,
                TAB_TOP + TAB_HEIGHT, TAB_ACTIVE_COLOR);
        graphics.fill(PADDING_X, HEADER_BOTTOM - 4, this.width - PADDING_X,
                HEADER_BOTTOM - 3, DIVIDER_COLOR);

        this.tabs[this.activeIndex].render(graphics, mouseX, mouseY, partialTick);

        String hint = ModLang.text(this.tabs[this.activeIndex].hintKey())
                .replace("%s", SettingsHint.keyLabel());
        String plain = hint;
        graphics.drawString(this.font, plain,
                this.width / 2 - this.font.width(plain) / 2, this.height - 40, TEXT_COLOR_HINT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= this.contentTop && mouseY < this.contentBottom
                && this.tabs[this.activeIndex].mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.tabs[this.activeIndex].mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        // The drag has ended by now, so a slider can commit the value it stopped on.
        this.tabs[this.activeIndex].mouseReleased();
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.tabs[this.activeIndex].keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Adds a control owned by the active tab. */
    public <T extends AbstractWidget> T addTabWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    /** Top of the area the tab may draw in, below the shared tab bar. */
    public int contentTop() {
        return this.contentTop;
    }

    /** Bottom of the tab area, above the shared hint and Done button. */
    public int contentBottom() {
        return this.contentBottom;
    }

    public int guiWidth() {
        return this.width;
    }

    public int guiHeight() {
        return this.height;
    }

    public net.minecraft.client.gui.Font guiFont() {
        return this.font;
    }

    /** Shortens text to fit a button label. */
    private String fit(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int budget = maxWidth - this.font.width(ellipsis);
        if (budget <= 0) {
            return "";
        }
        return this.font.plainSubstrByWidth(text, budget) + ellipsis;
    }
}
