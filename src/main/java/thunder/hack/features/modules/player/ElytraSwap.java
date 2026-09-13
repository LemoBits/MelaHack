package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.Bind;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class ElytraSwap extends Module {
    public ElytraSwap() {
        super("ElytraSwap", Category.PLAYER);
    }

    private final Setting<Boolean> delay = new Setting<>("Delay", false);
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Enable);
    private final Setting<Bind> switchButton = new Setting<>("SwitchButton", new Bind(-1, false, false), v -> mode.getValue() == Mode.Bind);
    private final Setting<Bind> fireWorkButton = new Setting<>("FireWorkButton", new Bind(-1, false, false), v -> mode.getValue() == Mode.Bind);
    private final Setting<Boolean> startFireWork = new Setting<>("StartFireWork", true, v -> mode.getValue() == Mode.Bind);
    private final Setting<FireWorkMode> fireWorkMode = new Setting<>("FireWorkMode", FireWorkMode.Normal, v -> mode.getValue() == Mode.Bind);

    private final Timer switchTimer = new Timer();
    private final Timer fireworkTimer = new Timer();

    public static boolean swapping = false;

    private enum Mode {
        Enable, Bind
    }

    private enum FireWorkMode {
        Silent, Normal
    }

    @Override
    public void onEnable() {
        if (mode.getValue() == Mode.Enable)
            swapChest(true);
    }

    @Override
    public void onUpdate() {
        if (mode.getValue() == Mode.Bind && mc.screen == null) {
            if (switchButton.getValue().getKey() != -1 && isKeyPressed(switchButton.getValue().getKey()) && switchTimer.every(500))
                swapChest(false);

            if (fireWorkButton.getValue().getKey() != -1 && isKeyPressed(fireWorkButton.getValue().getKey()) && fireworkTimer.every(500) && mc.player.isFallFlying())
                useFireWork();
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.SendPost e) {
        if (e.getPacket() instanceof ServerboundPlayerCommandPacket command
                && command.getAction() == ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
                && mode.getValue() == Mode.Bind
                && startFireWork.getValue()) {
            useFireWork();
        }
    }

    public void useFireWork() {
        SearchInvResult hotbarFireWorkResult = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET);
        SearchInvResult fireWorkResult = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET);

        InventoryUtility.saveSlot();
        if (hotbarFireWorkResult.found()) {
            hotbarFireWorkResult.switchTo();
        } else if (fireWorkResult.found()) {
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, fireWorkResult.slot(), mc.player.getInventory().getSelectedSlot(), ClickType.SWAP, mc.player);
            sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
        } else {
            sendMessage(isRu() ? "У тебя нет фейерверков!" : "You've got no fireworks!");
            return;
        }

        sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
        sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

        if (fireWorkMode.getValue() == FireWorkMode.Silent) {
            InventoryUtility.returnSlot();
            if (!hotbarFireWorkResult.found()) {
                mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, fireWorkResult.slot(), mc.player.getInventory().getSelectedSlot(), ClickType.SWAP, mc.player);
                sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            }
        }
    }

    public static int getChestPlateSlot() {
        Item[] items = {Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE};
        for (Item item : items) {
            SearchInvResult slot = InventoryUtility.findItemInInventory(item);
            if (slot.found()) {
                return slot.slot();
            }
        }
        return -1;
    }

    private void swapChest(boolean disable) {
        SearchInvResult result = InventoryUtility.findItemInInventory(Items.ELYTRA);


        if (mc.player.getInventory().getItem(38).getItem() == Items.ELYTRA) {
            int slot = getChestPlateSlot();
            if (slot != -1) {
                if (delay.getValue())
                    Managers.ASYNC.run(() -> {
                        swapping = true;
                        clickSlot(slot);
                        try {
                            Thread.sleep(200);
                        } catch (Exception ignored) {
                        }
                        clickSlot(6);
                        try {
                            Thread.sleep(200);
                        } catch (Exception ignored) {
                        }
                        clickSlot(slot);
                        sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                        swapping = false;
                    });
                else {
                    clickSlot(slot);
                    clickSlot(6);
                    clickSlot(slot);
                    sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                }
            } else {
                if (disable) disable(isRu() ? "У тебя нет нагрудника!" : "You don't have a chestplate!");
                else sendMessage(isRu() ? "У тебя нет нагрудника!" : "You don't have a chestplate!");
                return;
            }
        } else if (result.found()) {
            if (delay.getValue())
                new Thread(() -> {
                    swapping = true;
                    clickSlot(result.slot());
                    try {
                        Thread.sleep(200);
                    } catch (Exception ignored) {
                    }
                    clickSlot(6);
                    try {
                        Thread.sleep(200);
                    } catch (Exception ignored) {
                    }
                    clickSlot(result.slot());
                    sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                    if (startFireWork.getValue() && mc.player.fallDistance > 0)
                        sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    swapping = false;
                }).start();
            else {
                clickSlot(result.slot());
                clickSlot(6);
                clickSlot(result.slot());
                sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                if (startFireWork.getValue() && mc.player.fallDistance > 0)
                    sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
        } else {
            if (disable) disable(isRu() ? "У тебя нет элитры!" : "You don't have an elytra!");
            else sendMessage(isRu() ? "У тебя нет элитры!" : "You don't have an elytra!");
            return;
        }

        if (disable)
            disable(isRu() ? "Свапнул! Отключаю.." : "Swapped! Disabling..");
    }
}
