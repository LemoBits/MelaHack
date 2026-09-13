package thunder.hack.features.modules.render;

import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.Render3DEngine.FillAction;
import thunder.hack.utility.render.Render3DEngine.OutlineAction;

import static thunder.hack.utility.render.Render3DEngine.FILLED_QUEUE;
import static thunder.hack.utility.render.Render3DEngine.FillAction;
import static thunder.hack.utility.render.Render3DEngine.OUTLINE_QUEUE;
import static thunder.hack.utility.render.Render3DEngine.OutlineAction;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StorageEsp extends Module {
    public StorageEsp() {
        super("StorageEsp", Category.RENDER);
    }

    public final Setting<Boolean> outline = new Setting<>("Outline", true);
    public final Setting<Boolean> fill = new Setting<>("Fill", true);

    public final Setting<Boolean> chest = new Setting<>("Chest", true);
    public final Setting<Boolean> trappedChest = new Setting<>("Trapped Chest", true);
    public final Setting<Boolean> dispenser = new Setting<>("Dispenser", false);
    public final Setting<Boolean> shulker = new Setting<>("Shulker", true);
    public final Setting<Boolean> echest = new Setting<>("Ender Chest", true);
    public final Setting<Boolean> furnace = new Setting<>("Furnace", false);
    public final Setting<Boolean> hopper = new Setting<>("Hopper", false);
    public final Setting<Boolean> barrels = new Setting<>("Barrel", false);

    public final Setting<Boolean> cart = new Setting<>("Minecart", false);
    public final Setting<Boolean> frame = new Setting<>("ItemFrame", false);
    private final Setting<ColorSetting> chestColor = new Setting<>("ChestColor", new ColorSetting(0x8800FF00));
    private final Setting<ColorSetting> trappedChestColor = new Setting<>("TrappedChestColor", new ColorSetting(0x8800FF00));
    private final Setting<ColorSetting> shulkColor = new Setting<>("ShulkerColor", new ColorSetting(0x8800FF00));
    private final Setting<ColorSetting> echestColor = new Setting<>("EChestColor", new ColorSetting(0x8800FF00));
    private final Setting<ColorSetting> frameColor = new Setting<>("FrameColor", new ColorSetting(0x8800FF00));
    private final Setting<ColorSetting> shulkerframeColor = new Setting<>("ShulkFrameColor", new ColorSetting(0x8800FF00));
    private final Setting<ColorSetting> furnaceColor = new Setting<>("FurnaceColor", new ColorSetting(0x8800FF00));
    private final Setting<ColorSetting> hopperColor = new Setting<>("HopperColor", new ColorSetting(0x8800FF00));
    private final Setting<ColorSetting> dispenserColor = new Setting<>("DispenserColor", new ColorSetting(0x8800FF00));
    private final Setting<ColorSetting> barrelColor = new Setting<>("BarrelColor", new ColorSetting(0x8800FF00));
    private final Setting<ColorSetting> minecartColor = new Setting<>("MinecartColor", new ColorSetting(0x8800FF00));

    public void onRender3D(PoseStack stack) {
        if (mc.options.hideGui) return;
        for (BlockEntity blockEntity : getBlockEntities()) {
            Color color = getColor(blockEntity);

            if (color == null) continue;

            AABB chestbox = new AABB(
                    blockEntity.getBlockPos().getX() + 0.06,
                    blockEntity.getBlockPos().getY(),
                    blockEntity.getBlockPos().getZ() + 0.06,
                    (blockEntity.getBlockPos().getX() + 0.94),
                    (blockEntity.getBlockPos().getY() - 0.125 + 1),
                    (blockEntity.getBlockPos().getZ() + 0.94)
            );

            if (fill.getValue()) {
                if (blockEntity instanceof ChestBlockEntity) {
                    FILLED_QUEUE.add(new FillAction(chestbox, color));
                } else if (blockEntity instanceof EnderChestBlockEntity) {
                    FILLED_QUEUE.add(new FillAction(chestbox, color));
                } else FILLED_QUEUE.add(new FillAction(new AABB(blockEntity.getBlockPos()), color));
            }
            if (outline.getValue()) {
                if (blockEntity instanceof ChestBlockEntity) {
                    OUTLINE_QUEUE.add(new OutlineAction(chestbox, Render2DEngine.injectAlpha(color, 255), 1f));
                } else if (blockEntity instanceof EnderChestBlockEntity) {
                    OUTLINE_QUEUE.add(new OutlineAction(chestbox, Render2DEngine.injectAlpha(color, 255), 1f));
                } else
                    OUTLINE_QUEUE.add(new OutlineAction(new AABB(blockEntity.getBlockPos()), Render2DEngine.injectAlpha(color, 255), 1f));
            }
        }

        for (Entity ent : Managers.ASYNC.getAsyncEntities()) {
            if (ent instanceof ItemFrame iframe && frame.getValue()) {
                Color frameColor1 = frameColor.getValue().getColorObject();
                if (iframe.getItem().getItem() instanceof BlockItem bitem && bitem.getBlock() instanceof ShulkerBoxBlock)
                    frameColor1 = shulkerframeColor.getValue().getColorObject();

                if (fill.getValue())
                    FILLED_QUEUE.add(new FillAction(iframe.getBoundingBox(), frameColor1));

                if (outline.getValue())
                    OUTLINE_QUEUE.add(new OutlineAction(iframe.getBoundingBox(), Render2DEngine.injectAlpha(frameColor1, 255), 1f));
            }

            if (ent instanceof MinecartChest mcart && cart.getValue()) {
                if (fill.getValue())
                    FILLED_QUEUE.add(new FillAction(mcart.getBoundingBox(), minecartColor.getValue().getColorObject()));

                if (outline.getValue())
                    OUTLINE_QUEUE.add(new OutlineAction(mcart.getBoundingBox(), Render2DEngine.injectAlpha(minecartColor.getValue().getColorObject(), 255), 1f));
            }
        }
    }

    @Nullable
    private Color getColor(BlockEntity bEnt) {
        Color color = null;

        if (bEnt instanceof TrappedChestBlockEntity && trappedChest.getValue())
            color = trappedChestColor.getValue().getColorObject();
        else if (bEnt instanceof ChestBlockEntity && chest.getValue() && bEnt.getType() != BlockEntityType.TRAPPED_CHEST)
            color = chestColor.getValue().getColorObject();
        else if (bEnt instanceof EnderChestBlockEntity && echest.getValue())
            color = echestColor.getValue().getColorObject();
        else if (bEnt instanceof BarrelBlockEntity && barrels.getValue())
            color = barrelColor.getValue().getColorObject();
        else if (bEnt instanceof ShulkerBoxBlockEntity && shulker.getValue())
            color = shulkColor.getValue().getColorObject();
        else if (bEnt instanceof AbstractFurnaceBlockEntity && furnace.getValue())
            color = furnaceColor.getValue().getColorObject();
        else if (bEnt instanceof DispenserBlockEntity && dispenser.getValue())
            color = dispenserColor.getValue().getColorObject();
        else if (bEnt instanceof HopperBlockEntity && hopper.getValue())
            color = hopperColor.getValue().getColorObject();

        return color;
    }

    public static List<BlockEntity> getBlockEntities() {
        List<BlockEntity> list = new ArrayList<>();
        for (LevelChunk chunk : getLoadedChunks())
            list.addAll(chunk.getBlockEntities().values());

        return list;
    }

    public static List<LevelChunk> getLoadedChunks() {
        List<LevelChunk> chunks = new ArrayList<>();
        int viewDist = mc.options.renderDistance().get();
        for (int x = -viewDist; x <= viewDist; x++) {
            for (int z = -viewDist; z <= viewDist; z++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunkNow((int) mc.player.getX() / 16 + x, (int) mc.player.getZ() / 16 + z);

                if (chunk != null) chunks.add(chunk);
            }
        }
        return chunks;
    }
}
