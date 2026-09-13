package thunder.hack.features.hud.impl;
import org.apache.commons.lang3.StringUtils;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderer;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.math.FrameRateCounter;
import thunder.hack.utility.math.MathUtility;
import java.util.List;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static thunder.hack.features.hud.impl.PotionHud.getDuration;

public class LegacyHud extends Module {
    public LegacyHud() {
        super("LegacyHud", Category.HUD);
    }

    private static final ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);

    private final Setting<Font> customFont = new Setting<>("Font", Font.Minecraft);
    private final Setting<ColorSetting> colorSetting = new Setting<>("Color", new ColorSetting(new Color(0x0077FF)));
    private final Setting<Boolean> renderingUp = new Setting<>("RenderingUp", false);
    private final Setting<Boolean> waterMark = new Setting<>("Watermark", false);
    private final Setting<Boolean> arrayList = new Setting<>("ActiveModules", false);
    private final Setting<Boolean> coords = new Setting<>("Coords", false);
    private final Setting<Boolean> direction = new Setting<>("Direction", false);
    private final Setting<Boolean> armor = new Setting<>("Armor", false);
    private final Setting<Boolean> totems = new Setting<>("Totems", false);
    private final Setting<Boolean> greeter = new Setting<>("Welcomer", false);
    private final Setting<Boolean> speed = new Setting<>("Speed", false);
    private final Setting<Boolean> bps = new Setting<>("BPS", false, v -> speed.getValue());
    public final Setting<Boolean> potions = new Setting<>("Potions", false);
    private final Setting<Boolean> ping = new Setting<>("Ping", false);
    private final Setting<Boolean> tps = new Setting<>("TPS", false);
    private final Setting<Boolean> extraTps = new Setting<>("ExtraTPS", true, v -> tps.getValue());
    private final Setting<Boolean> offhandDurability = new Setting<>("OffhandDurability", false);
    private final Setting<Boolean> mainhandDurability = new Setting<>("MainhandDurability", false);
    private final Setting<Boolean> fps = new Setting<>("FPS", false);
    private final Setting<Boolean> chests = new Setting<>("Chests", false);
    private final Setting<Boolean> worldTime = new Setting<>("WorldTime", false);
    private final Setting<Boolean> biome = new Setting<>("Biome", false);
    public Setting<Boolean> time = new Setting<>("Time", false);

    public Setting<Integer> waterMarkY = new Setting<>("WatermarkPosY", 2, 0, 20, v -> waterMark.getValue());
    private int color;

    private enum Font {
        Minecraft, Comfortaa, Monsterrat, SF
    }

    public void onRender2D(GuiGraphics context) {
        if (fullNullCheck())
            return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int offset;

        switch (customFont.getValue()) {
            case Minecraft -> offset = 10;
            case Monsterrat -> offset = 9;
            default -> offset = 8;
        }

        color = colorSetting.getValue().getColor();

        if (waterMark.getValue())
            drawText(context, "thunderhack v" + ThunderHack.VERSION, 2, waterMarkY.getValue());

        int j = (mc.screen instanceof ChatScreen && !renderingUp.getValue()) ? 14 : 0;

        if (arrayList.getValue())
            for (Module module : Managers.MODULE.getEnabledModules().stream().filter(Module::isDrawn).sorted(Comparator.comparing(module -> getStringWidth(module.getFullArrayString()) * -1)).toList()) {
                if (!module.isDrawn()) {
                    continue;
                }
                String str = module.getDisplayName() + ChatFormatting.GRAY + ((module.getDisplayInfo() != null) ? (" [" + ChatFormatting.WHITE + module.getDisplayInfo() + ChatFormatting.GRAY + "]") : "");
                if (renderingUp.getValue()) {
                    drawText(context, str, (width - 2 - getStringWidth(str)), (2 + j * offset));
                    j++;
                } else {
                    j += offset;
                    drawText(context, str, (width - 2 - getStringWidth(str)), (height - j));
                }
            }

        int i = (mc.screen instanceof ChatScreen && renderingUp.getValue()) ? 13 : (renderingUp.getValue() ? -2 : 0);

        if (potions.getValue()) {
            List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
            for (MobEffectInstance potionEffect : effects) {
                MobEffect potion = potionEffect.getEffect().value();
                String power = "";
                switch (potionEffect.getAmplifier()) {
                    case 0 -> power = "I";
                    case 1 -> power = "II";
                    case 2 -> power = "III";
                    case 3 -> power = "IV";
                    case 4 -> power = "V";
                }
                String s = potion.getDisplayName().getString() + " " + power;
                String s2 = getDuration(potionEffect) + "";
                Color c = new Color(potionEffect.getEffect().value().getColor());

                if (renderingUp.getValue()) {
                    i += offset;
                    drawText(context, s + " " + s2, (width - getStringWidth(s + " " + s2) - 2), (height - 2 - i), c.getRGB());
                } else {
                    drawText(context, s + " " + s2, (width - getStringWidth(s + " " + s2) - 2), (2 + i++ * offset), c.getRGB());
                }
            }
        }

        if(worldTime.getValue()) {
            String str2 = "WorldTime: " + ChatFormatting.WHITE + mc.level.getDayTime() % 24000;
            drawText(context, str2, width - getStringWidth(str2) - 2, renderingUp.getValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        if (mainhandDurability.getValue()) {
            String str = "MainHand" + ChatFormatting.WHITE +" [" +  (mc.player.getMainHandItem().getMaxDamage() - mc.player.getMainHandItem().getDamageValue()) + "]";
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.getValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }
        if (tps.getValue()) {
            String str = "TPS " + ChatFormatting.WHITE + Managers.SERVER.getTPS() + (extraTps.getValue() ? " [" + Managers.SERVER.getTPS2() + "]" : "");
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.getValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        if (speed.getValue()) {
            String str = "Speed " + ChatFormatting.WHITE + MathUtility.round(Managers.PLAYER.currentPlayerSpeed * (bps.getValue() ? 20f : 72f) * ThunderHack.TICK_TIMER) + (bps.getValue() ? " b/s" : " km/h");
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.getValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }
        if (chests.getValue()) {
            Tuple<Integer, Integer> chests = ModuleManager.chestCounter.getChestCount();
            String str = "Chests: " + ChatFormatting.WHITE + "S:" + chests.getA() + " D:" + chests.getB();
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.getValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }
        if(biome.getValue()) {
            String str3 = "Biome: " + ChatFormatting.WHITE + biome();
            drawText(context, str3, width - getStringWidth(str3) - 2, renderingUp.getValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        if (time.getValue()) {
            String str = "Time " + ChatFormatting.WHITE + (new SimpleDateFormat("h:mm a")).format(new Date());
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.getValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }
        if (offhandDurability.getValue()) {
            String str = "OffHand" + ChatFormatting.WHITE + " [" + (mc.player.getOffhandItem().getMaxDamage() - mc.player.getOffhandItem().getDamageValue()) + "]";
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.getValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }
        if (ping.getValue()) {
            String str1 = "Ping " + ChatFormatting.WHITE + Managers.SERVER.getPing();
            drawText(context, str1, width - getStringWidth(str1) - 2, renderingUp.getValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        if (fps.getValue()) {
            String fpsText = "FPS " + ChatFormatting.WHITE + FrameRateCounter.INSTANCE.getFps();
            drawText(context, fpsText, width - getStringWidth(fpsText) - 2, renderingUp.getValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        boolean inHell = Objects.equals(mc.level.dimension().identifier().getPath(), "the_nether");
        int posX = (int) mc.player.getX();
        int posY = (int) mc.player.getY();
        int posZ = (int) mc.player.getZ();
        float nether = !inHell ? 0.125F : 8.0F;
        int hposX = (int) (mc.player.getX() * nether);
        int hposZ = (int) (mc.player.getZ() * nether);
        i = (mc.screen instanceof ChatScreen) ? 14 : 0;
        String coordinates = ChatFormatting.WHITE + "XYZ " + ChatFormatting.RESET + (inHell ? (posX + ", " + posY + ", " + posZ + ChatFormatting.WHITE + " [" + ChatFormatting.RESET + hposX + ", " + hposZ + ChatFormatting.WHITE + "]" + ChatFormatting.RESET) : (posX + ", " + posY + ", " + posZ + ChatFormatting.WHITE + " [" + ChatFormatting.RESET + hposX + ", " + hposZ + ChatFormatting.WHITE + "]"));
        String direction1 = "";

        i += offset;

        if (direction.getValue()) {
            switch (mc.player.getDirection()) {
                case EAST -> direction1 = "East" + ChatFormatting.WHITE + " [+X]";
                case WEST -> direction1 = "West" + ChatFormatting.WHITE + " [-X]";
                case NORTH -> direction1 = "North" + ChatFormatting.WHITE + " [-Z]";
                case SOUTH -> direction1 = "South" + ChatFormatting.WHITE + " [+Z]";
            }
            drawText(context, direction1, 2, (height - i - 11));
        }

        if (coords.getValue()) drawText(context, coordinates, 2, (height - i));
        if (armor.getValue()) renderArmorHUD(true, context);
        if (totems.getValue()) renderTotemHUD(context);
        if (greeter.getValue()) renderGreeter(context);
    }

    private void drawText(GuiGraphics context, String str, int x, int y, int color) {
        if (!customFont.getValue().equals(Font.Minecraft)) {
            FontRenderer adapter;
            switch (customFont.getValue()) {
                case Monsterrat -> adapter = FontRenderers.monsterrat;
                case SF -> adapter = FontRenderers.sf_medium;
                default -> adapter = FontRenderers.modules;
            }
            adapter.drawString(context.pose(), str.replace(ChatFormatting.WHITE + "", ""), x + 0.5, y + 0.5, Color.BLACK.getRGB());
            adapter.drawString(context.pose(), str, x, y, color);
            return;
        }
        context.drawString(mc.font, str, x, y, color, true);
    }

    private void drawText(GuiGraphics context, String str, int x, int y) {
        if (!customFont.getValue().equals(Font.Minecraft)) {
            FontRenderer adapter;
            switch (customFont.getValue()) {
                case Monsterrat -> adapter = FontRenderers.monsterrat;
                case SF -> adapter = FontRenderers.sf_medium;
                default -> adapter = FontRenderers.modules;
            }
            adapter.drawString(context.pose(), str.replace(ChatFormatting.WHITE + "", ""), x + 0.5, y + 0.5, Color.BLACK.getRGB());
            adapter.drawString(context.pose(), str, x, y, color);
            return;
        }
        context.drawString(mc.font, str, x, y, color, true);
    }

    private int getStringWidth(String str) {
        switch (customFont.getValue()) {
            case Monsterrat -> {
                return (int) FontRenderers.monsterrat.getStringWidth(str);
            }
            case SF -> {
                return (int) FontRenderers.sf_medium.getStringWidth(str);
            }
            case Minecraft -> {
                return mc.font.width(str);
            }
            default -> {
                return (int) FontRenderers.modules.getStringWidth(str);
            }
        }
    }

    public void renderGreeter(GuiGraphics context) {
        String text = "Good " + getTimeOfDay() + mc.player.getName().getString();
        drawText(context, text, (int) (mc.getWindow().getGuiScaledWidth() / 2.0F - getStringWidth(text) / 2.0F + 2.0F), 2);
    }

    public static String getTimeOfDay() {
        int timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (timeOfDay < 12) return "Morning ";
        if (timeOfDay < 16) return "Afternoon ";
        if (timeOfDay < 21) return "Evening ";
        return "Night ";
    }

    public void renderTotemHUD(GuiGraphics context) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int totems = InventoryUtility.getItemCount(Items.TOTEM_OF_UNDYING);
        int u = mc.player.getMaxAirSupply();
        int v = Math.min(mc.player.getAirSupply(), u);
        if (mc.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING)
            totems += mc.player.getOffhandItem().getCount();
        if (totems > 0) {
            int i = width / 2;
            int y = height - 55 - (mc.player.isUnderWater() || v < u ? 10 : 0);
            int x = i - 189 + 180 + 2;
            context.renderItem(totem, x, y);
            context.renderItemDecorations(mc.font, totem, x, y);
            drawText(context, totems + "", 8 + (int) (x - (float) getStringWidth(totems + "") / 2f), (y - 7), 16777215);
        }
    }
    private static String biome() {
        if (mc.player == null || mc.level == null) return null;
        Identifier id = mc.level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(mc.level.getBiome(mc.player.blockPosition()).value());
        if (id == null) return ("Unknown");

        return (Arrays.stream(id.getPath().split("_")).map(StringUtils::capitalize).collect(Collectors.joining(" ")));
    }

    public void renderArmorHUD(boolean percent, GuiGraphics context) {
        int i = 0;
        int u = mc.player.getMaxAirSupply();
        int v = Math.min(mc.player.getAirSupply(), u);

        int y = mc.getWindow().getGuiScaledHeight() - 55 - (mc.player.isUnderWater() || v < u ? 10 : 0);
        for (ItemStack is : thunder.hack.utility.player.ArmorUtility.getArmorItems(mc.player)) {
            i++;
            if (is.isEmpty())
                continue;
            int x = (mc.getWindow().getGuiScaledWidth() / 2) - 90 + (9 - i) * 20 + 2;
            context.renderItem(is, x, y);
            context.renderItemDecorations(mc.font, is, x, y);
            String s = (is.getCount() > 1) ? (is.getCount() + "") : "";
            drawText(context, s, (x + 19 - 2 - getStringWidth(s)), (y + 9), 16777215);
            if (percent) {
                float green = (float) (is.getMaxDamage() - is.getDamageValue()) / (float) is.getMaxDamage();
                float red = 1.0F - green;
                int dmg = 100 - (int) (red * 100.0F);

                drawText(context, dmg + "", (x + 8 - getStringWidth(dmg + "") / 2), (y - 11), new Color((int) MathUtility.clamp((red * 255.0F), 0, 255f), (int) MathUtility.clamp((green * 255.0F), 0, 255f), 0).getRGB());
            }
        }
    }
}
