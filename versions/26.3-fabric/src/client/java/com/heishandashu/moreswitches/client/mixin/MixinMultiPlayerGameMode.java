package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.client.feature.ClientFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops the firework-rocket use before it becomes a packet.
 *
 * <p>Both right-click paths are covered, because a rocket is used by both:</p>
 * <ul>
 *   <li>{@code useItem} — right-clicking into the air while gliding, which boosts the player;</li>
 *   <li>{@code useItemOn} — right-clicking a block, which places a rocket.</li>
 * </ul>
 *
 * <p>Each is cancelled at <code>HEAD</code>, before the click becomes a packet, so no rocket is
 * spawned client-side and none is requested from the server. Injecting at {@code RETURN} would be
 * too late: the packet is already sent inside the method.</p>
 *
 * <p>The drop-key switch is <b>not</b> here. On 1.21.1 the drop key calls
 * {@code LocalPlayer.drop(boolean)}; {@code MultiPlayerGameMode} has no {@code dropItem} method at
 * all, which is why an earlier version of this class crashed the game on world join once Mixin tried
 * to apply an injection that had no target.</p>
 *
 * <p>Handler parameter types match the target methods exactly ({@code useItem} takes
 * {@link Player}, {@code useItemOn} takes {@link LocalPlayer}); Mixin validates this when it applies
 * the injection. {@code require = 1} makes a failed application a startup crash instead of a switch
 * that silently does nothing.</p>
 *
 * <p>This class holds no decision logic; {@link ClientFeatures} decides.</p>
 */
@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$blockFireworkUse(Player player, InteractionHand hand,
                                               CallbackInfoReturnable<InteractionResult> cir) {
        if (ClientFeatures.blocksItemUse(Minecraft.getInstance(), player, hand)) {
            // FAIL consumes the action without reporting success: the click is eaten and nothing
            // else in vanilla reacts to it.
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$blockFireworkPlacement(LocalPlayer player, InteractionHand hand,
                                                     BlockHitResult hit,
                                                     CallbackInfoReturnable<InteractionResult> cir) {
        if (ClientFeatures.blocksItemUse(Minecraft.getInstance(), player, hand)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
