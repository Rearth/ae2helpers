package rearth.ae2helpers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.ImportCardConfig;

// because the source method is also loaded on the server (but never called), a separate helper class is needed
public class ImportCardClientHelper {
    
    public static void openScreen(ItemStack stack) {
        if (!stack.has(ae2helpers.IMPORT_CARD_CONFIG)) {
            stack.set(ae2helpers.IMPORT_CARD_CONFIG, ImportCardConfig.DEFAULT);
        }
        Minecraft.getInstance().setScreen(new ImportCardScreen(stack));
    }
}