package thunder.hack.features.modules.movement;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render3DEngine;

public class ClickTP extends Module {
    public ClickTP() {
        super("ClickTP", Category.MOVEMENT);
    }

    private final Setting<Float> blockOffset = new Setting<>("BlockOffset", 1.0f, -1f, 1f);
    private final Setting<Integer> spoofs = new Setting<>("Spoofs", 0, 0, 40);
    private final Setting<Boolean> ground = new Setting<>("Ground", false);

    private int delay;

    @EventHandler
    public void onSync(EventSync e) {
        if (delay >= 0)
            delay--;

        if (mc.options.keyPickItem.isDown() && delay < 0) {
            HitResult ray = mc.player.pick(256, Render3DEngine.getTickDelta(), false);
            if (ray instanceof BlockHitResult bhr && !mc.level.isEmptyBlock(bhr.getBlockPos())) {
                Vec3 pos = net.minecraft.world.phys.Vec3.atBottomCenterOf(bhr.getBlockPos());
                for (int i = 0; i < spoofs.getValue(); ++i)
                    sendPacket(new ServerboundMovePlayerPacket.Pos(pos.x(), pos.y() + blockOffset.getValue(), pos.z(), ground.getValue(), mc.player.horizontalCollision));
                mc.player.setPos(pos.x(), pos.y() + blockOffset.getValue(), pos.z());
                delay = 5;
            }
        }
    }

    @Override
    public void onRender3D(PoseStack stack) {
        HitResult ray = mc.player.pick(256, Render3DEngine.getTickDelta(), false);
        if (ray instanceof BlockHitResult bhr && !mc.level.isEmptyBlock(bhr.getBlockPos())) {
            BlockPos pos = bhr.getBlockPos();
            Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(new AABB(pos), HudEditor.getColor(1), 1));
        }
    }
}
