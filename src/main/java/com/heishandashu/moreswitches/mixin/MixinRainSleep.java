package com.heishandashu.moreswitches.mixin;

import com.heishandashu.moreswitches.sleep.RainSleepRule;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
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
    private void moreswitches$allowRainSleep(BlockPos pos,
                                             CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }
        if (!RainSleepRule.allowsSleep(player.level())) {
            return;
        }
        if (player.level().isDay()) {
            // 12500 ticks is mid-evening: vanilla then finds it dark enough and runs its own
            // wake-up, giving the same "slept until morning" outcome a night's sleep produces.
            ((net.minecraft.server.level.ServerLevel) player.level())
                    .setDayTime(player.level().getDayTime() + 12_500L);
        }
    }
}
