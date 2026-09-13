package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import thunder.hack.events.impl.EventKeyboardInput;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.player.MovementUtility;

public class NoSlow extends Module {
    public NoSlow() {
        super("NoSlow", Category.MOVEMENT);
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.NCP);
    private final Setting<Boolean> mainHand = new Setting<>("MainHand", true);
    private final Setting<SettingGroup> selection = new Setting<>("Selection", new SettingGroup(false, 0));
    private final Setting<Boolean> food = new Setting<>("Food", true).addToGroup(selection);
    private final Setting<Boolean> projectiles = new Setting<>("Projectiles", true).addToGroup(selection);
    private final Setting<Boolean> shield = new Setting<>("Shield", true).addToGroup(selection);
    public final Setting<Boolean> soulSand = new Setting<>("SoulSand", true).addToGroup(selection);
    public final Setting<Boolean> honey = new Setting<>("Honey", true).addToGroup(selection);
    public final Setting<Boolean> slime = new Setting<>("Slime", true).addToGroup(selection);
    public final Setting<Boolean> ice = new Setting<>("Ice", true).addToGroup(selection);
    public final Setting<Boolean> sweetBerryBush = new Setting<>("SweetBerryBush", true).addToGroup(selection);
    public final Setting<Boolean> sneak = new Setting<>("Sneak", false).addToGroup(selection);
    public final Setting<Boolean> crawl = new Setting<>("Crawl", false).addToGroup(selection);

    private boolean returnSneak;

    @Override
    public void onUpdate() {
        if (returnSneak) {
            mc.options.keyShift.setDown(false);
            mc.player.setSprinting(true);
            returnSneak = false;
        }

        if (mc.player.isUsingItem() && !mc.player.isHandsBusy() && !mc.player.isFallFlying()) {
            switch (mode.getValue()) {
                case StrictNCP -> sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
                case MusteryGrief -> {
                    if (mc.player.onGround() && mc.options.keyJump.isDown()) {
                        mc.options.keyShift.setDown(true);
                        returnSneak = true;
                    }
                }
                case Grim -> {
                    if (mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                        sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot() % 8 + 1));
                        sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot() % 7 + 2));
                        sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
                    } else if (mainHand.getValue()) {
                        // TODO rotations
                        sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                    }
                }
                case Matrix -> {
                    if (mc.player.onGround() && !mc.options.keyJump.isDown()) {
                        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * 0.3, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z * 0.3);
                    } else if (mc.player.fallDistance > 0.2f)
                        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * 0.95f, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z * 0.95f);
                }
                case GrimNew -> {
                    if (mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                        sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot() % 8 + 1));
                        sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot() % 7 + 2));
                        sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
                    } else if (mainHand.getValue() && (mc.player.getTicksUsingItem() <= 3 || mc.player.tickCount % 2 == 0)) {
                        sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                    }
                }
                case Matrix2 -> {
                    if (mc.player.onGround())
                        if (mc.player.tickCount % 2 == 0)
                            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * 0.5f, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z * 0.5f);
                        else
                            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * 0.95f, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z * 0.95f);
                }
                case LFCraft -> {
                    if (mc.player.getTicksUsingItem() <= 3)
                        sendSequencedPacket(id -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, mc.player.blockPosition().above(), Direction.NORTH, id));
                }
            }
        }
    }

    @EventHandler
    public void onKeyboardInput(EventKeyboardInput e) {
        if (mode.getValue() == Mode.Matrix3 && mc.player.isUsingItem() && !mc.player.isFallFlying()) {
            MovementUtility.scaleMovementInput(5f, 5f);
            float mult = 1f;

            if (mc.player.onGround()) {
                if (mc.player.input.getMoveVector().y != 0 && mc.player.input.getMoveVector().x != 0) {
                    MovementUtility.scaleMovementInput(0.35f, 0.35f);
                } else {
                    MovementUtility.scaleMovementInput(0.5f, 0.5f);
                }
            } else {
                if (mc.player.input.getMoveVector().y != 0 && mc.player.input.getMoveVector().x != 0) {
                    mult = 0.47f;
                } else {
                    mult = 0.67f;
                }
            }
            MovementUtility.scaleMovementInput(mult, mult);
        }
    }

    public boolean canNoSlow() {
        if (mode.getValue() == Mode.Matrix3)
            return false;

        if (!food.getValue() && mc.player.getUseItem().getComponents().has(DataComponents.FOOD))
            return false;

        if (!shield.getValue() && mc.player.getUseItem().getItem() == Items.SHIELD)
            return false;

        if (!projectiles.getValue()
                && (mc.player.getUseItem().getItem() == Items.CROSSBOW || mc.player.getUseItem().getItem() == Items.BOW || mc.player.getUseItem().getItem() == Items.TRIDENT))
            return false;

        if (mode.getValue() == Mode.MusteryGrief && mc.player.onGround() && !mc.options.keyJump.isDown())
            return false;

        if (!mainHand.getValue() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND)
            return false;

        if ((mc.player.getOffhandItem().getComponents().has(DataComponents.FOOD) || mc.player.getOffhandItem().getItem() == Items.SHIELD)
                && (mode.getValue() == Mode.GrimNew || mode.getValue() == Mode.Grim) && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND)
            return false;

        return true;
    }

    public enum Mode {
        NCP, StrictNCP, Matrix, Grim, MusteryGrief, GrimNew, Matrix2, LFCraft, Matrix3
    }
}
