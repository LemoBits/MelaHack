package thunder.hack.utility.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import thunder.hack.utility.render.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.mojang.blaze3d.platform.GlStateManager;
import thunder.hack.utility.render.compat.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import thunder.hack.utility.render.ShaderProgramKeys;
import thunder.hack.utility.render.BufferRenderer;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static thunder.hack.features.modules.Module.mc;

public class Render3DEngine {
    public static List<FillAction> FILLED_QUEUE = new ArrayList<>();
    public static List<OutlineAction> OUTLINE_QUEUE = new ArrayList<>();
    public static List<FadeAction> FADE_QUEUE = new ArrayList<>();
    public static List<FillSideAction> FILLED_SIDE_QUEUE = new ArrayList<>();
    public static List<OutlineSideAction> OUTLINE_SIDE_QUEUE = new ArrayList<>();
    public static List<DebugLineAction> DEBUG_LINE_QUEUE = new ArrayList<>();
    public static List<LineAction> LINE_QUEUE = new ArrayList<>();

    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

    private static float prevCircleStep;
    private static float circleStep;

    // getTickDelta() -> mc.getRenderTickCounter().getTickProgress(true)

    public static void onRender3D(PoseStack stack) {
        if (!FILLED_QUEUE.isEmpty() || !FADE_QUEUE.isEmpty() || !FILLED_SIDE_QUEUE.isEmpty()) {
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
            RenderSystem.disableDepthTest();
            setupRender();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            FILLED_QUEUE.forEach(action -> setFilledBoxVertexes(bufferBuilder, stack.last().pose(), action.box(), action.color()));

            FADE_QUEUE.forEach(action -> setFilledFadePoints(action.box(), bufferBuilder, stack.last().pose(), action.color(), action.color2()));

            FILLED_SIDE_QUEUE.forEach(action -> setFilledSidePoints(bufferBuilder, stack.last().pose(), action.box, action.color(), action.side()));
            Render2DEngine.endBuilding(bufferBuilder);

            endRender();
            RenderSystem.enableDepthTest();

            FADE_QUEUE.clear();
            FILLED_SIDE_QUEUE.clear();
            FILLED_QUEUE.clear();
        }

        if (!OUTLINE_QUEUE.isEmpty() || !OUTLINE_SIDE_QUEUE.isEmpty()) {
            setupRender();
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.LINES, DefaultVertexFormat.POSITION_COLOR);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

            RenderSystem.lineWidth(2f);

            OUTLINE_QUEUE.forEach(action -> {
                setOutlinePoints(action.box(), matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color());
            });

            OUTLINE_SIDE_QUEUE.forEach(action -> {
                setSideOutlinePoints(action.box, matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color(), action.side());
            });

            Render2DEngine.endBuilding(buffer);

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            endRender();
            OUTLINE_QUEUE.clear();
            OUTLINE_SIDE_QUEUE.clear();
        }

        if (!DEBUG_LINE_QUEUE.isEmpty()) {
            setupRender();
            RenderSystem.disableDepthTest();
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            DEBUG_LINE_QUEUE.forEach(action -> {
                PoseStack matrices = matrixFrom(action.start.x(), action.start.y(), action.start.z());
                vertexLine(matrices, buffer, 0f, 0f, 0f, (float) (action.end.x() - action.start.x()), (float) (action.end.y() - action.start.y()), (float) (action.end.z() - action.start.z()), action.color);
            });
            Render2DEngine.endBuilding(buffer);
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            endRender();
            DEBUG_LINE_QUEUE.clear();
        }

