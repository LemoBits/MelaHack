package thunder.hack.features.modules.player;

import com.mojang.blaze3d.vertex.PoseStack;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class ViewLock extends Module {
    public ViewLock() {
        super("ViewLock", Category.PLAYER);
    }

    private final Setting<Boolean> lockCurrent = new Setting<>("LockCurrent", true);
    public final Setting<Boolean> pitch = new Setting<>("Pitch", true);
    public final Setting<Float> pitchValue = new Setting<>("PitchValue", 0f, -90f, 90f, v -> pitch.getValue());
    public final Setting<Boolean> yaw = new Setting<>("Yaw", true);
    public final Setting<Float> yawValue = new Setting<>("YawValue", 0f, -180f, 180f, v -> yaw.getValue());

    @Override
    public void onEnable() {
        if(lockCurrent.getValue()) {
            yawValue.setValue(mc.player.getYRot());
            pitchValue.setValue(mc.player.getXRot());
        }
    }

    public void onRender3D(PoseStack m) {
        if (pitch.getValue()) mc.player.setXRot(pitchValue.getValue());
        if (yaw.getValue()) mc.player.setYRot(yawValue.getValue());
    }
}
