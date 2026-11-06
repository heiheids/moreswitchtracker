package com.heishandashu.tracker.module;

/**
 * Stable ids for every feature switch.
 *
 * <p>These strings are the contract between three things, so keep them unchanged once
 * released:</p>
 * <ul>
 *   <li>the {@code features} block in {@code config/tracker.json}</li>
 *   <li>the language file keys, e.g. {@code tracker.module.<id>.name}</li>
 *   <li>the module classes themselves</li>
 * </ul>
 */
public final class ModuleIds {

    /** 遗产追踪 -- death coordinates, dropped-item highlight, countdown, loss alerts. */
    public static final String LEGACY_TRACKING = "legacy_tracking";

    /** 玩家追踪 -- other players. */
    public static final String PLAYER_TRACKING = "player_tracking";

    /** 流浪商人追踪 -- wandering traders, their despawn countdown and a clickable highlight. */
    public static final String WANDERING_TRADER_TRACKING = "wandering_trader_tracking";

    /** 不消失怪物追踪 -- mobs that will never despawn (persistence required / named). */
    public static final String PERSISTENT_MOB_TRACKING = "persistent_mob_tracking";

    /** 实体卡顿源头追踪 -- find which entities / areas are causing tick lag. */
    public static final String LAG_SOURCE_TRACKING = "lag_source_tracking";

    /** 精英怪追踪 -- mobs with far more health than their kind normally has. */
    public static final String ELITE_MOB_TRACKING = "elite_mob_tracking";

    /** 折跃门追踪器 -- marks the nearest end gateway for ten minutes. */
    public static final String END_GATEWAY_TRACKING = "end_gateway_tracking";

    /** 村民兴趣点追踪 -- the bed and workstation a villager claimed, drawn as rays. */
    public static final String VILLAGER_POI_TRACKING = "villager_poi_tracking";

    /** 僵尸仇恨追踪 -- a ray from every zombie-family mob to its target. */
    public static final String ZOMBIE_AGGRO_TRACKING = "zombie_aggro_tracking";

    /** 末影龙仇恨追踪 -- a ray from the dragon to the player it is after. */
    public static final String DRAGON_AGGRO_TRACKING = "dragon_aggro_tracking";

    /** 怪物透视 -- give every hostile mob within 128 blocks the glowing (spectral arrow) outline. */
    public static final String MOB_GLOW_TRACKING = "mob_glow_tracking";

    /** 自定义坐标追踪 -- highlight one coordinate the player types, until switched off. */
    public static final String CUSTOM_COORD_TRACKING = "custom_coord_tracking";

    private ModuleIds() {
    }
}
