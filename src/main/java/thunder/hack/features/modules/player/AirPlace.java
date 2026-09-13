package thunder.hack.features.modules.player;

import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.render.Render3DEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AirPlace extends Module {
    public AirPlace() {
        super("AirPlace", Category.PLAYER);
    }

    private final Setting<Float> range = new Setting<>("Range", 5f, 0f, 6f);

    private final Setting<SettingGroup> renderGroup = new Setting<>("Render", new SettingGroup(false, 0));
    private final Setting<Boolean> swing = new Setting<>("Swing", true).addToGroup(renderGroup);
    private final Setting<ColorSetting> fillColor = new Setting<>("Fill Color", new ColorSetting(new Color(100, 50, 255, 50))).addToGroup(renderGroup);
    private final Setting<ColorSetting> lineColor = new Setting<>("Line Color", new ColorSetting(new Color(100, 50, 255, 150))).addToGroup(renderGroup);
    private final Setting<Integer> lineWidth = new Setting<>("Line Width", 2, 1, 5).addToGroup(renderGroup);

    private BlockHitResult hit;
    private int cooldown;

    @Override
    public void onUpdate() {
        if (cooldown > 0)
            cooldown--;

        HitResult hitResult = mc.getCameraEntity().pick(range.getValue(), 0, false);

        if (hitResult instanceof BlockHitResult bhr) hit = bhr;
        else return;

        boolean main = mc.player.getMainHandItem().getItem() instanceof BlockItem;
        boolean off = mc.player.getOffhandItem().getItem() instanceof BlockItem;
        if (mc.options.keyUse.isDown() && (main || off) && cooldown <= 0) {
            mc.gameMode.useItemOn(mc.player, main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, hit);
            if (swing.getValue()) mc.player.swing(main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            else sendPacket(new ServerboundSwingPacket(main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND));
            cooldown = ModuleManager.fastUse.isEnabled() && (ModuleManager.fastUse.blocks.getValue() || ModuleManager.fastUse.all.getValue()) ? 0 : 4;
        }
    }

    @Override
    public void onRender3D(PoseStack stack) {
        if (hit == null || !mc.level.getBlockState(hit.getBlockPos()).getBlock().equals(Blocks.AIR) || (!(mc.player.getMainHandItem().getItem() instanceof BlockItem) && !(mc.player.getOffhandItem().getItem() instanceof BlockItem)))
            return;

        Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(
                new AABB(hit.getBlockPos()),
                fillColor.getValue().getColorObject()
        ));
        Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(
                new AABB(hit.getBlockPos()),
                lineColor.getValue().getColorObject(),
                lineWidth.getValue()
        ));
    }
}
