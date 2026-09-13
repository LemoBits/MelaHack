package thunder.hack.features.modules.movement;

import com.mojang.blaze3d.vertex.PoseStack;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class Parkour extends Module {
    public Parkour() {
        super("Parkour", Category.MOVEMENT);
    }

    private final Setting<Float> jumpFactor = new Setting<>("JumpFactor", 0.01f, 0.001f, 0.3f);

    private final thunder.hack.utility.Timer delay = new thunder.hack.utility.Timer();

    public void onRender3D(PoseStack stack) {
        if (mc.player.onGround()
                && !mc.options.keyJump.isDown()
                && !mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().inflate(-jumpFactor.getValue(), 0, -jumpFactor.getValue()).move(0, -0.99, 0)).iterator().hasNext()
                && delay.every(150))
            mc.player.jumpFromGround();
    }
}
