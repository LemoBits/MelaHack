package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.injection.accesors.IInteractionManager;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.interfaces.IEntity;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.MovementUtility;
import thunder.hack.utility.player.SearchInvResult;

import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.utility.player.MovementUtility.isMoving;

public class Speed extends Module {
    public Speed() {
        super("Speed", Category.MOVEMENT);
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.NCP);
    public Setting<Boolean> useTimer = new Setting<>("Use Timer", false);
    public Setting<Boolean> pauseInLiquids = new Setting<>("PauseInLiquids", false);
    public Setting<Boolean> pauseWhileSneaking = new Setting<>("PauseWhileSneaking", false);
    public final Setting<Integer> hurttime = new Setting<>("HurtTime", 0, 0, 10, v -> mode.is(Mode.MatrixDamage));
    public final Setting<Float> boostFactor = new Setting<>("BoostFactor", 2f, 0f, 10f, v -> mode.is(Mode.MatrixDamage) || mode.is(Mode.Vanilla));
    public final Setting<Boolean> allowOffGround = new Setting<>("AllowOffGround", true, v -> mode.is(Mode.MatrixDamage));
    public final Setting<Integer> shiftTicks = new Setting<>("ShiftTicks", 0, 0, 10, v -> mode.is(Mode.MatrixDamage));
    public final Setting<Integer> fireWorkSlot = new Setting<>("FireSlot", 1, 1, 9, v -> mode.getValue() == Mode.FireWork);
    public final Setting<Integer> delay = new Setting<>("Delay", 8, 1, 20, v -> mode.getValue() == Mode.FireWork);
    public final Setting<Boolean> strict = new Setting<>("Strict", false, v -> mode.is(Mode.GrimIce));
    public final Setting<Float> matrixJBSpeed = new Setting<>("TimerSpeed", 1.088f, 1f, 2f, v -> mode.is(Mode.MatrixJB));
    public final Setting<Boolean> armorStands = new Setting<>("ArmorStands", false, v -> mode.is(Mode.GrimCombo) || mode.is(Mode.GrimEntity2));

    public double baseSpeed;
    private int stage, ticks, prevSlot;
    private float prevForward = 0;
    private thunder.hack.utility.Timer elytraDelay = new thunder.hack.utility.Timer();
    private thunder.hack.utility.Timer startDelay = new thunder.hack.utility.Timer();

    public enum Mode {
        StrictStrafe, MatrixJB, NCP, ElytraLowHop, MatrixDamage, GrimEntity, GrimEntity2, FireWork, Vanilla, GrimIce, GrimCombo
    }

    @Override
    public void onDisable() {
        ThunderHack.TICK_TIMER = 1f;
    }

    @Override
    public void onEnable() {
        stage = 1;
        ticks = 0;
        baseSpeed = 0.2873D;
        startDelay.reset();
        prevSlot = -1;
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player.isInLiquid() && pauseInLiquids.getValue() || mc.player.isShiftKeyDown() && pauseWhileSneaking.getValue()) {
            return;
        }

        if (mode.getValue() == Mode.MatrixJB) {
            boolean closeToGround = false;

            for (VoxelShape a : mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().inflate(0.5, 0.0, 0.5).move(0.0, -1.0, 0.0)))
                if (a != Shapes.empty()) {
                    closeToGround = true;
                    break;
                }

