package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.quiet.QuietRules;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.PackCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the resource-pack list treat a version-mismatched pack as compatible.
 *
 * <p>This is where the warning actually comes from. The list asks its entry for a
 * {@code PackCompatibility} and then branches on the value — showing the "(made for an older
 * version)" marker, or raising the confirmation dialog — without going through
 * {@code PackCompatibility.isCompatible()}. Patching only that method therefore left the warning
 * on screen, which is exactly what happened on the first attempt.</p>
 *
 * <p>Returning {@code COMPATIBLE} here covers every branch at once: no marker, no dialog, and the
 * pack is enabled immediately. Nothing about how the pack loads changes.</p>
 */
@Mixin(targets = "net.minecraft.client.gui.screens.packs.PackSelectionModel$EntryBase")
public class MixinPackSelectionEntry {

    @Inject(method = "getCompatibility", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$reportCompatible(CallbackInfoReturnable<PackCompatibility> cir) {
        if (QuietRules.skipResourcePackWarning()) {
            cir.setReturnValue(PackCompatibility.COMPATIBLE);
        }
    }
}
