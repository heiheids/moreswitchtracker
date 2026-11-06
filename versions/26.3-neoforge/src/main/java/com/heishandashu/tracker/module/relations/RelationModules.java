package com.heishandashu.tracker.module.relations;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.RelationNetworking;

/**
 * The three ray features (村民兴趣点 / 僵尸仇恨 / 末影龙仇恨).
 *
 * <p>They are separate switches because the player asked for them separately, but they are the same
 * mechanism -- a line from a mob to what it is tied to -- so they share one collector
 * ({@link RelationCollector}) and one payload ({@link RelationNetworking}). Keeping them in one
 * small file makes that shared nature obvious instead of hiding it behind three near-identical
 * files.</p>
 */
public final class RelationModules {

    private RelationModules() {
    }

    /** 村民兴趣点追踪 -- the bed and workstation a villager has claimed. */
    public static final class VillagerPoi implements TrackerModule {

        @Override
        public String id() {
            return ModuleIds.VILLAGER_POI_TRACKING;
        }

        @Override
        public boolean defaultEnabled() {
            return false;
        }

        @Override
        public void registerCommon() {
            RelationNetworking.registerPayload();
        }

        @Override
        public void registerServer() {
            RelationCollector.init();
        }
    }

    /** 僵尸仇恨追踪 -- every zombie-family mob's attack target. */
    public static final class ZombieAggro implements TrackerModule {

        @Override
        public String id() {
            return ModuleIds.ZOMBIE_AGGRO_TRACKING;
        }

        @Override
        public boolean defaultEnabled() {
            return false;
        }

        @Override
        public void registerCommon() {
            RelationNetworking.registerPayload();
        }

        @Override
        public void registerServer() {
            RelationCollector.init();
        }
    }

    /** 末影龙仇恨追踪 -- the player the dragon is after. */
    public static final class DragonAggro implements TrackerModule {

        @Override
        public String id() {
            return ModuleIds.DRAGON_AGGRO_TRACKING;
        }

        @Override
        public boolean defaultEnabled() {
            return false;
        }

        @Override
        public void registerCommon() {
            RelationNetworking.registerPayload();
        }

        @Override
        public void registerServer() {
            RelationCollector.init();
        }
    }

    public static boolean villagerPoiActive() {
        return TrackerModules.isEnabled(ModuleIds.VILLAGER_POI_TRACKING);
    }

    public static boolean zombieAggroActive() {
        return TrackerModules.isEnabled(ModuleIds.ZOMBIE_AGGRO_TRACKING);
    }

    public static boolean dragonAggroActive() {
        return TrackerModules.isEnabled(ModuleIds.DRAGON_AGGRO_TRACKING);
    }
}
