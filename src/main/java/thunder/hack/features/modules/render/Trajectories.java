package thunder.hack.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

public class Trajectories extends Module {
    public Trajectories() {
        super("Trajectories", Category.RENDER);
    }

    private final Setting<Mode> mode = new Setting<>("ColorMode", Mode.Sync);
    private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(0x2250b4b4), v -> mode.getValue() == Mode.Custom);
    private final Setting<Mode> lmode = new Setting<>("LandedColorMode", Mode.Sync);
    private final Setting<ColorSetting> lcolor = new Setting<>("LandedColor", new ColorSetting(0x2250b4b4), v -> lmode.getValue() == Mode.Custom);

    private boolean isThrowable(Item item) {
        return item instanceof EnderpearlItem || item instanceof TridentItem || item instanceof ExperienceBottleItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem;
    }

    private float getDistance(Item item) {
        return item instanceof BowItem ? 1.0f : 0.4f;
    }

    private float getThrowVelocity(Item item) {
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) return 0.5f;
        if (item instanceof ExperienceBottleItem) return 0.59f;
        if (item instanceof TridentItem) return 2f;
        return 1.5f;
    }

    private int getThrowPitch(Item item) {
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem || item instanceof ExperienceBottleItem)
            return 20;
        return 0;
    }

    @Override
    public void onRender3D(PoseStack stack) {
        if (mc.options.hideGui) return;
        if (mc.player == null || mc.level == null || !mc.options.getCameraType().isFirstPerson())
            return;
        InteractionHand hand;

        ItemStack mainHand = mc.player.getMainHandItem();
        ItemStack offHand = mc.player.getOffhandItem();

        if (mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem || isThrowable(mainHand.getItem())) {
            hand = InteractionHand.MAIN_HAND;
        } else if (offHand.getItem() instanceof BowItem || offHand.getItem() instanceof CrossbowItem || isThrowable(offHand.getItem())) {
            hand = InteractionHand.OFF_HAND;
        } else return;

        boolean prev_bob = mc.options.bobView().get();
        mc.options.bobView().set(false);

        var enchantments = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if ((offHand.getItem() instanceof CrossbowItem && EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.MULTISHOT), offHand) != 0) ||
                (mainHand.getItem() instanceof CrossbowItem && EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.MULTISHOT), mainHand) != 0)) {

            calcTrajectory(hand == InteractionHand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYRot() - 10);
            calcTrajectory(hand == InteractionHand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYRot());
            calcTrajectory(hand == InteractionHand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYRot() + 10);

        } else calcTrajectory(hand == InteractionHand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYRot());
        mc.options.bobView().set(prev_bob);
    }

    private void calcTrajectory(Item item, float yaw) {
        double x = Render2DEngine.interpolate(mc.player.xo, mc.player.getX(), Render3DEngine.getTickDelta());
        double y = Render2DEngine.interpolate(mc.player.yo, mc.player.getY(), Render3DEngine.getTickDelta());
        double z = Render2DEngine.interpolate(mc.player.zo, mc.player.getZ(), Render3DEngine.getTickDelta());

        y = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;

        if (item == mc.player.getMainHandItem().getItem()) {
            x = x - Mth.cos(yaw / 180.0f * 3.1415927f) * 0.16f;
            z = z - Mth.sin(yaw / 180.0f * 3.1415927f) * 0.16f;
        } else {
            x = x + Mth.cos(yaw / 180.0f * 3.1415927f) * 0.16f;
            z = z + Mth.sin(yaw / 180.0f * 3.1415927f) * 0.16f;
        }

        final float maxDist = getDistance(item);
        double motionX = -Mth.sin(yaw / 180.0f * 3.1415927f) * Mth.cos(mc.player.getXRot() / 180.0f * 3.1415927f) * maxDist;
        double motionY = -Mth.sin((mc.player.getXRot() - getThrowPitch(item)) / 180.0f * 3.141593f) * maxDist;
        double motionZ = Mth.cos(yaw / 180.0f * 3.1415927f) * Mth.cos(mc.player.getXRot() / 180.0f * 3.1415927f) * maxDist;

        float power = mc.player.getTicksUsingItem() / 20.0f;
        power = (power * power + power * 2.0f) / 3.0f;

        if (power > 1.0f || power == 0) {
            power = 1.0f;
        }

        final float distance = Mth.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;

        final float pow = (item instanceof BowItem ? (power * 2.0f) : item instanceof CrossbowItem ? (2.2f) : 1.0f) * getThrowVelocity(item);

        motionX *= pow;
        motionY *= pow;
        motionZ *= pow;
        if (!mc.player.onGround())
            motionY += mc.player.getDeltaMovement().y();

        Vec3 lastPos;
        for (int i = 0; i < 300; i++) {
            lastPos = new Vec3(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            if (mc.level.getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock() == Blocks.WATER) {
                motionX *= 0.8;
                motionY *= 0.8;
                motionZ *= 0.8;
            } else {
                motionX *= 0.99;
                motionY *= 0.99;
                motionZ *= 0.99;
            }

            if (item instanceof BowItem) motionY -= 0.05000000074505806;
            else if (mc.player.getMainHandItem().getItem() instanceof CrossbowItem) motionY -= 0.05000000074505806;
            else motionY -= 0.03f;


            Vec3 pos = new Vec3(x, y, z);

            for (Entity ent : mc.level.entitiesForRendering()) {
                if (ent instanceof Arrow || ent.equals(mc.player)) continue;
                if (ent.getBoundingBox().intersects(new AABB(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.3))) {
                    Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(
                            ent.getBoundingBox(),
                            lmode.getValue() == Mode.Sync ? HudEditor.getColor(i * 10) : lcolor.getValue().getColorObject(),
                            2f));
                    Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(
                            ent.getBoundingBox(), lmode.getValue() == Mode.Sync ? Render2DEngine.injectAlpha(HudEditor.getColor(i * 10), 100) : lcolor.getValue().getColorObject()
                    ));
                    break;
                }
            }

            BlockHitResult bhr = mc.level.clip(new ClipContext(lastPos, pos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
            if (bhr != null && bhr.getType() == HitResult.Type.BLOCK) {
                Render3DEngine.OUTLINE_SIDE_QUEUE.add(new Render3DEngine.OutlineSideAction(
                        new AABB(bhr.getBlockPos()), lmode.getValue() == Mode.Sync ? HudEditor.getColor(i * 10) : lcolor.getValue().getColorObject(), 2f, bhr.getDirection()
                ));
                Render3DEngine.FILLED_SIDE_QUEUE.add(new Render3DEngine.FillSideAction(
                        new AABB(bhr.getBlockPos()), lmode.getValue() == Mode.Sync ? Render2DEngine.injectAlpha(HudEditor.getColor(i * 10), 100) : lcolor.getValue().getColorObject(), bhr.getDirection()
                ));


                break;
            }

            if (y <= -65) break;
            if (motionX == 0 && motionY == 0 && motionZ == 0) continue;

            Render3DEngine.drawLine(lastPos, pos, mode.getValue() == Mode.Sync ? HudEditor.getColor(i) : color.getValue().getColorObject());
        }
    }

    private enum Mode {
        Custom,
        Sync
    }
}
