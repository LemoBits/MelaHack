package thunder.hack.features.modules.combat;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import thunder.hack.features.modules.base.TrapModule;

public final class SelfTrap extends TrapModule {
    public SelfTrap() {
        super("SelfTrap", Category.COMBAT);
    }

    @Override
    protected boolean needNewTarget() {
        return target == null;
    }

    @Override
    protected @Nullable Player getTarget() {
        return mc.player;
    }
}