            if (MovementUtility.isMoving() && closeToGround && mc.player.fallDistance <= 0) {
                ThunderHack.TICK_TIMER = 1f;
                mc.player.setOnGround(true);
                mc.player.jumpFromGround();
            } else if (mc.player.fallDistance > 0 && useTimer.getValue()) {
                ThunderHack.TICK_TIMER = matrixJBSpeed.getValue();
                mc.player.push(0f, -0.003f, 0f);
            }
        }
    }

    @EventHandler
    public void modifyVelocity(EventPlayerTravel e) {
        if (mode.getValue() == Mode.GrimEntity && !e.isPre() && ThunderHack.core.getSetBackTime() > 1000) {
            for (Player ent : Managers.ASYNC.getAsyncPlayers()) {
                if (ent != mc.player && mc.player.distanceToSqr(ent) <= 2.25) {
                    float p = mc.level.getBlockState(((IEntity) mc.player).thunderHack_Recode$getVelocityBP()).getBlock().getFriction();
                    float f = mc.player.onGround() ? p * 0.91f : 0.91f;
                    float f2 = mc.player.onGround() ? p : 0.99f;
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().x() / f * f2, mc.player.getDeltaMovement().y(), mc.player.getDeltaMovement().z() / f * f2);
                    break;
                }
            }
        }

        if ((mode.is(Mode.GrimEntity2) || mode.is(Mode.GrimCombo)) && !e.isPre() && ThunderHack.core.getSetBackTime() > 1000 && MovementUtility.isMoving()) {
            int collisions = 0;
            for (Entity ent : mc.level.entitiesForRendering())
                if (ent != mc.player && (!(ent instanceof ArmorStand) || armorStands.getValue()) && (ent instanceof LivingEntity || ent instanceof Boat) && mc.player.getBoundingBox().inflate(1.0).intersects(ent.getBoundingBox()))
                    collisions++;

            double[] motion = MovementUtility.forward(0.08 * collisions);
            mc.player.push(motion[0], 0.0, motion[1]);
        }
    }

    @EventHandler
    public void onTick(EventTick e) {
        //first author: Delyfss
        if ((mode.is(Mode.GrimIce) || mode.is(Mode.GrimCombo)) && mc.player.onGround()) {
            BlockPos pos = ((IEntity) mc.player).thunderHack_Recode$getVelocityBP();
            SearchInvResult result = InventoryUtility.findBlockInHotBar(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);
            if (mc.level.isEmptyBlock(pos) || !result.found() || !mc.options.keyJump.isDown())
                return;

            prevSlot = mc.player.getInventory().getSelectedSlot();
            result.switchTo();
            sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), 90, mc.player.onGround(), mc.player.horizontalCollision));

            if (strict.getValue()) {
                sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
                sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.UP));
            }

            sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
            sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(pos.below().getCenter().add(0, 0.5, 0), Direction.UP, pos.below(), false), id));
            mc.level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
        }
    }

    @EventHandler
    public void onPostTick(EventPostTick e) {
        if ((mode.is(Mode.GrimIce) || mode.is(Mode.GrimCombo)) && prevSlot != -1) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
            ((IInteractionManager) mc.gameMode).syncSlot();
            prevSlot = -1;
        }
    }

    @Override
    public void onUpdate() {
        if (mc.player.isInLiquid() && pauseInLiquids.getValue() || mc.player.isShiftKeyDown() && pauseWhileSneaking.getValue()) {
            return;
        }

        if (mode.getValue() == Mode.FireWork) {
            ticks--;
            int ellySlot = InventoryUtility.getElytra();
            int fireSlot = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET).slot();
            boolean inOffHand = mc.player.getOffhandItem().getItem() == Items.FIREWORK_ROCKET;
            if (fireSlot == -1) {
                int fireInInv = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).slot();
                if (fireInInv != -1)
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, fireInInv, fireWorkSlot.getValue() - 1, ContainerInput.SWAP, mc.player);
            }

            if (ellySlot != -1 && (fireSlot != -1 || inOffHand) && !mc.player.onGround() && mc.player.fallDistance > 0) {
                if (ticks <= 0) {
                    if (ellySlot != -2) {
                        mc.gameMode.handleContainerInput(0, ellySlot, 1, ContainerInput.PICKUP, mc.player);
                        mc.gameMode.handleContainerInput(0, 6, 1, ContainerInput.PICKUP, mc.player);
                    }
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    int prevSlot = mc.player.getInventory().getSelectedSlot();
                    if (prevSlot != fireSlot && !inOffHand)
                        sendPacket(new ServerboundSetCarriedItemPacket(fireSlot));
                    mc.gameMode.useItem(mc.player, inOffHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                    if (prevSlot != fireSlot && !inOffHand)
                        sendPacket(new ServerboundSetCarriedItemPacket(prevSlot));

                    if (ellySlot != -2) {
                        mc.gameMode.handleContainerInput(0, 6, 1, ContainerInput.PICKUP, mc.player);
                        mc.gameMode.handleContainerInput(0, ellySlot, 1, ContainerInput.PICKUP, mc.player);
                    }
                    mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                    ticks = delay.getValue();
                }
            }
        }

        if (mode.getValue() == Mode.ElytraLowHop) {
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
                return;
            }
            if (mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().inflate(-0.29, 0, -0.29).move(0.0, -3, 0.0f)).iterator().hasNext() && elytraDelay.passedMs(150) && startDelay.passedMs(500)) {
                int elytra = InventoryUtility.getElytra();
                if (elytra == -1) disable(isRu() ? "Для этого режима нужна элитра!" : "You need elytra for this mode!");
                else Strafe.disabler(elytra);

                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), 0f, mc.player.getDeltaMovement().z());
                if (isMoving())
                    MovementUtility.setMotion(0.85);
                elytraDelay.reset();
            }
        }
    }

    @EventHandler
    public void onPostPlayerUpdate(PostPlayerUpdateEvent event) {
        if (mode.getValue() == Mode.MatrixDamage) {
            if (MovementUtility.isMoving() && mc.player.hurtTime > hurttime.getValue()) {
                if (mc.player.onGround()) {
                    MovementUtility.setMotion(0.387f * boostFactor.getValue());
                } else if (mc.player.isInWater()) {
                    MovementUtility.setMotion(0.346f * boostFactor.getValue());
                } else if (!mc.player.onGround() && allowOffGround.getValue()) {
                    MovementUtility.setMotion(0.448f * boostFactor.getValue());
                }

                if (shiftTicks.getValue() > 0) {
                    event.cancel();
                    event.setIterations(shiftTicks.getValue());
                }
            }
        }
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (mc.player.isInLiquid() && pauseInLiquids.getValue() || mc.player.isShiftKeyDown() && pauseWhileSneaking.getValue()) {
            return;
        }
        if (mode.getValue() != Mode.NCP && mode.getValue() != Mode.StrictStrafe) return;
        if (mc.player.getAbilities().flying) return;
        if (mc.player.isFallFlying()) return;
        if (mc.player.getFoodData().getFoodLevel() <= 6) return;
        if (event.isCancelled()) return;
        event.cancel();

        if (MovementUtility.isMoving()) {
            ThunderHack.TICK_TIMER = useTimer.getValue() ? 1.088f : 1f;
            float currentSpeed = mode.getValue() == Mode.NCP && mc.player.input.getMoveVector().y <= 0 && prevForward > 0 ? Managers.PLAYER.currentPlayerSpeed * 0.66f : Managers.PLAYER.currentPlayerSpeed;
            boolean canJump = !mc.player.horizontalCollision || ModuleManager.step.isDisabled();

            if (stage == 1 && mc.player.onGround() && canJump) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, MovementUtility.getJumpSpeed(), mc.player.getDeltaMovement().z);
                event.setY(MovementUtility.getJumpSpeed());
                baseSpeed *= 2.149;
                stage = 2;
            } else if (stage == 2) {
                baseSpeed = currentSpeed - (0.66 * (currentSpeed - MovementUtility.getBaseMoveSpeed()));
                stage = 3;
            } else {
                if ((mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, mc.player.getDeltaMovement().y(), 0.0)).iterator().hasNext() || mc.player.verticalCollision))
                    stage = 1;
                baseSpeed = currentSpeed - currentSpeed / 159.0D;
            }

            baseSpeed = Math.max(baseSpeed, MovementUtility.getBaseMoveSpeed());

            double ncpSpeed = mode.getValue() == Mode.StrictStrafe || mc.player.input.getMoveVector().y < 1 ? 0.465 : 0.576;
            double ncpBypassSpeed = mode.getValue() == Mode.StrictStrafe || mc.player.input.getMoveVector().y < 1 ? 0.44 : 0.57;

            if (mc.player.hasEffect(MobEffects.SPEED)) {
                double amplifier = mc.player.getEffect(MobEffects.SPEED).getAmplifier();
                ncpSpeed *= 1 + (0.2 * (amplifier + 1));
                ncpBypassSpeed *= 1 + (0.2 * (amplifier + 1));
            }

            if (mc.player.hasEffect(MobEffects.SLOWNESS)) {
                double amplifier = mc.player.getEffect(MobEffects.SLOWNESS).getAmplifier();
                ncpSpeed /= 1 + (0.2 * (amplifier + 1));
                ncpBypassSpeed /= 1 + (0.2 * (amplifier + 1));
            }

            baseSpeed = Math.min(baseSpeed, ticks > 25 ? ncpSpeed : ncpBypassSpeed);

            if (ticks++ > 50)
                ticks = 0;

            MovementUtility.modifyEventSpeed(event, baseSpeed);
            prevForward = mc.player.input.getMoveVector().y;
        } else {
            ThunderHack.TICK_TIMER = 1f;
            event.setX(0);
            event.setZ(0);
        }
    }
}
