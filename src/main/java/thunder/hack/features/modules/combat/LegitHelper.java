package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.AsyncManager;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventEntitySpawn;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IMinecraftClient;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.Bind;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.player.SearchInvResult;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import static thunder.hack.features.modules.combat.Criticals.getEntity;
import static thunder.hack.features.modules.combat.Criticals.getInteractType;

public class LegitHelper extends Module {
    public LegitHelper() {
        super("LegitHelper", Category.COMBAT);
    }

    private final Setting<BooleanSettingGroup> minecarts = new Setting<>("Minecarts", new BooleanSettingGroup(true));
    private final Setting<Float> maxDistance = new Setting<>("MaxDistance", 4f, 2f, 6f).addToGroup(minecarts);
    private final Setting<Boolean> refill = new Setting<>("Refill", true).addToGroup(minecarts);
    private final Setting<Integer> refillSlot = new Setting<>("RefillSlot", 9, 1, 9, v -> refill.getValue()).addToGroup(minecarts);

    private final Setting<BooleanSettingGroup> anchors = new Setting<>("Anchors", new BooleanSettingGroup(true));
    private final Setting<Integer> anchorDelay = new Setting<>("AnchorDelay", 50, 5, 250).addToGroup(anchors);
    private final Setting<Bind> anchorBind = new Setting<>("AnchorBind", new Bind(GLFW.GLFW_KEY_Y, false, false)).addToGroup(anchors);

    private final Setting<BooleanSettingGroup> crystals = new Setting<>("Crystals", new BooleanSettingGroup(true));
    private final Setting<Integer> crystalDelay = new Setting<>("CrystalDelay", 50, 5, 250).addToGroup(crystals);
    private final Setting<Bind> crystalBind = new Setting<>("CrystalBind", new Bind(GLFW.GLFW_KEY_U, false, false)).addToGroup(crystals);
    private final Setting<Boolean> changePitch = new Setting<>("ChangePitch", false).addToGroup(crystals);
    private final Setting<Boolean> crystalOptimizer = new Setting<>("CrystalOptimizer", false).addToGroup(crystals);
    private final Setting<Boolean> switchBack = new Setting<>("SwitchBack", false).addToGroup(crystals);

    private final Setting<BooleanSettingGroup> shieldBreaker = new Setting<>("ShieldBreaker", new BooleanSettingGroup(false));
    private final Setting<Integer> breakerDelay = new Setting<>("BreakerDelay", 50, 5, 250).addToGroup(shieldBreaker);
    private final Setting<Boolean> swapBack = new Setting<>("SwapBack", true).addToGroup(shieldBreaker);

    private final Setting<BooleanSettingGroup> windBoostJump = new Setting<>("WindBoostJump", new BooleanSettingGroup(true));
    private final Setting<Bind> windBoostBind = new Setting<>("WindBoostBind", new Bind(GLFW.GLFW_KEY_I, false, false)).addToGroup(windBoostJump);

    private final Setting<BooleanSettingGroup> crossBow = new Setting<>("CrossBow", new BooleanSettingGroup(true));
    private final Setting<Bind> crossBowBind = new Setting<>("CrossBowBind", new Bind(GLFW.GLFW_KEY_O, false, false)).addToGroup(crossBow);
    private final Setting<Boolean> cbswapBack = new Setting<>("CBSwapBack", true).addToGroup(crossBow);

    private Timer timer = new Timer();
    private Timer cbtimer = new Timer();

    private Vec3 lastCrystalVec = Vec3.ZERO;
    private Vec3 rotationVec = Vec3.ZERO;

