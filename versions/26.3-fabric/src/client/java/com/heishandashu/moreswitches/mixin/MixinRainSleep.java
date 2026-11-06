package com.heishandashu.moreswitches.mixin;

import com.heishandashu.moreswitches.sleep.RainSleepRule;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a bed be used during rain, even in daylight.
 *
 * <p>Vanilla refuses because it is day, so with the switch on and rain falling the clock is moved to
 * the point vanilla itself treats as "time to wake up". Everything after that is the untouched
 * vanilla sleep path, which is why the player still gets the normal wake-up and the normal "skip to
 * morning" result rather than a custom teleport.</p>
 *
 * <p>The clock only moves when a bed would otherwise be refused, so a normal night's sleep is never
 * disturbed.</p>
 */
@Mixin(Player.class)
public class MixinRainSleep {

    @Inject(method = "startSleepInBed", at = @At("HEAD"), require = 1)
    private void moreswitches$allowRainSleep(AbstractBedBlock bed, BlockState state,
                                             BedRule bedRule, BlockPos pos,
                                             CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }
        if (!RainSleepRule.allowsSleep(player.level())) {
            return;
        }
        // 26.3 replaced Level#isDay/getDayTime with world clocks. 0..12000 is daytime.
        long dayTime = player.level().getOverworldClockTime() % 24000L;
        if (dayTime < 12000L) {
            net.minecraft.server.level.ServerLevel level =
                    (net.minecraft.server.level.ServerLevel) player.level();
            net.minecraft.core.Holder<net.minecraft.world.clock.WorldClock> clock =
                    level.registryAccess()
                            .lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                            .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
            ((net.minecraft.world.clock.ServerClockManager) level.clockManager())
                    .moveToTimeMarker(clock,
                            net.minecraft.world.clock.ClockTimeMarkers.WAKE_UP_FROM_SLEEP);
        }
    }
}
