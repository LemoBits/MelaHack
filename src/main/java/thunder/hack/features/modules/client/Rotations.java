package thunder.hack.features.modules.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventFixVelocity;
import thunder.hack.events.impl.EventKeyboardInput;
import thunder.hack.events.impl.EventPlayerJump;
import thunder.hack.events.impl.EventPlayerTravel;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class Rotations extends Module {
    public Rotations() {
        super("Rotations", Category.CLIENT);
    }

    private final Setting<MoveFix> moveFix = new Setting<>("MoveFix", MoveFix.Off);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false);

    private enum MoveFix {
        Off, Focused, Free
    }

    public float fixRotation;
    private float prevYaw, prevPitch;

    public void onJump(EventPlayerJump e) {
        if (Float.isNaN(fixRotation) || moveFix.getValue() == MoveFix.Off || mc.player.isHandsBusy())
            return;

        if (e.isPre()) {
            prevYaw = mc.player.getYRot();
            mc.player.setYRot(fixRotation);
        } else mc.player.setYRot(prevYaw);
    }

    public void onPlayerMove(EventFixVelocity event) {
        if (moveFix.getValue() == MoveFix.Free) {
            if (Float.isNaN(fixRotation) || mc.player.isHandsBusy())
                return;
            event.setVelocity(fix(fixRotation, event.getMovementInput(), event.getSpeed()));
        }
    }

    public void modifyVelocity(EventPlayerTravel e) {
        if (ModuleManager.aura.isEnabled() && ModuleManager.aura.target != null && ModuleManager.aura.rotationMode.not(Aura.Mode.None)
                && ModuleManager.aura.elytraTarget.getValue() && Managers.PLAYER.ticksElytraFlying > 5) {
            if (e.isPre()) {
                prevYaw = mc.player.getYRot();
                prevPitch = mc.player.getXRot();

                mc.player.setYRot(fixRotation);
                mc.player.setXRot(ModuleManager.aura.rotationPitch);
            } else {
                mc.player.setYRot(prevYaw);
                mc.player.setXRot(prevPitch);
            }
            return;
        }

        if (moveFix.getValue() == MoveFix.Focused && !Float.isNaN(fixRotation) && !mc.player.isHandsBusy()) {
            if (e.isPre()) {
                prevYaw = mc.player.getYRot();
                mc.player.setYRot(fixRotation);
            } else {
                mc.player.setYRot(prevYaw);
            }
        }
    }

    public void onKeyInput(EventKeyboardInput e) {
        if (moveFix.getValue() == MoveFix.Free) {
            if (Float.isNaN(fixRotation) || mc.player.isHandsBusy())
                return;

            float mF = mc.player.input.getMoveVector().y;
            float mS = mc.player.input.getMoveVector().x;
            float delta = (mc.player.getYRot() - fixRotation) * Mth.DEG_TO_RAD;
            float cos = Mth.cos(delta);
            float sin = Mth.sin(delta);
            MovementUtility.setMovementInput((float) Math.round(mS * cos - mF * sin), (float) Math.round(mF * cos + mS * sin));
        }
    }

    private Vec3 fix(float yaw, Vec3 movementInput, float speed) {
        double d = movementInput.lengthSqr();
        if (d < 1.0E-7)
            return Vec3.ZERO;
        Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
        float f = Mth.sin(yaw * Mth.DEG_TO_RAD);
        float g = Mth.cos(yaw * Mth.DEG_TO_RAD);
        return new Vec3(vec3d.x * (double) g - vec3d.z * (double) f, vec3d.y, vec3d.z * (double) g + vec3d.x * (double) f);
    }

    @Override
    public boolean isToggleable() {
        return false;
    }
}
