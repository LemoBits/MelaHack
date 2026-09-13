package thunder.hack.features.modules.player;

import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.clickui.ClickGUI;
import thunder.hack.gui.hud.HudEditorGui;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.MovementUtility;

import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class AutoArmor extends Module {
    public AutoArmor() {
        super("AutoArmor", Category.PLAYER);
    }

    private final Setting<EnchantPriority> head = new Setting<>("Head", EnchantPriority.Protection);
    private final Setting<EnchantPriority> body = new Setting<>("Body", EnchantPriority.Protection);
    private final Setting<EnchantPriority> tights = new Setting<>("Tights", EnchantPriority.Protection);
    private final Setting<EnchantPriority> feet = new Setting<>("Feet", EnchantPriority.Protection);
    private final Setting<ElytraPriority> elytraPriority = new Setting<>("ElytraPriority", ElytraPriority.Ignore);
    private final Setting<Integer> delay = new Setting<>("Delay", 5, 0, 10);
    private final Setting<Boolean> oldVersion = new Setting<>("OldVersion", false);
    private final Setting<Boolean> pauseInventory = new Setting<>("PauseInventory", false);
    private final Setting<Boolean> noMove = new Setting<>("NoMove", false);
    private final Setting<Boolean> ignoreCurse = new Setting<>("IgnoreCurse", true);
    private final Setting<Boolean> strict = new Setting<>("Strict", false);

    private int tickDelay = 0;

    List<ArmorData> armorList = Arrays.asList(
            new ArmorData(EquipmentSlot.FEET, 36, -1, -1, -1),
            new ArmorData(EquipmentSlot.LEGS, 37, -1, -1, -1),
            new ArmorData(EquipmentSlot.CHEST, 38, -1, -1, -1),
            new ArmorData(EquipmentSlot.HEAD, 39, -1, -1, -1)
    );

    @Override
    public void onUpdate() {
        if (mc.gui.screen() != null && pauseInventory.getValue() && !(mc.gui.screen() instanceof ChatScreen) && !(mc.gui.screen() instanceof ClickGUI) && !(mc.gui.screen() instanceof HudEditorGui))
            return;

        if (tickDelay-- > 0)
            return;

        armorList.forEach(ArmorData::reset);

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            int prot = getProtection(stack);
            if (prot > 0)
                for (ArmorData e : armorList) {
                    if (e.getEquipmentSlot() == mc.player.getEquipmentSlotForItem(stack))
                        if (prot > e.getPrevProt() && prot > e.getNewProtection()) {
                            e.setNewSlot(i);
                            e.setNewProtection(prot);
                        }
                }
        }

        for (ArmorData armorPiece : armorList) {
            int slot = armorPiece.getNewSlot();
            if (slot != -1) {
                if ((armorPiece.getPrevProt() == -1 || !oldVersion.getValue()) && slot < 9) {
                    InventoryUtility.saveAndSwitchTo(slot);
                    sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                    InventoryUtility.returnSlot();
                } else {
                    if (MovementUtility.isMoving() && noMove.getValue())
                        return;

                    int newArmorSlot = slot < 9 ? 36 + slot : slot;

                    if(strict.getValue())
                        sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));

                    clickSlot(newArmorSlot);
                    clickSlot((armorPiece.getArmorSlot() - 34) + (39 - armorPiece.getArmorSlot()) * 2);
                    if (armorPiece.getPrevProt() != -1)
                        clickSlot(newArmorSlot);

                    sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                }

                tickDelay = delay.getValue();
                return;
            }
        }
    }

    private int getProtection(ItemStack is) {
        if (mc.player.getEquipmentSlotForItem(is).isArmor() || is.is(Items.ELYTRA)) {
            int prot = 0;

            EquipmentSlot slot = mc.player.getEquipmentSlotForItem(is);

            if (is.is(Items.ELYTRA)) {
                if (!isUsableElytra(is))
                    return 0;

                boolean ePlus = elytraPriority.is(ElytraPriority.ElytraPlus) && (ModuleManager.elytraRecast.isEnabled() || ModuleManager.elytraPlus.isEnabled());
                boolean ignore = elytraPriority.is(ElytraPriority.Ignore) && mc.player.getInventory().getItem(38).is(Items.ELYTRA);

                if (ePlus || ignore || elytraPriority.is(ElytraPriority.Always))
                    prot = 999;
            }

            int blastMultiplier = 1;
            int protectionMultiplier = 1;

            switch (slot) {
                case HEAD -> {
                    if(head.is(EnchantPriority.Protection)) protectionMultiplier *= 2;
                    else blastMultiplier *= 2;
                }
                case BODY -> {
                    if(body.is(EnchantPriority.Protection)) protectionMultiplier *= 2;
                    else blastMultiplier *= 2;
                }
                case LEGS -> {
                    if(tights.is(EnchantPriority.Protection)) protectionMultiplier *= 2;
                    else blastMultiplier *= 2;
                }
                case FEET -> {
                    if(feet.is(EnchantPriority.Protection)) protectionMultiplier *= 2;
                    else blastMultiplier *= 2;
                }
            }

            if (is.isEnchanted()) {
                ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(is);

                var enchantments = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var protection = enchantments.getOrThrow(Enchantments.PROTECTION);
                var blastProtection = enchantments.getOrThrow(Enchantments.BLAST_PROTECTION);
                var bindingCurse = enchantments.getOrThrow(Enchantments.BINDING_CURSE);

                if (enchants.keySet().contains(protection))
                    prot += enchants.getLevel(protection) * protectionMultiplier;

                if (enchants.keySet().contains(blastProtection))
                    prot += enchants.getLevel(blastProtection) * blastMultiplier;

                if (enchants.keySet().contains(bindingCurse) && ignoreCurse.getValue())
                    prot = -999;
            }

            if (slot.isArmor()) {
                final double[] armorValues = new double[2];
                is.forEachModifier(slot, (attribute, modifier) -> {
                    if (attribute.is(Attributes.ARMOR)) {
                        armorValues[0] += modifier.amount();
                    } else if (attribute.is(Attributes.ARMOR_TOUGHNESS)) {
                        armorValues[1] += modifier.amount();
                    }
                });
                int base = (int) Math.ceil(armorValues[0] + armorValues[1]);
                return base * 10 + prot;
            }
            return prot;
        } else if (!is.isEmpty()) return 0;
        return -1;
    }

    private boolean isUsableElytra(ItemStack stack) {
        return LivingEntity.canGlideUsing(stack, EquipmentSlot.CHEST);
    }

    public class ArmorData {
        private EquipmentSlot equipmentSlot;
        private int armorSlot, prevProtection, newSlot, newProtection;

        public ArmorData(EquipmentSlot equipmentSlot, int armorSlot, int prevProtection, int newSlot, int newProtection) {
            this.equipmentSlot = equipmentSlot;
            this.armorSlot = armorSlot;
            this.prevProtection = prevProtection;
            this.newSlot = newSlot;
            this.newProtection = newProtection;
        }

        public int getArmorSlot() {
            return armorSlot;
        }

        public int getPrevProt() {
            return prevProtection;
        }

        public void setPrevProt(int prevProtection) {
            this.prevProtection = prevProtection;
        }

        public int getNewSlot() {
            return newSlot;
        }

        public void setNewSlot(int newSlot) {
            this.newSlot = newSlot;
        }

        public int getNewProtection() {
            return newProtection;
        }

        public void setNewProtection(int newProtection) {
            this.newProtection = newProtection;
        }

        public EquipmentSlot getEquipmentSlot() {
            return equipmentSlot;
        }

        public void reset() {
            setPrevProt(getProtection(mc.player.getInventory().getItem(getArmorSlot())));
            setNewSlot(-1);
            setNewProtection(-1);
        }
    }

    private enum ElytraPriority {
        None, Always, ElytraPlus, Ignore
    }

    private enum EnchantPriority {
        Blast, Protection
    }
}
