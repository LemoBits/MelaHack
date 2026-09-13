package thunder.hack.features.modules.misc;

import com.google.common.collect.Lists;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.clickui.ClickGUI;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;

import java.util.Comparator;
import java.util.HashMap;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class AutoTrader extends Module {
    public AutoTrader() {
        super("AutoTrader", Category.MISC);
    }

    private final Setting<BooleanSettingGroup> buy = new Setting<>("Buy", new BooleanSettingGroup(true));
    private final Setting<String> buyItem = new Setting<>("BuyItem", "apple").addToGroup(buy);
    private final Setting<BooleanSettingGroup> sell = new Setting<>("Sell", new BooleanSettingGroup(false));
    private final Setting<String> sellItem = new Setting<>("SellItem", "bread").addToGroup(sell);
    private final Setting<SettingGroup> disable = new Setting<>("Disable", new SettingGroup(false, 0));
    private final Setting<Boolean> noVillagers = new Setting<>("NoVillagers", true).addToGroup(disable);
    private final Setting<Boolean> noItems = new Setting<>("NoItems", false).addToGroup(disable);

    private int interactTicks, cooldown;
    private int lastVillager;
    private HashMap<Integer, Integer> villagers = new HashMap<>();

    @Override
    public void onUpdate() {
        if (fullNullCheck())
            return;

        if (interactTicks > 0)
            interactTicks--;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        HashMap<Integer, Integer> cacheVillagers = new HashMap<>(villagers);
        cacheVillagers.forEach((id, time) -> {
            if (mc.player.tickCount - time > 160)
                villagers.remove(id);
        });

        if (mc.screen instanceof MerchantScreen merch) {
            MerchantMenu msh = merch.getMenu();
            MerchantOffers offers = msh.getOffers();

            for (int i = 0; i < offers.size(); i++) {
                MerchantOffer offer = offers.get(i);
                if (goodDeal(offer)) {
                    msh.tryMoveItems(i);
                    msh.setSelectionHint(i);
                    sendPacket(new ServerboundSelectTradePacket(i));
                    clickSlot(2, ClickType.QUICK_MOVE);
                    cooldown = 3;
                    return;
                } else if (!msh.getSlot(0).getItem().isEmpty()) {
                    clickSlot(0, ClickType.QUICK_MOVE);
                    cooldown = 3;
                    return;
                } else if (!msh.getSlot(1).getItem().isEmpty()) {
                    clickSlot(1, ClickType.QUICK_MOVE);
                    cooldown = 3;
                    return;
                } else if (offer.isOutOfStock()) {
                    villagers.put(lastVillager, mc.player.tickCount);
                }
            }
            mc.player.closeContainer();
        } else if (interactTicks <= 0 && !(mc.screen instanceof ClickGUI)) {
            Entity ent = Lists.newArrayList(mc.level.entitiesForRendering()).stream()
                    .filter(e -> (e instanceof Villager))
                    .filter(e -> mc.player.distanceToSqr(e) < 4f * 4f)
                    .filter(e -> !villagers.containsKey(e.getId()))
                    .min(Comparator.comparing(e -> mc.player.distanceTo(e))).orElse(null);

            if (ent != null) {
                float[] angles = InteractionUtility.calculateAngle(ent.getEyePosition().add(Math.random() * 0.2, 0, Math.random() * 0.2));
                mc.player.setYRot(angles[0]);
                mc.player.setXRot(angles[1]);
                mc.gameMode.interact(mc.player, ent, InteractionHand.MAIN_HAND);
                lastVillager = ent.getId();
                interactTicks = 12;
            } else if (noVillagers.getValue())
                disable(isRu() ? "Рядом нет жителей!" : "There are no villagers nearby!");
        }
    }

    private boolean goodDeal(MerchantOffer offer) {
        boolean selectedBuyItem = (offer.getResult().getItem().getDescriptionId().equals("item.minecraft." + buyItem.getValue())
                || offer.getResult().getItem().getDescriptionId().equals("block.minecraft." + buyItem.getValue()));

        boolean selectedSellItem = (offer.getCostA().getItem().getDescriptionId().equals("item.minecraft." + sellItem.getValue())
                || offer.getCostA().getItem().getDescriptionId().equals("block.minecraft." + sellItem.getValue()));

        boolean haveItems = offer.getCostA().getCount() <= InventoryUtility.getItemCount(offer.getCostA().getItem());

        boolean canBuy = selectedBuyItem && !offer.isOutOfStock() && buy.getValue().isEnabled();

        boolean canSell = selectedSellItem && !offer.isOutOfStock() && sell.getValue().isEnabled();

        if ((canBuy || canSell) && !haveItems) {
            if (noItems.getValue())
                disable(isRu() ? "Кончились предметы!" : "Out of items!");
            return false;
        }

        return canBuy || canSell;
    }
}
