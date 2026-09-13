package thunder.hack.utility.render.animation;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.mojang.blaze3d.platform.GlStateManager;
import thunder.hack.utility.render.compat.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.TextureStorage;

import static thunder.hack.features.modules.Module.mc;

public class CaptureMark {
    private static float espValue = 1f, prevEspValue;
    private static float espSpeed = 1f;
    private static boolean flipSpeed;

    public static void render(Entity target) {
        Camera camera = mc.gameRenderer.getMainCamera();

        double tPosX = Render2DEngine.interpolate(target.xo, target.getX(), Render3DEngine.getTickDelta()) - camera.getPosition().x;
        double tPosY = Render2DEngine.interpolate(target.yo, target.getY(), Render3DEngine.getTickDelta()) - camera.getPosition().y;
        double tPosZ = Render2DEngine.interpolate(target.zo, target.getZ(), Render3DEngine.getTickDelta()) - camera.getPosition().z;

        PoseStack matrices = new PoseStack();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
        matrices.translate(tPosX, (tPosY + target.getEyeHeight(target.getPose()) / 2f), tPosZ);
        matrices.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        matrices.mulPose(Axis.ZP.rotationDegrees(Render2DEngine.interpolateFloat(prevEspValue, espValue, Render3DEngine.getTickDelta())));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, TextureStorage.capture);
        matrices.translate(-0.75, -0.75, -0.01);
        Matrix4f matrix = matrices.last().pose();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(matrix, 0, 1.5f, 0).setUv(0f, 1f).setColor(HudEditor.getColor(90).getRGB());
        buffer.addVertex(matrix, 1.5f, 1.5f, 0).setUv(1f, 1f).setColor(HudEditor.getColor(0).getRGB());
        buffer.addVertex(matrix, 1.5f, 0, 0).setUv(1f, 0).setColor(HudEditor.getColor(180).getRGB());
        buffer.addVertex(matrix, 0, 0, 0).setUv(0, 0).setColor(HudEditor.getColor(270).getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static void tick() {
        prevEspValue = espValue;
        espValue += espSpeed;
        if (espSpeed > 25) flipSpeed = true;
        if (espSpeed < -25) flipSpeed = false;
        espSpeed = flipSpeed ? espSpeed - 0.5f : espSpeed + 0.5f;
    }
}