        if (!LINE_QUEUE.isEmpty()) {
            setupRender();
            Tesselator tessellator = Tesselator.getInstance();
            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            RenderSystem.lineWidth(2f);
            RenderSystem.disableDepthTest();
            BufferBuilder buffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.LINES, DefaultVertexFormat.POSITION_COLOR);
            LINE_QUEUE.forEach(action -> {
                PoseStack matrices = matrixFrom(action.start.x(), action.start.y(), action.start.z());
                vertexLine(matrices, buffer, 0f, 0f, 0f, (float) (action.end.x() - action.start.x()), (float) (action.end.y() - action.start.y()), (float) (action.end.z() - action.start.z()), action.color);
            });
            Render2DEngine.endBuilding(buffer);
            RenderSystem.enableCull();
            RenderSystem.lineWidth(1f);
            RenderSystem.enableDepthTest();
            endRender();
            LINE_QUEUE.clear();
        }
    }

    @Deprecated
    @SuppressWarnings("unused")
    public static void drawFilledBox(PoseStack stack, AABB box, Color c) {
        FILLED_QUEUE.add(new FillAction(box, c));
    }

    public static void setFilledBoxVertexes(@NotNull BufferBuilder bufferBuilder, Matrix4f m, @NotNull AABB box, @NotNull Color c) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.position().x);
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.position().y);
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.position().z);
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.position().x);
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.position().y);
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.position().z);

        bufferBuilder.addVertex(m, minX, minY, minZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, minY, minZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, minY, maxZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, minX, minY, maxZ).setColor(c.getRGB());

        bufferBuilder.addVertex(m, minX, minY, minZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, minX, maxY, minZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, maxY, minZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, minY, minZ).setColor(c.getRGB());

        bufferBuilder.addVertex(m, maxX, minY, minZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, maxY, minZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, maxY, maxZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, minY, maxZ).setColor(c.getRGB());

        bufferBuilder.addVertex(m, minX, minY, maxZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, minY, maxZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, maxY, maxZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, minX, maxY, maxZ).setColor(c.getRGB());

        bufferBuilder.addVertex(m, minX, minY, minZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, minX, minY, maxZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, minX, maxY, maxZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, minX, maxY, minZ).setColor(c.getRGB());

        bufferBuilder.addVertex(m, minX, maxY, minZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, minX, maxY, maxZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, maxY, maxZ).setColor(c.getRGB());
        bufferBuilder.addVertex(m, maxX, maxY, minZ).setColor(c.getRGB());
    }

    public static @NotNull AABB interpolateBox(@NotNull AABB from, @NotNull AABB to, float delta) {
        double X = Render2DEngine.interpolate(from.maxX, to.maxX, delta);
        double Y = Render2DEngine.interpolate(from.maxY, to.maxY, delta);
        double Z = Render2DEngine.interpolate(from.maxZ, to.maxZ, delta);
        double X1 = Render2DEngine.interpolate(from.minX, to.minX, delta);
        double Y1 = Render2DEngine.interpolate(from.minY, to.minY, delta);
        double Z1 = Render2DEngine.interpolate(from.minZ, to.minZ, delta);
        return new AABB(X1, Y1, Z1, X, Y, Z);
    }

    @Deprecated
    public static void drawFilledSide(PoseStack stack, @NotNull AABB box, Color c, Direction dir) {
        FILLED_SIDE_QUEUE.add(new FillSideAction(box, c, dir));
    }

    public static void setFilledSidePoints(BufferBuilder buffer, Matrix4f matrix, AABB box, Color c, Direction dir) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.position().x);
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.position().y);
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.position().z);
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.position().x);
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.position().y);
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.position().z);

        if (dir == Direction.DOWN) {
            buffer.addVertex(matrix, minX, minY, minZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, minY, minZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, minY, maxZ).setColor(c.getRGB());
            buffer.addVertex(matrix, minX, minY, maxZ).setColor(c.getRGB());
        }

        if (dir == Direction.NORTH) {
            buffer.addVertex(matrix, minX, minY, minZ).setColor(c.getRGB());
            buffer.addVertex(matrix, minX, maxY, minZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, maxY, minZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, minY, minZ).setColor(c.getRGB());
        }

        if (dir == Direction.EAST) {
            buffer.addVertex(matrix, maxX, minY, minZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, maxY, minZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, minY, maxZ).setColor(c.getRGB());
        }
        if (dir == Direction.SOUTH) {
            buffer.addVertex(matrix, minX, minY, maxZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, minY, maxZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(c.getRGB());
            buffer.addVertex(matrix, minX, maxY, maxZ).setColor(c.getRGB());
        }

        if (dir == Direction.WEST) {
            buffer.addVertex(matrix, minX, minY, minZ).setColor(c.getRGB());
            buffer.addVertex(matrix, minX, minY, maxZ).setColor(c.getRGB());
            buffer.addVertex(matrix, minX, maxY, maxZ).setColor(c.getRGB());
            buffer.addVertex(matrix, minX, maxY, minZ).setColor(c.getRGB());
        }

        if (dir == Direction.UP) {
            buffer.addVertex(matrix, minX, maxY, minZ).setColor(c.getRGB());
            buffer.addVertex(matrix, minX, maxY, maxZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(c.getRGB());
            buffer.addVertex(matrix, maxX, maxY, minZ).setColor(c.getRGB());
        }
    }

    public static void drawTextIn3D(String text, @NotNull Vec3 pos, double offX, double offY, double textOffset, @NotNull Color color) {
        PoseStack matrices = new PoseStack();
        Camera camera = mc.gameRenderer.mainCamera();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
        matrices.translate(pos.x() - camera.position().x, pos.y() - camera.position().y, pos.z() - camera.position().z);
        matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        setupRender();
        matrices.translate(offX, offY - 0.1, -0.01);
        matrices.scale(-0.025f, -0.025f, 0);
        FontRenderers.sf_medium.drawCenteredString(matrices, text, textOffset, 0f, color.getRGB());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        endRender();
    }

    public static @NotNull Vec3 worldSpaceToScreenSpace(@NotNull Vec3 pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        int displayHeight = mc.getWindow().getScreenHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        double deltaX = pos.x - camera.position().x;
        double deltaY = pos.y - camera.position().y;
        double deltaZ = pos.z - camera.position().z;

        Vector4f transformedCoordinates = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.f).mul(lastWorldSpaceMatrix);
        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);
        matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);

        return new Vec3(target.x / getScaleFactor(), (displayHeight - target.y) / getScaleFactor(), target.z);
    }

    public static double getScaleFactor() {
        return ClientSettings.scaleFactorFix.getValue() ? ClientSettings.scaleFactorFixValue.getValue() : mc.getWindow().getGuiScale();
    }

    @Deprecated
    @SuppressWarnings("unused")
    public static void drawFilledFadeBox(@NotNull PoseStack stack, @NotNull AABB box, @NotNull Color c, @NotNull Color c1) {
        FADE_QUEUE.add(new FadeAction(box, c, c1));
    }

    public static void setFilledFadePoints(AABB box, BufferBuilder buffer, Matrix4f posMatrix, Color c, Color c1) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.position().x);
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.position().y);
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.position().z);
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.position().x);
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.position().y);
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.position().z);

        if (ModuleManager.holeESP.culling.getValue())
            RenderSystem.enableCull();

        buffer.addVertex(posMatrix, minX, minY, minZ).setColor(c.getRGB());
        buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(c1.getRGB());
        buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(c1.getRGB());
        buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(c.getRGB());

        buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(c.getRGB());
        buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(c1.getRGB());
        buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(c1.getRGB());
        buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(c.getRGB());

        buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(c.getRGB());
        buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(c.getRGB());
        buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(c1.getRGB());
        buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(c1.getRGB());

        buffer.addVertex(posMatrix, minX, minY, minZ).setColor(c.getRGB());
        buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(c.getRGB());
        buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(c1.getRGB());
        buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(c1.getRGB());

        buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(c1.getRGB());
        buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(c1.getRGB());
        buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(c1.getRGB());
        buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(c1.getRGB());

        if (ModuleManager.holeESP.culling.getValue())
            RenderSystem.disableCull();
    }

    public static void drawLine(@NotNull Vec3 start, @NotNull Vec3 end, @NotNull Color color) {
        LINE_QUEUE.add(new LineAction(start, end, color));
    }

    @Deprecated
    public static void drawBoxOutline(@NotNull AABB box, Color color, float lineWidth) {
        OUTLINE_QUEUE.add(new OutlineAction(box, color, lineWidth));
    }

    public static void setOutlinePoints(AABB box, PoseStack matrices, BufferBuilder buffer, Color color) {
        box = box.move(new Vec3(box.minX, box.minY, box.minZ).reverse());

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
        vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
        vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
        vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
        vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
    }

    @Deprecated
    public static void drawSideOutline(@NotNull AABB box, Color color, float lineWidth, Direction dir) {
        OUTLINE_SIDE_QUEUE.add(new OutlineSideAction(box, color, lineWidth, dir));
    }

    public static void setSideOutlinePoints(AABB box, PoseStack matrices, BufferBuilder buffer, Color color, Direction dir) {
        box = box.move(new Vec3(box.minX, box.minY, box.minZ).reverse());

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        switch (dir) {
            case UP -> {
                vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
                vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
                vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
                vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
            }
            case DOWN -> {
                vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
                vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
                vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
                vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
            }
            case EAST -> {
                vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
                vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
                vertexLine(matrices, buffer, x2, y2, z2, x2, y2, z1, color);
                vertexLine(matrices, buffer, x2, y1, z2, x2, y1, z1, color);
            }
            case WEST -> {
                vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
                vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
                vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
                vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
            }
            case NORTH -> {
                vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
                vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
                vertexLine(matrices, buffer, x2, y1, z1, x1, y1, z1, color);
                vertexLine(matrices, buffer, x2, y2, z1, x1, y2, z1, color);
            }
            case SOUTH -> {
                vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
                vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
                vertexLine(matrices, buffer, x1, y1, z2, x2, y1, z2, color);
                vertexLine(matrices, buffer, x1, y2, z2, x2, y2, z2, color);
            }
        }
    }

    public static void drawHoleOutline(@NotNull AABB box, Color color, float lineWidth) {
        setupRender();
        PoseStack matrices = matrixFrom(box.minX, box.minY, box.minZ);
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.LINES, DefaultVertexFormat.POSITION_COLOR);

        RenderSystem.disableCull();
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(lineWidth);

        box = box.move(new Vec3(box.minX, box.minY, box.minZ).reverse());

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float y2 = (float) box.maxY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float z2 = (float) box.maxZ;

        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);

        vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
        vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);

        Render2DEngine.endBuilding(buffer);
        RenderSystem.enableCull();
        endRender();
    }

    public static void vertexLine(@NotNull PoseStack matrices, @NotNull VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, @NotNull Color lineColor) {
        Matrix4f model = matrices.last().pose();
        PoseStack.Pose entry = matrices.last();
        Vector3f normalVec = getNormal(x1, y1, z1, x2, y2, z2);
        buffer.addVertex(model, x1, y1, z1).setColor(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).setNormal(entry, normalVec.x(), normalVec.y(), normalVec.z());
        buffer.addVertex(model, x2, y2, z2).setColor(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).setNormal(entry, normalVec.x(), normalVec.y(), normalVec.z());
    }

    public static @NotNull Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = Mth.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);

        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    public static @NotNull PoseStack matrixFrom(double x, double y, double z) {
        PoseStack matrices = new PoseStack();

        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));

        matrices.translate(x - camera.position().x, y - camera.position().y, z - camera.position().z);

        return matrices;
    }

    public static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public static void endRender() {
        RenderSystem.disableBlend();
    }

    public static void drawTargetEsp(PoseStack stack, @NotNull Entity target) {
        ArrayList<Vec3> vecs = new ArrayList<>();
        ArrayList<Vec3> vecs1 = new ArrayList<>();
        ArrayList<Vec3> vecs2 = new ArrayList<>();

        double x = target.xo + (target.getX() - target.xo) * getTickDelta()
                - mc.getEntityRenderDispatcher().camera.position().x;
        double y = target.yo + (target.getY() - target.yo) * getTickDelta()
                - mc.getEntityRenderDispatcher().camera.position().y;
        double z = target.zo + (target.getZ() - target.zo) * getTickDelta()
                - mc.getEntityRenderDispatcher().camera.position().z;


        double height = target.getBbHeight();

        for (int i = 0; i <= 361; ++i) {
            double v = Math.sin(Math.toRadians(i));
            double u = Math.cos(Math.toRadians(i));
            Vec3 vec = new Vec3((float) (u * 0.5f), height, (float) (v * 0.5f));
            vecs.add(vec);

            double v1 = Math.sin(Math.toRadians((i + 120) % 360));
            double u1 = Math.cos(Math.toRadians(i + 120) % 360);
            Vec3 vec1 = new Vec3((float) (u1 * 0.5f), height, (float) (v1 * 0.5f));
            vecs1.add(vec1);

            double v2 = Math.sin(Math.toRadians((i + 240) % 360));
            double u2 = Math.cos(Math.toRadians((i + 240) % 360));
            Vec3 vec2 = new Vec3((float) (u2 * 0.5f), height, (float) (v2 * 0.5f));
            vecs2.add(vec2);
            height -= 0.004f;
        }


        stack.pushPose();
        stack.translate(x, y, z);
        BufferBuilder bufferBuilder;
        setupRender();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        bufferBuilder = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = stack.last().pose();

        for (int j = 0; j < vecs.size() - 1; ++j) {
            float alpha = 1f - (((float) j + ((System.currentTimeMillis() - ThunderHack.initTime) / 5f)) % 360) / 60f;
            bufferBuilder.addVertex(matrix, (float) vecs.get(j).x, (float) vecs.get(j).y, (float) vecs.get(j).z).setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int) (j / 20f)), (int) (alpha * 255)).getRGB());
            bufferBuilder.addVertex(matrix, (float) vecs.get(j + 1).x, (float) vecs.get(j + 1).y + 0.1f, (float) vecs.get(j + 1).z).setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int) (j / 20f)), (int) (alpha * 255f)).getRGB());
        }
        Render2DEngine.endBuilding(bufferBuilder);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        bufferBuilder = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int j = 0; j < vecs1.size() - 1; ++j) {
            float alpha = 1f - (((float) j + ((System.currentTimeMillis() - ThunderHack.initTime) / 5f)) % 360) / 60f;
            bufferBuilder.addVertex(matrix, (float) vecs1.get(j).x, (float) vecs1.get(j).y, (float) vecs1.get(j).z).setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int) (j / 20f)), (int) (alpha * 255)).getRGB());
            bufferBuilder.addVertex(matrix, (float) vecs1.get(j + 1).x, (float) vecs1.get(j + 1).y + 0.1f, (float) vecs1.get(j + 1).z).setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int) (j / 20f)), (int) (alpha * 255f)).getRGB());
        }
        Render2DEngine.endBuilding(bufferBuilder);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        bufferBuilder = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int j = 0; j < vecs2.size() - 1; ++j) {
            float alpha = 1f - (((float) j + ((System.currentTimeMillis() - ThunderHack.initTime) / 5f)) % 360) / 60f;
            bufferBuilder.addVertex(matrix, (float) vecs2.get(j).x, (float) vecs2.get(j).y, (float) vecs2.get(j).z).setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int) (j / 20f)), (int) (alpha * 255)).getRGB());
            bufferBuilder.addVertex(matrix, (float) vecs2.get(j + 1).x, (float) vecs2.get(j + 1).y + 0.1f, (float) vecs2.get(j + 1).z).setColor(Render2DEngine.injectAlpha(HudEditor.getColor((int) (j / 20f)), (int) (alpha * 255f)).getRGB());
        }
        Render2DEngine.endBuilding(bufferBuilder);

        RenderSystem.enableCull();
        stack.translate(-x, -y, -z);
        endRender();
        RenderSystem.enableDepthTest();
        stack.popPose();
    }

    public static void renderCrosses(@NotNull AABB box, Color color, float lineWidth) {
        setupRender();
        PoseStack matrices = matrixFrom(box.minX, box.minY, box.minZ);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(lineWidth);
        BufferBuilder buffer = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.LINES, DefaultVertexFormat.POSITION_COLOR);

        box = box.move(new Vec3(box.minX, box.minY, box.minZ).reverse());

        vertexLine(matrices, buffer, (float) box.maxX, (float) box.minY, (float) box.minZ, (float) box.minX, (float) box.minY, (float) box.maxZ, color);
        vertexLine(matrices, buffer, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.minY, (float) box.maxZ, color);

        Render2DEngine.endBuilding(buffer);
        RenderSystem.enableCull();
        endRender();
    }

    public static void drawSphere(PoseStack matrix, float radius, int slices, int stacks, int color) {
        float drho = 3.1415927F / ((float) stacks);
        float dtheta = 6.2831855F / ((float) slices - 1f);
        float rho;
        float theta;
        float x;
        float y;
        float z;
        int i;
        int j;
        setupRender();
        for (i = 1; i < stacks; ++i) {
            rho = (float) i * drho;

            BufferBuilder buffer = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            for (j = 0; j < slices; ++j) {
                theta = (float) j * dtheta;
                x = (float) (Math.cos(theta) * Math.sin(rho));
                y = (float) (Math.sin(theta) * Math.sin(rho));
                z = (float) Math.cos(rho);
                buffer.addVertex(matrix.last().pose(), x * radius, y * radius, z * radius).setColor(color);
            }
            Render2DEngine.endBuilding(buffer);
        }

        for (j = 0; j < slices; ++j) {
            theta = (float) j * dtheta;

            BufferBuilder buffer = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            for (i = 0; i <= stacks; ++i) {
                rho = (float) i * drho;
                x = (float) (Math.cos(theta) * Math.sin(rho));
                y = (float) (Math.sin(theta) * Math.sin(rho));
                z = (float) Math.cos(rho);
                buffer.addVertex(matrix.last().pose(), x * radius, y * radius, z * radius).setColor(color);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.buildOrThrow());
        }
        endRender();
    }

    public static void drawCylinder(PoseStack stack, final float radius, final float height, final int slices, final int stacks, int color) {

        final float da = (float) ((Math.PI * 2f) / slices);
        final float dz = height / stacks;

        BufferBuilder buffer = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float y = 0;

        for (int j = 0; j <= stacks; ++j) {
            for (int i = 0; i <= slices; ++i) {
                final float x = (float) Math.cos(i * da);
                final float z = (float) Math.sin(i * da);
                buffer.addVertex(stack.last().pose(), x * radius, y, z * radius).setColor(color);
            }
            y += dz;
        }

        BufferRenderer.drawWithGlobalProgram(buffer.buildOrThrow());

        buffer = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (int i = 0; i <= slices; ++i) {
            final float x = (float) Math.cos(i * da);
            final float z = (float) Math.sin(i * da);

            buffer.addVertex(stack.last().pose(), x * radius, 0, z * radius).setColor(color);
            buffer.addVertex(stack.last().pose(), x * radius, height, z * radius).setColor(color);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.buildOrThrow());
    }


    public static void drawCircle3D(PoseStack stack, Entity ent, float radius, int color, int points, boolean hudColor, int colorOffset) {
        setupRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        double x = ent.xo + (ent.getX() - ent.xo) * getTickDelta() - mc.getEntityRenderDispatcher().camera.position().x;
        double y = ent.yo + (ent.getY() - ent.yo) * getTickDelta() - mc.getEntityRenderDispatcher().camera.position().y;
        double z = ent.zo + (ent.getZ() - ent.zo) * getTickDelta() - mc.getEntityRenderDispatcher().camera.position().z;
        stack.pushPose();
        stack.translate(x, y, z);

        Matrix4f matrix = stack.last().pose();
        for (int i = 0; i <= points; i++) {
            if (hudColor)
                color = HudEditor.getColor(i * colorOffset).getRGB();

            bufferBuilder.addVertex(matrix, (float) (radius * Math.cos(i * 6.28 / points)), 0f, (float) (radius * Math.sin(i * 6.28 / points))).setColor(color);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.buildOrThrow());
        endRender();
        stack.translate(-x, -y, -z);
        stack.popPose();
    }

    public static void drawOldTargetEsp(PoseStack stack, Entity target) {
        double cs = prevCircleStep + (circleStep - prevCircleStep) * getTickDelta();
        double prevSinAnim = absSinAnimation(cs - 0.45f);
        double sinAnim = absSinAnimation(cs);
        double x = target.xo + (target.getX() - target.xo) * getTickDelta() - mc.getEntityRenderDispatcher().camera.position().x;
        double y = target.yo + (target.getY() - target.yo) * getTickDelta() - mc.getEntityRenderDispatcher().camera.position().y + prevSinAnim * target.getBbHeight();
        double z = target.zo + (target.getZ() - target.zo) * getTickDelta() - mc.getEntityRenderDispatcher().camera.position().z;
        double nextY = target.yo + (target.getY() - target.yo) * getTickDelta() - mc.getEntityRenderDispatcher().camera.position().y + sinAnim * target.getBbHeight();
        stack.pushPose();
        setupRender();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        float cos;
        float sin;
        for (int i = 0; i <= 30; i++) {
            cos = (float) (x + Math.cos(i * 6.28 / 30) * target.getBbWidth() * 0.8);
            sin = (float) (z + Math.sin(i * 6.28 / 30) * target.getBbWidth() * 0.8);
            bufferBuilder.addVertex(stack.last().pose(), cos, (float) nextY, sin).setColor(Render2DEngine.injectAlpha(HudEditor.getColor(i), 170).getRGB());
            bufferBuilder.addVertex(stack.last().pose(), cos, (float) y, sin).setColor(Render2DEngine.injectAlpha(HudEditor.getColor(i), 0).getRGB());
        }
        Render2DEngine.endBuilding(bufferBuilder);
        RenderSystem.enableCull();
        endRender();
        RenderSystem.enableDepthTest();
        stack.popPose();
    }

    // Kalry не пасть
    // anti yg protection
    public static void renderGhosts(int espLength, int factor, float shaking, float amplitude, Entity target) {
        Camera camera = mc.gameRenderer.mainCamera();

        double tPosX = Render2DEngine.interpolate(target.xo, target.getX(), Render3DEngine.getTickDelta()) - camera.position().x;
        double tPosY = Render2DEngine.interpolate(target.yo, target.getY(), Render3DEngine.getTickDelta()) - camera.position().y;
        double tPosZ = Render2DEngine.interpolate(target.zo, target.getZ(), Render3DEngine.getTickDelta()) - camera.position().z;
        float iAge = (float) Render2DEngine.interpolate(target.tickCount - 1, target.tickCount, Render3DEngine.getTickDelta());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, TextureStorage.firefly);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        boolean canSee = mc.player.hasLineOfSight(target);

        if (canSee) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else RenderSystem.disableDepthTest();

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i <= espLength; i++) {
                double radians = Math.toRadians((((float) i / 1.5f + iAge) * factor + (j * 120)) % (factor * 360));
                double sinQuad = Math.sin(Math.toRadians(iAge * 2.5f + i * (j + 1)) * amplitude) / shaking;

                float offset = ((float) i / espLength);
                PoseStack matrices = new PoseStack();
                matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
                matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
                matrices.translate(tPosX + Math.cos(radians) * target.getBbWidth(), (tPosY + 1 + sinQuad), tPosZ + Math.sin(radians) * target.getBbWidth());
                matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
                matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
                Matrix4f matrix = matrices.last().pose();
                int color = Render2DEngine.applyOpacity(HudEditor.getColor((int) (180 * offset)), offset).getRGB();
                float scale = Math.max(0.24f * (offset), 0.2f);
                buffer.addVertex(matrix, -scale, scale, 0).setUv(0f, 1f).setColor(color);
                buffer.addVertex(matrix, scale, scale, 0).setUv(1f, 1f).setColor(color);
                buffer.addVertex(matrix, scale, -scale, 0).setUv(1f, 0).setColor(color);
                buffer.addVertex(matrix, -scale, -scale, 0).setUv(0, 0).setColor(color);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.buildOrThrow());

        if (canSee) {
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
        } else RenderSystem.enableDepthTest();

        RenderSystem.disableBlend();
    }

    public static void updateTargetESP() {
        prevCircleStep = circleStep;
        circleStep += 0.15f;
    }

    public static double absSinAnimation(double input) {
        return Math.abs(1 + Math.sin(input)) / 2;
    }

    public static Vec3 interpolatePos(float prevposX, float prevposY, float prevposZ, float posX, float posY, float posZ) {
        double x = prevposX + ((posX - prevposX) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.position().x;
        double y = prevposY + ((posY - prevposY) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.position().y;
        double z = prevposZ + ((posZ - prevposZ) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.position().z;
        return new Vec3(x, y, z);
    }

    public static void drawLineDebug(Vec3 start, Vec3 end, Color color) {
        DEBUG_LINE_QUEUE.add(new DebugLineAction(start, end, color));
    }

    public static float getTickDelta() {
        return mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
    }

    public record FillAction(AABB box, Color color) {
    }

    public record OutlineAction(AABB box, Color color, float lineWidth) {
    }

    public record FadeAction(AABB box, Color color, Color color2) {
    }

    public record FillSideAction(AABB box, Color color, Direction side) {
    }

    public record OutlineSideAction(AABB box, Color color, float lineWidth, Direction side) {
    }

    public record DebugLineAction(Vec3 start, Vec3 end, Color color) {
    }

    public record LineAction(Vec3 start, Vec3 end, Color color) {
    }
}
