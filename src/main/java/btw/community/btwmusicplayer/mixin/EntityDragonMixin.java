package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.MusicPlayerState;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityDragon.class)
public class EntityDragonMixin {
    @Inject(method = "onDeathUpdate", at = @At("HEAD"))
    private void onDragonDeathUpdate(CallbackInfo ci) {
        MusicPlayerState.reportBossDefeated("ender_dragon");
    }
}