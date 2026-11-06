package com.heishandashu.tracker.module.persistent;

import com.heishandashu.tracker.net.PickedMobNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * One mob that picked a dropped item up and therefore will not despawn any more.
 *
 * <p>Names are kept as {@link ItemStack} / {@link Mob} derived values rather than plain strings so
 * they can be sent as translatable components and resolved in the reader's own language.</p>
 */
public final class PickedMobRecord {

    private final int id;
    private final UUID uuid;
    private final Identifier dimension;
    /** Latest known position: refreshed while the mob is loaded. */
    private BlockPos pos;
    /** Snapshot of what it picked up; only names and the count are ever used. */
    private ItemStack stack;
    private final Identifier typeId;
    private final String customName;
    private long lastSeen;

    public PickedMobRecord(int id, Mob mob, ItemStack stack, long gameTime) {
        this.id = id;
        this.uuid = mob.getUUID();
        this.dimension = mob.level().dimension().identifier();
        this.pos = mob.blockPosition();
        this.stack = stack.copy();
        this.typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        this.customName = mob.hasCustomName() ? mob.getName().getString() : "";
        this.lastSeen = gameTime;
    }

    public int id() {
        return this.id;
    }

    /** The display number the player types, e.g. {@code #00007}. */
    public String label() {
        return String.format("#%05d", this.id);
    }

    public UUID uuid() {
        return this.uuid;
    }

    public Identifier dimension() {
        return this.dimension;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public ItemStack stack() {
        return this.stack;
    }

    public Identifier typeId() {
        return this.typeId;
    }

    public String customName() {
        return this.customName;
    }

    public long lastSeen() {
        return this.lastSeen;
    }

    /** Kept while the mob is loaded, so the record always has its latest position. */
    public void refresh(BlockPos newPos, long gameTime) {
        this.lastSeen = gameTime;
        if (newPos != null) {
            this.pos = newPos;
        }
    }

    /** The mob picked up something else: show what it is holding now. */
    public void refreshStack(ItemStack newStack) {
        if (!newStack.isEmpty()) {
            this.stack = newStack.copy();
        }
    }

    public PickedMobNetworking.PickedMobSnap snapshot() {
        return new PickedMobNetworking.PickedMobSnap(
                this.id, this.uuid, this.dimension.toString(),
                this.pos.getX() + 0.5, this.pos.getY(), this.pos.getZ() + 0.5,
                this.typeId.toString(),
                BuiltInRegistries.ITEM.getKey(this.stack.getItem()).toString(),
                this.stack.getCount(), this.customName);
    }
}
