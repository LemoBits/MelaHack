package thunder.hack.utility.interfaces;

import thunder.hack.features.modules.render.Trails;

import java.util.List;
import net.minecraft.core.BlockPos;

public interface IEntity {
    List<Trails.Trail> getTrails();

    BlockPos thunderHack_Recode$getVelocityBP();
}
