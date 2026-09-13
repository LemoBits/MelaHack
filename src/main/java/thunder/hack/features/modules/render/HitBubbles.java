package thunder.hack.features.modules.render;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import thunder.hack.utility.render.compat.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventAttack;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.injection.accesors.IEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

import java.util.ArrayList;

import static thunder.hack.utility.render.Render2DEngine.drawBubble;

public class HitBubbles extends Module {
    public HitBubbles() {
        super("HitBubbles", Category.RENDER);
    }

    public final Setting<Integer> lifeTime = new Setting<>("LifeTime", 30, 1, 150);

    private final ArrayList<HitBubble> bubbles = new ArrayList<>();

    @EventHandler
    public void onHit(EventAttack e) {
        Vec3 point = Managers.PLAYER.getRtxPoint(((IEntity) mc.player).getLastYaw(), ((IEntity) mc.player).getLastPitch(), ModuleManager.aura.attackRange.getValue());
        if (point != null && !e.isPre())
            bubbles.add(new HitBubble((float) point.x, (float) point.y, (float) point.z, -((IEntity) mc.player).getLastYaw(), ((IEntity) mc.player).getLastPitch(), new Timer()));
    }

    public void onRender3D(PoseStack matrixStack) {
        RenderSystem.disableDepthTest();
        ArrayList<HitBubble> bubblesCopy = Lists.newArrayList(bubbles);
        bubblesCopy.forEach(b -> {
            matrixStack.pushPose();
            matrixStack.translate(b.x - mc.getEntityRenderDispatcher().camera.getPosition().x(), b.y - mc.getEntityRenderDispatcher().camera.getPosition().y(), b.z - mc.getEntityRenderDispatcher().camera.getPosition().z());
            matrixStack.mulPose(Axis.YP.rotationDegrees(b.yaw));
            matrixStack.mulPose(Axis.XP.rotationDegrees(b.pitch));
            drawBubble(matrixStack, -b.life.getPassedTimeMs() / 4f, b.life.getPassedTimeMs() / 1500f);
            matrixStack.popPose();
        });
        RenderSystem.enableDepthTest();
        bubbles.removeIf(b -> b.life.passedMs(lifeTime.getValue() * 50));
    }

    public record HitBubble(float x, float y, float z, float yaw, float pitch, Timer life) {
    }
}
