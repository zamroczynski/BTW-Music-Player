package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.MusicPlayerState;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerControllerMP.class)
public class PlayerControllerMPMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(EntityPlayer player, Entity target, CallbackInfo ci) {
        boolean shouldTriggerCombat = false;
        String targetType = "Unknown";

        if (target instanceof IMob) {
            shouldTriggerCombat = true;
            targetType = "Hostile Mob";
        }
        else if (target instanceof EntityPlayer) {
            shouldTriggerCombat = true;
            targetType = "Player";
        }
        else if (target instanceof EntityWolf) {
            shouldTriggerCombat = true;
            targetType = "Wolf";
        }
        else if (target instanceof EntitySquid) {
            shouldTriggerCombat = true;
            targetType = "Squid";
        }

        if (shouldTriggerCombat) {
            System.out.println("[Music Player Combat Trigger] Gracz zaatakował cel typu: " + targetType + ". Uruchamiam muzykę walki.");
            MusicPlayerState.setPlayerAttackedByMob();
        }
    }
}