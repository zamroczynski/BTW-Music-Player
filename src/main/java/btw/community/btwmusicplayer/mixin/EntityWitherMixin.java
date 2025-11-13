package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.MusicPlayerState;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityWither;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityWither.class)
public class EntityWitherMixin {
    @Inject(method = "dropFewItems", at = @At("HEAD"))
    private void onWitherDropsItems(boolean wasKilledByPlayer, int lootingLevel, CallbackInfo ci) {
        MusicPlayerState.reportBossDefeated("wither");
    }
}