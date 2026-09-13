package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.AsyncManager;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.cmd.Command;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.PlayerUtility;

import java.util.function.Consumer;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class MiddleClick extends Module {
    public MiddleClick() {
        super("MiddleClick", Category.MISC);
    }

    private final Setting<Action> onBlock = new Setting<>("OnBlock", Action.Xp);
    private final Setting<Action> onAir = new Setting<>("OnAir", Action.Pearl);
    private final Setting<Action> onEntity = new Setting<>("OnEntity", Action.Friend);
    private final Setting<Action> onFlying = new Setting<>("OnElytra", Action.Firework);
    private final Setting<Boolean> silent = new Setting<>("Silent", true);
    private final Setting<Boolean> inventory = new Setting<>("Inventory", true);
    private final Setting<Integer> swapDelay = new Setting<>("SwapDelay", 100, 0, 1000, v -> !silent.getValue());
    private final Setting<BooleanSettingGroup> antiWaste = new Setting<>("AntiWaste", new BooleanSettingGroup(true));
    private final Setting<Integer> durability = new Setting<>("StopOn", 90, 0, 100).addToGroup(antiWaste);
    public final Setting<Boolean> antiPickUp = new Setting<>("AntiPickUp", true);
    private final Setting<Boolean> feetExp = new Setting<>("FeetXP", false);

    private static final Timer timer = new Timer();
    private String state = "none";

    @Override
    public String getDisplayInfo() {
        return state;
    }

    @EventHandler
    private void onSync(EventSync event) {
        if (mc.screen == null) {
            HitResult target = mc.hitResult;

            if (mc.player.isFallFlying()) {
                if (mc.options.keyPickItem.isDown())
                    onFlying.getValue().doAction(event);
                state = onFlying.getValue().toString();
                return;
            }

            if (target instanceof EntityHitResult ehr && ehr.getEntity() instanceof Player) {
                if (mc.options.keyPickItem.isDown())
                    onEntity.getValue().doAction(event);
                state = onEntity.getValue().toString();
                return;
            }

            if (target instanceof BlockHitResult bhr) {
                if (mc.level.isEmptyBlock(bhr.getBlockPos())) {
                    if (mc.options.keyPickItem.isDown())
                        onAir.getValue().doAction(event);
                    state = onAir.getValue().toString();
                } else {
                    if (mc.options.keyPickItem.isDown())
                        onBlock.getValue().doAction(event);
                    state = onBlock.getValue().toString();
                }
                return;
            }
            if (mc.options.keyPickItem.isDown())
                onAir.getValue().doAction(event);
            state = onAir.getValue().toString();
        }
    }

    private static boolean needXp() {
        for (ItemStack stack : thunder.hack.utility.player.ArmorUtility.getArmorItems(mc.player))
            if (PlayerUtility.calculatePercentage(stack) < ModuleManager.middleClick.durability.getValue())
                return true;

        return false;
    }

    private static int getHpSlot() {
        for (int i = 0; i < 9; ++i)
            if (isStackPotion(mc.player.getInventory().getItem(i)))
                return i;
        return -1;
    }

    private static int findHpInInventory() {
        for (int i = 36; i >= 0; i--)
            if (isStackPotion(mc.player.getInventory().getItem(i)))
                return i < 9 ? i + 36 : i;
        return -1;
    }

    private static boolean isStackPotion(ItemStack stack) {
        if (stack == null)
            return false;

        if (stack.getItem() == Items.SPLASH_POTION) {
            PotionContents potionContentsComponent = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            for (MobEffectInstance effect : potionContentsComponent.getAllEffects())
                if (effect.getEffect().value() == MobEffects.INSTANT_HEALTH.value())
                    return true;
        }
        return false;
    }

    public static class PearlThread extends Thread {
        public LocalPlayer player;
        int epSlot, originalSlot, delay;
        boolean inv;

        public PearlThread(LocalPlayer entityPlayer, int epSlot, int originalSlot, int delay, boolean inventory) {
            this.player = entityPlayer;
            this.epSlot = epSlot;
            this.originalSlot = originalSlot;
            this.delay = delay;
            inv = inventory;
        }

        @Override
        public void run() {
            if (!inv) {
                InventoryUtility.switchTo(epSlot);
                AsyncManager.sleep(delay);
                InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                AsyncManager.sleep(delay);
                InventoryUtility.switchTo(originalSlot);
            } else {
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, epSlot, originalSlot, ContainerInput.SWAP, mc.player);
                AsyncManager.sleep(delay);
                if (ModuleManager.aura.isEnabled() && Aura.target != null)
                    mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));
                InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                AsyncManager.sleep(delay);
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, epSlot, originalSlot, ContainerInput.SWAP, mc.player);
            }
            super.run();
        }
    }

    public enum Action {
        HealingPot((EventSync e) -> {
            if (timer.every(250)) {
                if (getHpSlot() != -1) {
                    int hpSlot = getHpSlot();
                    int originalSlot = mc.player.getInventory().getSelectedSlot();
                    if (hpSlot != -1) {
                        InventoryUtility.switchTo(hpSlot);
                        InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                        InventoryUtility.switchTo(originalSlot);
                    }
                } else {
                    int hpSlot = findHpInInventory();
                    if (hpSlot != -1) {
                        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, hpSlot, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                        InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                        mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, hpSlot, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                    }
                }
            }
        }),

        Firework((EventSync e) -> {
            if (timer.every(250)) {
                int epSlot1 = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET).slot();
                if (ModuleManager.middleClick.silent.getValue()) {
                    if (!ModuleManager.middleClick.inventory.getValue() || (ModuleManager.middleClick.inventory.getValue() && epSlot1 != -1)) {
                        int originalSlot = mc.player.getInventory().getSelectedSlot();
                        if (epSlot1 != -1) {
                            mc.player.getInventory().setSelectedSlot(epSlot1);
                            mc.player.connection.send(new ServerboundSetCarriedItemPacket(epSlot1));
                            InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                            mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                            mc.player.getInventory().setSelectedSlot(originalSlot);
                            mc.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
                        }
                    } else {
                        int epSlot = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).slot();
                        if (epSlot != -1) {
                            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, epSlot, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                            InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                            mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, epSlot, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                        }
                    }
                } else {
                    if (!ModuleManager.middleClick.inventory.getValue() || (ModuleManager.middleClick.inventory.getValue() && epSlot1 != -1)) {
                        if (epSlot1 != -1)
                            new PearlThread(mc.player, epSlot1, mc.player.getInventory().getSelectedSlot(), ModuleManager.middleClick.swapDelay.getValue(), false).start();
                    } else {
                        int epSlot = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).slot();
                        if (epSlot != -1)
                            new PearlThread(mc.player, epSlot, mc.player.getInventory().getSelectedSlot(), ModuleManager.middleClick.swapDelay.getValue(), true).start();
                    }
                }
            }
        }),

        Friend((EventSync e) -> {
            if (timer.every(800)) {
                if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof Player)
                    if (Managers.FRIEND.isFriend(ehr.getEntity().getName().getString())) {
                        Managers.FRIEND.removeFriend(ehr.getEntity().getName().getString());
                        Command.sendMessage(isRu() ? "§b" + ehr.getEntity().getName().getString() + "§r удален из друзей!" : "§b" + ehr.getEntity().getName().getString() + "§r removed from friends!");
                    } else {
                        Managers.FRIEND.addFriend(ehr.getEntity().getName().getString());
                        Command.sendMessage(isRu() ? "Добавлен друг §b" + ehr.getEntity().getName().getString() : "Added friend §b" + ehr.getEntity().getName().getString());
                    }
            }
        }),

        Pearl((EventSync e) -> {
            if (timer.every(500)) {
                if (ModuleManager.aura.isEnabled() && Aura.target != null)
                    mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));

                int epSlot1 = InventoryUtility.findItemInHotBar(Items.ENDER_PEARL).slot();
                if (ModuleManager.middleClick.silent.getValue()) {
                    if (!ModuleManager.middleClick.inventory.getValue() || (ModuleManager.middleClick.inventory.getValue() && epSlot1 != -1)) {
                        int originalSlot = mc.player.getInventory().getSelectedSlot();
                        if (epSlot1 != -1) {
                            mc.player.getInventory().setSelectedSlot(epSlot1);
                            mc.player.connection.send(new ServerboundSetCarriedItemPacket(epSlot1));
                            InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                            mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                            mc.player.getInventory().setSelectedSlot(originalSlot);
                            mc.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
                        }
                    } else {
                        int epSlot = InventoryUtility.findItemInInventory(Items.ENDER_PEARL).slot();
                        if (epSlot != -1) {
                            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, epSlot, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                            InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                            mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, epSlot, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                        }
                    }
                } else {
                    if (!ModuleManager.middleClick.inventory.getValue() || (ModuleManager.middleClick.inventory.getValue() && epSlot1 != -1)) {
                        if (epSlot1 != -1)
                            new PearlThread(mc.player, epSlot1, mc.player.getInventory().getSelectedSlot(), ModuleManager.middleClick.swapDelay.getValue(), false).start();
                    } else {
                        int epSlot = InventoryUtility.findItemInInventory(Items.ENDER_PEARL).slot();
                        if (epSlot != -1)
                            new PearlThread(mc.player, epSlot, mc.player.getInventory().getSelectedSlot(), ModuleManager.middleClick.swapDelay.getValue(), true).start();
                    }
                }
            }
        }),

        Xp((EventSync e) -> {
            if (ModuleManager.middleClick.feetExp.getValue())
                if (!ModuleManager.middleClick.antiWaste.getValue().isEnabled() || needXp())
                    mc.player.setXRot(90);

            e.addPostAction(() -> {
                if (ModuleManager.middleClick.antiWaste.getValue().isEnabled() && !needXp()) return;
                if (mc.options.keyPickItem.isDown()) {
                    int slot = InventoryUtility.findItemInHotBar(Items.EXPERIENCE_BOTTLE).slot();
                    if (slot != -1) {
                        int lastSlot = mc.player.getInventory().getSelectedSlot();
                        InventoryUtility.switchTo(slot);
                        InteractionUtility.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                        if (ModuleManager.middleClick.silent.getValue())
                            InventoryUtility.switchTo(lastSlot);
                    }
                }
            });
        }),

        None((EventSync e) -> {
        });

        private final Consumer<EventSync> r;

        Action(Consumer<EventSync> r) {
            this.r = r;
        }

        public void doAction(EventSync e) {
            r.accept(e);
        }
    }
}
