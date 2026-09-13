package thunder.hack.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class NoBob extends Module {
    public NoBob() {
        super("NoBob", Category.RENDER);
    }

    public static Setting<Mode> mode = new Setting<>("Mode", Mode.Sexy);

    public void bobView(PoseStack matrices, float tickDelta) {
        if (!(mc.getCameraEntity() instanceof Player))
            return;

        float g = -(float) mc.player.getDeltaMovement().horizontalDistance();
        float h = Mth.lerp(tickDelta, ((thunder.hack.injection.accesors.IPlayerEntity) mc.player).getLastStrideDistance(), mc.player.bob);
        matrices.translate(0, -Math.abs(g * h * (mode.is(Mode.Sexy) ? 0.00035 : 0.)), 0);
    }

    public enum Mode {
        Sexy,
        Off
    }
}
