package thunder.hack.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import thunder.hack.events.Event;

public class EventHeldItemRenderer extends Event {
    private final InteractionHand hand;
    private final ItemStack item;
    private float ep;
    private final PoseStack stack;

    public EventHeldItemRenderer(InteractionHand hand, ItemStack item, float equipProgress, PoseStack stack) {
        this.hand = hand;
        this.item = item;
        this.ep = equipProgress;
        this.stack = stack;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getItem() {
        return item;
    }

    public float getEp() {
        return ep;
    }

    public PoseStack getStack() {
        return stack;
    }
}