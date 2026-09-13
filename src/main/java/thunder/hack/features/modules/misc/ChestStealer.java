package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.Religion;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ItemSelectSetting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;

import java.util.ArrayList;
import java.util.Random;

import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.features.modules.render.StorageEsp.getBlockEntities;

import com.mojang.blaze3d.vertex.PoseStack;

public class ChestStealer extends Module {
    public ChestStealer() {
        super("ChestStealer", Category.MISC);
    }

    public final Setting<ItemSelectSetting> items = new Setting<>("Items", new ItemSelectSetting(new ArrayList<>()));
    private final Setting<Integer> delay = new Setting<>("Delay", 100, 0, 1000);
    private final Setting<Boolean> random = new Setting<>("Random", false);
    private final Setting<Boolean> close = new Setting<>("Close", false);
    private final Setting<Boolean> autoMyst = new Setting<>("AutoMyst", false);
    private final Setting<Sort> sort = new Setting<>("Sort", Sort.None);

    private final Timer autoMystDelay = new Timer();
    private final Timer timer = new Timer();
    private final Random rnd = new Random();

    @Override
    public void onEnable() {
        if (ModuleManager.religion.isOn() && ModuleManager.religion.ReligionSetting.is(Religion.YourReligion.Christianity)) {
            ModuleManager.religion.sendMessage(isRu() ? "Не укради!" : "Do not steal!");
            disable();
        }
    }

    public void onRender3D(PoseStack stack) {
        if (mc.player.containerMenu instanceof ChestMenu chest) {
            for (int i = 0; i < chest.getContainer().getContainerSize(); i++) {
                Slot slot = chest.getSlot(i);
                if (slot.hasItem() && isAllowed(slot.getItem())
                        && timer.every(delay.getValue() + (random.getValue() && delay.getValue() != 0 ? rnd.nextInt(delay.getValue()) : 0))
                        && !(mc.gui.screen().getTitle().getString().contains("Аукцион") || mc.gui.screen().getTitle().getString().contains("покупки"))) {
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
                    autoMystDelay.reset();
                }
            }
            if (isContainerEmpty(chest) && close.getValue())
                mc.player.closeContainer();
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (autoMyst.getValue() && mc.gui.screen() == null && autoMystDelay.passedMs(3000)) {
            for (BlockEntity be : getBlockEntities()) {
                if (be instanceof EnderChestBlockEntity) {
                    if (mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(be.getBlockPos())) > 39)
                        continue;
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(net.minecraft.world.phys.Vec3.atBottomCenterOf(be.getBlockPos()).add(MathUtility.random(-0.4, 0.4), 0.375, MathUtility.random(-0.4, 0.4)), Direction.UP, be.getBlockPos(), false));
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    break;
                }
            }
        }
    }

    private boolean isAllowed(ItemStack stack) {
        boolean allowed = items.getValue().contains(stack.getItem().getDescriptionId().replace("block.minecraft.", "").replace("item.minecraft.", ""));
        return switch (sort.getValue()) {
            case None -> true;
            case WhiteList -> allowed;
            default -> !allowed;
        };
    }

    private boolean isContainerEmpty(ChestMenu container) {
        for (int i = 0; i < (container.getContainer().getContainerSize() == 90 ? 54 : 27); i++)
            if (container.getSlot(i).hasItem()) return false;
        return true;
    }

    private enum Sort {None, WhiteList, BlackList}
}