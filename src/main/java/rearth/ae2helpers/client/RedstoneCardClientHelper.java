package rearth.ae2helpers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.RedstoneCardConfig;

// separate client-only helper, mirroring ImportCardClientHelper
public class RedstoneCardClientHelper {

    public static void openScreen(ItemStack stack) {
        if (!stack.has(ae2helpers.REDSTONE_CARD_CONFIG)) {
            stack.set(ae2helpers.REDSTONE_CARD_CONFIG, RedstoneCardConfig.DEFAULT);
        }
        Minecraft.getInstance().setScreen(new RedstoneCardScreen(stack));
    }
}
