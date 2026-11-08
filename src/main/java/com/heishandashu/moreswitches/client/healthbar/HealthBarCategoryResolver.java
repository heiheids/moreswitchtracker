package com.heishandashu.moreswitches.client.healthbar;

import com.heishandashu.moreswitches.config.HealthBarCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Sorts a creature into one of the four 血条 categories.
 *
 * <p>Registry ids are used for the two groups that are awkward to express as a type: every golem
 * shares an id suffix but not a superclass, and a zombie villager must stay a monster rather than
 * being swept into "villagers".</p>
 */
public final class HealthBarCategoryResolver {

    private HealthBarCategoryResolver() {
    }

    /** The category this creature belongs to, or null when it is not tracked at all. */
    public static HealthBarCategory of(LivingEntity entity) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String path = key == null ? "" : key.getPath();

        // Golems first: they are neither hostile nor animals, and the id is the only common trait.
        if (path.contains("golem")) {
            return HealthBarCategory.GOLEM;
        }
        // Hostility before "villager": a zombie villager is a monster, not a trader.
        if (entity instanceof Enemy) {
            return HealthBarCategory.MONSTER;
        }
        if (path.contains("villager") || path.contains("trader")) {
            return HealthBarCategory.VILLAGER;
        }
        if (entity instanceof Animal) {
            return HealthBarCategory.ANIMAL;
        }
        return null;
    }
}