    @Override
    public void onUpdate() {
        if (anchors.getValue().isEnabled() && isKeyPressed(anchorBind) && timer.every(anchorDelay.getValue() * 5L + 100)) {
            int glowSlot = InventoryUtility.findItemInHotBar(Items.GLOWSTONE).slot();
            int anchorSlot = InventoryUtility.findItemInHotBar(Items.RESPAWN_ANCHOR).slot();
            if (glowSlot == -1 || anchorSlot == -1) return;

            int prevSlot = mc.player.getInventory().getSelectedSlot();

            Managers.ASYNC.run(() -> {
                mc.player.getInventory().setSelectedSlot(anchorSlot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(anchorSlot));
            });

            Managers.ASYNC.run(() -> mc.executeIfPossible(() -> ((IMinecraftClient) mc).idoItemUse()), anchorDelay.getValue());

            Managers.ASYNC.run(() -> {
                mc.player.getInventory().setSelectedSlot(glowSlot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(glowSlot));
            }, anchorDelay.getValue() * 2);

            Managers.ASYNC.run(() -> mc.executeIfPossible(() -> ((IMinecraftClient) mc).idoItemUse()), anchorDelay.getValue() * 3L);

            Managers.ASYNC.run(() -> {
                mc.player.getInventory().setSelectedSlot(prevSlot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(prevSlot));
            }, anchorDelay.getValue() * 4);

            Managers.ASYNC.run(() -> mc.executeIfPossible(() -> ((IMinecraftClient) mc).idoItemUse()), anchorDelay.getValue() * 5L);

            return;
        }

        boolean crystalAtCrosshair = mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystal;
        boolean obbyAtCrosshair = mc.hitResult instanceof BlockHitResult bhr && mc.level.getBlockState(bhr.getBlockPos()).getBlock() == Blocks.OBSIDIAN;

        if (crystals.getValue().isEnabled() && isKeyPressed(crystalBind) && timer.every(crystalDelay.getValue() * (crystalAtCrosshair ? 1L : obbyAtCrosshair ? 2L : 4L))) {
            int crystalSlot = InventoryUtility.findItemInHotBar(Items.END_CRYSTAL).slot();
            int obbySlot = InventoryUtility.findBlockInHotBar(Blocks.OBSIDIAN).slot();
            if (obbySlot == -1 || crystalSlot == -1 || crystalSlot >= 9 || obbySlot >= 9) return;

            if (crystalAtCrosshair) {
                mc.gameMode.attack(mc.player, ((EntityHitResult) mc.hitResult).getEntity());
                mc.player.swing(InteractionHand.MAIN_HAND);
                return;
            }

            int prevSlot = mc.player.getInventory().getSelectedSlot();

            if (!obbyAtCrosshair) {
                mc.player.getInventory().setSelectedSlot(obbySlot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(obbySlot));
                ((IMinecraftClient) mc).idoItemUse();
            }

            Managers.ASYNC.run(() -> {
                if (!obbyAtCrosshair)
                    AsyncManager.sleep(crystalDelay.getValue());

                mc.player.getInventory().setSelectedSlot(crystalSlot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(crystalSlot));
                AsyncManager.sleep(crystalDelay.getValue());
                ((IMinecraftClient) mc).idoItemUse();
                lastCrystalVec = mc.hitResult.getLocation();
                if (switchBack.getValue()) {
                    AsyncManager.sleep(crystalDelay.getValue());
                    mc.player.getInventory().setSelectedSlot(prevSlot);
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(prevSlot));
                }
            });
        }


