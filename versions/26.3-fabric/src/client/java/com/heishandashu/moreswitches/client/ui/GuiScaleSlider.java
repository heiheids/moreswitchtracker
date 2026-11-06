package com.heishandashu.moreswitches.client.ui;

import com.heishandashu.moreswitches.client.gui.FreeGuiScale;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

/**
 * The stepless GUI-scale slider on the 自由界面 page.
 *
 * <p>Vanilla's own control is a cycle button with a handful of integers. This one is a real slider
 * over the whole range, in {@link FreeGuiScale#STEP} increments, so 2.35 or 2.75 are reachable.</p>
 *
 * <p>The slider only reports the value it is pointing at; it does not resize anything itself. The
 * page applies the change once the drag ends, because a resize rebuilds the screen and would
 * otherwise pull the widget out from under the mouse.</p>
 */
public final class GuiScaleSlider extends AbstractSliderButton {

    /** Turns a scale into the caption drawn inside the slider. */
    public interface Labeller {
        Component label(float scale);
    }

    private final float min;
    private final float max;
    private final Labeller labeller;

    /**
     * Whether the handle is being held.
     *
     * <p>Tracked here rather than read from the superclass: 26.3 keeps its own flag private, and the
     * page needs to know when a drag has finished so it can commit the value once.</p>
     */
    private boolean held;

    /** Set by a release and cleared by {@link #consumeRelease()}, so one drag commits once. */
    private boolean released;

    public GuiScaleSlider(int x, int y, int width, int height, float min, float max,
                          float initial, Labeller labeller) {
        super(x, y, width, height, Component.empty(), normalise(initial, min, max));
        this.min = min;
        this.max = max;
        this.labeller = labeller;
        updateMessage();
    }

    /** The scale the handle currently points at, snapped to the slider's step. */
    public float scale() {
        float raw = (float) (this.min + (this.max - this.min) * this.value);
        return Math.max(this.min, Math.min(FreeGuiScale.snap(raw), this.max));
    }

    public boolean dragging() {
        return this.held;
    }

    /**
     * True exactly once per release that followed a press on this slider.
     *
     * <p>The page uses this instead of "the mouse went up somewhere", which would also fire for a
     * click on the language button or a tab and would commit a scale nobody asked for.</p>
     */
    public boolean consumeRelease() {
        boolean value = this.released;
        this.released = false;
        return value;
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        this.held = true;
        this.released = false;
        super.onClick(event, doubleClick);
    }

    @Override
    public void onRelease(net.minecraft.client.input.MouseButtonEvent event) {
        this.held = false;
        this.released = true;
        super.onRelease(event);
    }

    /** Moves the handle without treating it as a player edit. */
    public void setScaleQuietly(float scale) {
        this.value = normalise(scale, this.min, this.max);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(this.labeller.label(scale()));
    }

    @Override
    protected void applyValue() {
        // Nothing to do: the page reads scale() and commits when the drag ends.
    }

    private static double normalise(float scale, float min, float max) {
        if (max <= min) {
            return 0.0D;
        }
        double value = (scale - (double) min) / ((double) max - min);
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
