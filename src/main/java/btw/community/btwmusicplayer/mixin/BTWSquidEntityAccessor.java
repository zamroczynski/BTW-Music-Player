package btw.community.btwmusicplayer.mixin;

import btw.entity.mob.BTWSquidEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BTWSquidEntity.class)
public interface BTWSquidEntityAccessor {
    @Accessor("tentacleAttackInProgressCounter")
    int getTentacleAttackInProgressCounter();
}