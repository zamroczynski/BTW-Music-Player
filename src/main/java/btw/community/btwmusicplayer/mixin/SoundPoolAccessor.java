package btw.community.btwmusicplayer.mixin;

import net.minecraft.src.SoundPool;
import net.minecraft.src.SoundPoolEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(SoundPool.class)
public interface SoundPoolAccessor {
    @Accessor("nameToSoundPoolEntriesMapping")
    Map<String, List<SoundPoolEntry>> getSoundMap();
}