        if (shieldBreaker.getValue().isEnabled()
                && mc.hitResult instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof Player pl
                && !Managers.FRIEND.isFriend(pl)
                && (pl.getOffhandItem().getItem() == Items.SHIELD || pl.getMainHandItem().getItem() == Items.SHIELD)
                && pl.getUseItem().getItem() == Items.SHIELD && timer.every(500)) {

            int axeSlot = InventoryUtility.getAxeHotBar().slot();
            if (axeSlot == -1)
                return;

            int prevSlot = mc.player.getInventory().getSelectedSlot();

            Managers.ASYNC.run(() -> {
                AsyncManager.sleep(breakerDelay.getValue());
                mc.player.getInventory().setSelectedSlot(axeSlot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(axeSlot));
                AsyncManager.sleep(breakerDelay.getValue());
                if (mc.hitResult instanceof EntityHitResult ehr2)
                    mc.gameMode.attack(mc.player, ehr2.getEntity());
                mc.player.swing(InteractionHand.MAIN_HAND);

                if (swapBack.getValue()) {
                    AsyncManager.sleep(breakerDelay.getValue());
                    mc.player.getInventory().setSelectedSlot(prevSlot);
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(prevSlot));
                }
            });
        }

        if (minecarts.getValue().isEnabled() && refill.getValue())
            if (mc.player.getInventory().getItem(refillSlot.getValue() - 1).getItem() != Items.TNT_MINECART) {
                SearchInvResult result = InventoryUtility.findItemInInventory(Items.TNT_MINECART);
                if (result.found())
                    clickSlot(result.slot(), refillSlot.getValue() - 1, ContainerInput.SWAP);
            }

        if (crossBow.getValue().isEnabled() && isKeyPressed(crossBowBind) && cbtimer.every(300)) {
            SearchInvResult result = InventoryUtility.findInHotBar(i -> i.getItem() == Items.CROSSBOW && i.get(DataComponents.CHARGED_PROJECTILES) != null && !i.get(DataComponents.CHARGED_PROJECTILES).isEmpty());
            if (result.found()) {
                InventoryUtility.saveAndSwitchTo(result.slot());
                InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                if (cbswapBack.getValue())
                    InventoryUtility.returnSlot();
            }
        }
    }


    @EventHandler
    public void onEntitySpawn(EventEntitySpawn e) {
        if (e.getEntity() instanceof EndCrystal cr && e.getEntity().distanceToSqr(lastCrystalVec) < 4f) {
            lastCrystalVec = Vec3.ZERO;
            if (changePitch.getValue()) {
                float pitch = InteractionUtility.calculateAngle(cr.position().add(0, 0.15, 0))[1];
                double gcdFix = (Math.pow(mc.options.sensitivity().get() * 0.6 + 0.2, 3.0)) * 1.2;
                mc.player.setXRot((float) (pitch - (pitch - mc.player.getXRot()) % gcdFix));
            }
            mc.gameMode.attack(mc.player, e.getEntity());
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.@NotNull Send event) {
        if (crystalOptimizer.getValue() && event.getPacket() instanceof ServerboundInteractPacket
                && getInteractType(event.getPacket()) == Criticals.InteractType.ATTACK && getEntity(event.getPacket()) instanceof EndCrystal c
                && !ModuleManager.autoCrystal.isEnabled()) {
            c.discard();
            c.setRemoved(Entity.RemovalReason.KILLED);
            c.onClientRemoval();
        }
    }

    @EventHandler
    public void onPacketSendPost(PacketEvent.@NotNull SendPost event) {
        if (event.getPacket() instanceof ServerboundPlayerActionPacket action && action.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
            if (minecarts.getValue().isEnabled() && mc.player.getMainHandItem().getItem() == Items.BOW) {
                BlockPos bp = calcTrajectory(mc.player.getYRot());
                if (bp != null && PlayerUtility.squaredDistanceFromEyes(net.minecraft.world.phys.Vec3.atBottomCenterOf(bp)) <= maxDistance.getPow2Value() && PlayerUtility.squaredDistanceFromEyes(net.minecraft.world.phys.Vec3.atBottomCenterOf(bp)) > 3) {

                    SearchInvResult baseResult = InventoryUtility.findItemInHotBar(Items.RAIL, Items.ACTIVATOR_RAIL, Items.DETECTOR_RAIL, Items.POWERED_RAIL);
                    SearchInvResult cartResult = InventoryUtility.findItemInHotBar(Items.TNT_MINECART);

                    if (baseResult.found() && cartResult.found()) {
                        InventoryUtility.saveSlot();
                        baseResult.switchTo();
                        sendSequencedPacket(s -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,
                                new BlockHitResult(new Vec3(bp.getX() + 0.5, bp.above().getY(), bp.getZ() + 0.)
                                        , Direction.UP,
                                        bp,
                                        false), s));

                        rotationVec = net.minecraft.world.phys.Vec3.atBottomCenterOf(bp.above());
                        cartResult.switchTo();
                        sendSequencedPacket(s -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,
                                new BlockHitResult(new Vec3(bp.getX() + 0.5, bp.above().getY() + .125, bp.getZ() + 0.5)
                                        , Direction.UP,
                                        bp.above(),
                                        false), s));
                        InventoryUtility.returnSlot();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (rotationVec != null) {
            float[] angle = InteractionUtility.calculateAngle(rotationVec);
            mc.player.setYRot(angle[0]);
            mc.player.setXRot(angle[1]);
            rotationVec = null;
        }

        if (isKeyPressed(windBoostBind) && mc.player.onGround()) {
            SearchInvResult result = InventoryUtility.findItemInHotBar(Items.WIND_CHARGE);
            if (result.found()) {
                mc.player.setXRot(90);
                mc.player.jumpFromGround();
                InventoryUtility.saveAndSwitchTo(result.slot());
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                InventoryUtility.returnSlot();
            }
        }
    }

    private BlockPos calcTrajectory(float yaw) {
        double x = Render2DEngine.interpolate(mc.player.xo, mc.player.getX(), Render3DEngine.getTickDelta());
        double y = Render2DEngine.interpolate(mc.player.yo, mc.player.getY(), Render3DEngine.getTickDelta());
        double z = Render2DEngine.interpolate(mc.player.zo, mc.player.getZ(), Render3DEngine.getTickDelta());

        y = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;

        double motionX = -Mth.sin(yaw / 180.0f * 3.1415927f) * Mth.cos(mc.player.getXRot() / 180.0f * 3.1415927f);
        double motionY = -Mth.sin((mc.player.getXRot()) / 180.0f * 3.141593f);
        double motionZ = Mth.cos(yaw / 180.0f * 3.1415927f) * Mth.cos(mc.player.getXRot() / 180.0f * 3.1415927f);
        float power = mc.player.getTicksUsingItem() / 20.0f;

        power = (power * power + power * 2.0f) / 3.0f;
        if (power > 1.0f) {
            power = 1.0f;
        }

        final float distance = Mth.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;

        final float pow = power * 3;
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

            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;


            motionY -= 0.05000000074505806;
            Vec3 pos = new Vec3(x, y, z);

            for (Entity ent : mc.level.entitiesForRendering()) {
                if (ent instanceof Arrow || ent.equals(mc.player)) continue;
                if (ent.getBoundingBox().intersects(new AABB(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.3)))
                    return null;
            }

            BlockHitResult bhr = mc.level.clip(new ClipContext(lastPos, pos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
            if (bhr != null && bhr.getType() == HitResult.Type.BLOCK) {
                return bhr.getBlockPos();
            }

            if (y <= -65) break;
        }
        return null;
    }
}
