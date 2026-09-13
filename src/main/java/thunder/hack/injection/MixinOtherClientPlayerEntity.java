package thunder.hack.injection;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.modules.misc.FakePlayer;
import thunder.hack.utility.interfaces.IEntityLiving;
import thunder.hack.utility.interfaces.IOtherClientPlayerEntity;

import static thunder.hack.features.modules.Module.mc;

@Mixin(RemotePlayer.class)
public class MixinOtherClientPlayerEntity extends AbstractClientPlayer implements IOtherClientPlayerEntity {
    @Unique private double backUpX, backUpY, backUpZ;

    public MixinOtherClientPlayerEntity(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    public void resolve(Aura.Resolver mode) {
        if ((Object) this == FakePlayer.fakePlayer) {
            backUpY = -999;
            return;
        }

        backUpX = getX();
        backUpY = getY();
        backUpZ = getZ();

        if(mode == Aura.Resolver.BackTrack) {
            double minDst = 999d;
            Aura.Position bestPos = null;
            for (Aura.Position p : ((IEntityLiving) this).getPositionHistory()) {
                double dst = mc.player.distanceToSqr(p.getX(), p.getY(), p.getZ());
                if (dst < minDst) {
                    minDst = dst;
                    bestPos = p;
                }
            }
            if(bestPos != null) {
                setPos(bestPos.getX(), bestPos.getY(), bestPos.getZ());
                if(Aura.target == this)
                    ModuleManager.aura.resolvedBox = getBoundingBox();
            }
            return;
        }

        Vec3 from = new Vec3(((IEntityLiving) this).getPrevServerX(), ((IEntityLiving) this).getPrevServerY(), ((IEntityLiving) this).getPrevServerZ());
        Vec3 to = new Vec3(getX(), getY(), getZ());

        if(mode == Aura.Resolver.Advantage) {
            if (mc.player.distanceToSqr(from) > mc.player.distanceToSqr(to)) setPos(to.x, to.y, to.z);
            else setPos(from.x, from.y, from.z);
        } else {
            setPos(to.x, to.y, to.z);
        }
        if(Aura.target == this)
            ModuleManager.aura.resolvedBox = getBoundingBox();
    }

    public void releaseResolver() {
        if (backUpY != -999) {
            setPos(backUpX, backUpY, backUpZ);
            backUpY = -999;
        }
    }
}
