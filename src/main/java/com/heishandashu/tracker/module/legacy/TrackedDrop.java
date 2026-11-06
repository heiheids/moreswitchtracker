package com.heishandashu.tracker.module.legacy;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * One dropped item that came out of a death.
 *
 * <p>State is decided by the hooks in {@code com.heishandashu.tracker.mixin}: the item itself
 * never has to be guessed at, because {@code hurtServer}, the pickup paths and
 * {@code Entity#remove} all report the real reason.</p>
 */
public final class TrackedDrop {

    public enum State {
        /** Still lying in the world. */
        ALIVE,
        /** The chunk unloaded: the item is saved, the countdown is frozen. */
        PAUSED,
        /** Destroyed by damage (lava, fire, cactus, explosion, ...). */
        DESTROYED,
        /** Picked up by a mob. */
        TAKEN_BY_MOB,
        /** Picked up by a player. */
        TAKEN_BY_PLAYER,
        /** Absorbed by another dropped stack; the items still exist there. */
        MERGED,
        /** Reached the end of its 6000-tick life and vanished. */
        EXPIRED,
        /** Removed for a reason we could not attribute. */
        UNKNOWN
    }

    private final UUID uuid;
    private final ItemStack stack;
    private State state = State.ALIVE;
    /** Last known age in ticks; used for the "did it expire" decision. */
    private int lastAge;
    /** Network entity id, refreshed while the item is visible; -1 when unknown. */
    private int entityId = -1;
    /** Last seen entity instance; lets the countdown read a live age without a lookup. */
    private net.minecraft.world.entity.item.ItemEntity entity;
    /** Who or what was responsible: a damage cause key, or a mob display name. */
    private Component detail;
    /** Makes sure an end-of-life message is only printed once. */
    private boolean reported;

    public TrackedDrop(UUID uuid, ItemStack stack) {
        this.uuid = uuid;
        this.stack = stack.copy();
    }

    public UUID uuid() {
        return this.uuid;
    }

    /** A snapshot of the stack as it was when the death happened. */
    public ItemStack stack() {
        return this.stack;
    }

    public State state() {
        return this.state;
    }

    public void state(State state) {
        this.state = state;
    }

    public int lastAge() {
        return this.lastAge;
    }

    public void lastAge(int age) {
        this.lastAge = age;
    }

    /** Network entity id, refreshed whenever the item is visible; -1 when unknown. */
    public int entityId() {
        return this.entityId;
    }

    public void entityId(int entityId) {
        this.entityId = entityId;
    }

    /** The last entity instance seen for this drop, or {@code null} while it is unloaded. */
    public net.minecraft.world.entity.item.ItemEntity entity() {
        return this.entity;
    }

    public void entity(net.minecraft.world.entity.item.ItemEntity entity) {
        this.entity = entity;
    }

    public Component detail() {
        return this.detail;
    }

    public void detail(Component detail) {
        this.detail = detail;
    }

    public boolean reported() {
        return this.reported;
    }

    public void reported(boolean reported) {
        this.reported = reported;
    }

    public boolean stillInWorld() {
        return this.state == State.ALIVE || this.state == State.PAUSED;
    }
}
