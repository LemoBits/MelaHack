package thunder.hack.features.modules.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import thunder.hack.utility.render.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import thunder.hack.utility.render.compat.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import thunder.hack.utility.render.ShaderProgramKeys;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import thunder.hack.utility.render.BufferRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import thunder.hack.events.impl.TotemPopEvent;
import thunder.hack.injection.accesors.IEntity;
import thunder.hack.injection.accesors.IEntityRenderDispatcher;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.util.concurrent.CopyOnWriteArrayList;

public final class PopChams extends Module {
    public PopChams() {
        super("PopChams", Category.RENDER);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Textured);
    private final Setting<Boolean> secondLayer = new Setting<>("SecondLayer", true);
    private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(0x8800FF00));
    private final Setting<Integer> ySpeed = new Setting<>("YSpeed", 0, -10, 10);
    private final Setting<Integer> aSpeed = new Setting<>("AlphaSpeed", 5, 1, 100);
    private final Setting<Float> rotSpeed = new Setting<>("RotationSpeed", 0.25f, 0f, 6f);

    private final CopyOnWriteArrayList<Person> popList = new CopyOnWriteArrayList<>();

    private enum Mode {
        Simple, Textured
    }

    @Override
    public void onUpdate() {
        popList.forEach(person -> person.update(popList));
    }

    @Override
    public void onRender3D(PoseStack stack) {
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        if (mode.is(Mode.Simple)) RenderSystem.defaultBlendFunc();
        else RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        popList.forEach(person -> renderEntity(stack, person.player, person.modelPlayer, person.getTexture(), person.getAlpha()));
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    @EventHandler
    @SuppressWarnings("unused")
    private void onTotemPop(@NotNull TotemPopEvent e) {
        if (e.getEntity().equals(mc.player) || mc.level == null) return;

        Player entity = new Player(mc.level, new GameProfile(e.getEntity().getUUID(), e.getEntity().getName().getString())) {
            @Override public boolean isSpectator() {return false;}
            @Override public boolean isCreative() {return false;}
            @Override public GameType gameMode() { return GameType.SURVIVAL; }
        };

        entity.copyPosition(e.getEntity());
        entity.yBodyRot = e.getEntity().yBodyRot;
        entity.yHeadRot = e.getEntity().yHeadRot;
        entity.attackAnim = e.getEntity().attackAnim;
        entity.swingTime = e.getEntity().swingTime;
        entity.setShiftKeyDown(e.getEntity().isShiftKeyDown());
        entity.walkAnimation.setSpeed(e.getEntity().walkAnimation.speed());
        popList.add(new Person(entity, ((AbstractClientPlayer) e.getEntity()).getSkin().body().texturePath()));
    }

    private void renderEntity(@NotNull PoseStack matrices, @NotNull LivingEntity entity, @NotNull PlayerModel modelBase, Identifier texture, int alpha) {
        modelBase.leftPants.visible = secondLayer.getValue();
        modelBase.rightPants.visible = secondLayer.getValue();
        modelBase.leftSleeve.visible = secondLayer.getValue();
        modelBase.rightSleeve.visible = secondLayer.getValue();
        modelBase.jacket.visible = secondLayer.getValue();
        modelBase.hat.visible = secondLayer.getValue();

        double x = entity.getX() - mc.getEntityRenderDispatcher().camera.position().x;
        double y = entity.getY() - mc.getEntityRenderDispatcher().camera.position().y;
        double z = entity.getZ() - mc.getEntityRenderDispatcher().camera.position().z;
        ((IEntity) entity).thunderHack$setPosition(entity.position().add(0, (double) ySpeed.getValue() / 50., 0));

        matrices.pushPose();
        matrices.translate((float) x, (float) y, (float) z);

        float yRotYaw = ((alpha / 255f) * 360f * rotSpeed.getValue());
        yRotYaw = yRotYaw == 0 ? 0 : Render2DEngine.interpolateFloat(yRotYaw, yRotYaw - (((aSpeed.getValue() / 255f) * 360f * rotSpeed.getValue())), Render3DEngine.getTickDelta());

        matrices.mulPose(Axis.YP.rotation(MathUtility.rad(180 - entity.yBodyRot + yRotYaw)));
        prepareScale(matrices);

        @SuppressWarnings("unchecked")
        AvatarRenderState renderState = ((EntityRenderer<Player, AvatarRenderState>) mc.getEntityRenderDispatcher()
                .getRenderer((Player) entity))
                .createRenderState((Player) entity, Render3DEngine.getTickDelta());
        modelBase.setupAnim(renderState);

        BufferBuilder buffer;
        if (mode.is(Mode.Textured)) {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
            buffer = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION);
            buffer = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
        }

        RenderSystem.setShaderColor(color.getValue().getGlRed(), color.getValue().getGlGreen(), color.getValue().getGlBlue(), alpha / 255f);

        modelBase.renderToBuffer(matrices, buffer, 10, 0, -1);
        Render2DEngine.endBuilding(buffer);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        matrices.popPose();
    }

    private static void prepareScale(@NotNull PoseStack matrixStack) {
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.scale(1.6f, 1.8f, 1.6f);
        matrixStack.translate(0.0F, -1.501F, 0.0F);
    }

    private class Person {
        private final Player player;
        private final PlayerModel modelPlayer;
        private Identifier texture;
        private int alpha;

        public Person(Player player, Identifier texture) {
            this.player = player;
            modelPlayer = new PlayerModel(new EntityRendererProvider.Context(mc.getEntityRenderDispatcher(), new net.minecraft.client.renderer.block.BlockModelResolver(mc.getModelManager()), mc.getItemModelResolver(), mc.getMapRenderer(), mc.getResourceManager(), mc.getEntityModels(), ((IEntityRenderDispatcher) mc.getEntityRenderDispatcher()).getEquipmentModelLoader(), mc.getAtlasManager(), mc.font, mc.playerSkinRenderCache()).bakeLayer(ModelLayers.PLAYER), false);
            modelPlayer.getHead().offsetScale(new Vector3f(-0.3f, -0.3f, -0.3f));
            alpha = color.getValue().getAlpha();
            this.texture = texture;
        }

        public void update(CopyOnWriteArrayList<Person> arrayList) {
            if (alpha <= 0) {
                arrayList.remove(this);
                player.discard();
                player.remove(Entity.RemovalReason.KILLED);
                player.onClientRemoval();
                return;
            }
            alpha -= aSpeed.getValue();
        }

        public int getAlpha() {
            return MathUtility.clamp(alpha, 0, 255);
        }

        public Identifier getTexture() {
            return texture;
        }
    }
}
