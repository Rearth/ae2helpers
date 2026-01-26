package rearth.ae2helpers.client;

import appeng.api.stacks.AEKey;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.CraftingTermMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.network.PacketDistributor;
import rearth.ae2helpers.Config;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.network.FillCraftingSlotPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AutoCraftingWatcher {
    
    public static final AutoCraftingWatcher INSTANCE = new AutoCraftingWatcher();
    
    // crafting slot index (0-9) to ingredient being crafted / waiting
    private final Map<Integer, Ingredient> pendingSlots = new HashMap<>();
    
    private boolean active = false;
    private boolean screenOpened = false;
    private int startDelay = 0;
    private int craftingSlotsOffset = 0;
    
    
    public void setPending(Map<Integer, Ingredient> recipeMap, Set<Integer> slotsToWatch) {
        this.pendingSlots.clear();
        for (Integer slotIndex : slotsToWatch) {
            Ingredient ing = recipeMap.get(slotIndex);
            if (ing != null && !ing.isEmpty()) {
                this.pendingSlots.put(slotIndex, ing);
            }
        }
        
        if (!this.pendingSlots.isEmpty()) {
            startDelay = 15;
            active = true;
            screenOpened = false;
        }
    }
    
    public void onScreenRemoved() {
        if (screenOpened)
            clear();
    }
    
    public void clear() {
        this.pendingSlots.clear();
        this.active = false;
        this.startDelay = 0;
        this.screenOpened = false;
        ae2helpers.LOGGER.info("Cleared screen");
    }
    
    public void onTick(MEStorageScreen<?> screen) {
        if (!active || pendingSlots.isEmpty()) return;
        
        if (!(screen.getMenu() instanceof CraftingTermMenu menu)) {
            clear();
            return;
        }
        
        var craftingSlots = menu.getSlots(SlotSemantics.CRAFTING_GRID);
        craftingSlotsOffset = craftingSlots.getFirst().index;
        
        if (startDelay > 0) {
            startDelay--;
            ae2helpers.LOGGER.info("Skipping " + startDelay);
            return; // Wait for AE2 to finish its native moves
        }
        
        this.screenOpened = true;
        
        var repo = menu.getClientRepo();
        if (repo == null) return;
        
        
        var it = pendingSlots.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            int slotIndex = entry.getKey();
            Ingredient ingredient = entry.getValue();
            
            // ae2helpers.LOGGER.debug("Trying to fill slot: " + slotIndex + " with: " + ingredient.getItems()[0]);
            
            // if slot has been filled otherwise (e.g. by user)
            if (craftingSlots.get(slotIndex).hasItem()) {
                it.remove();
                continue;
            }
            
            // check if system has this ingredient
            var entries = repo.getByIngredient(ingredient);
            AEKey bestMatch = null;
            
            for (var potential : entries) {
                // Check if we have at least 1 stored
                if (potential.getStoredAmount() > 0) {
                    bestMatch = potential.getWhat();
                    break;
                }
            }
            
            // move found ingredient to slot
            if (bestMatch != null) {
                
                ae2helpers.LOGGER.info("Found match for slot " + slotIndex + ": " + bestMatch.wrapForDisplayOrFilter());
                
                // Send packet to server
                PacketDistributor.sendToServer(new FillCraftingSlotPacket(slotIndex, bestMatch));
                
                // Stop watching this slot locally (server will fill it shortly)
                it.remove();
                
                // stop to avoid trying to put the same item into multiple slots
                startDelay = 5;
                break;
            }
        }
        
        if (pendingSlots.isEmpty()) {
            active = false;
        }
    }
    
    
    public void renderGhosts(GuiGraphics guiGraphics, Slot slot) {
        if (!active || !pendingSlots.containsKey(slot.index - craftingSlotsOffset)) return;
        
        if (slot.hasItem()) return;
        
        Ingredient ingredient = pendingSlots.get(slot.index - craftingSlotsOffset);
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) return;
        
        // Cycle items based on time
        long time = Minecraft.getInstance().level.getGameTime() / 30;
        ItemStack stackToRender = stacks[(int) (time % stacks.length)];
        
        // Render Ghost Logic
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 50);
        
        // 1. Render the item
        guiGraphics.renderFakeItem(stackToRender, slot.x, slot.y);
        
        guiGraphics.pose().pushPose();
        
        guiGraphics.pose().translate(0, 0, 250); // Draw on top of slot
        
        RenderSystem.disableDepthTest();
        guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x608B8B8B);
        RenderSystem.enableDepthTest();
        
        drawSpinner(guiGraphics, slot.x + 8, slot.y + 8);
        
        guiGraphics.pose().popPose();
        
        guiGraphics.pose().popPose();
    }
    
    private void drawSpinner(GuiGraphics guiGraphics, int x, int y) {
        long millis = System.currentTimeMillis();
        // Rotation speed: one full turn every 1000ms
        float angle = (millis % 1000) / 1000f * 360f;
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        
        // Draw 6 dots in a circle
        int dots = 6;
        int radius = 5;
        int color = 0xFFFFFFFF; // White
        
        for (int i = 0; i < dots; i++) {
            // Calculate alpha to create a "fade trail" effect
            // The leading dot is opaque, trailing dots fade out
            int alpha = 255 - (i * (200 / dots));
            int dotColor = (alpha << 24) | (color & 0x00FFFFFF);
            
            // Position based on fixed circle, but we rotate the whole pose so simple math works
            double rad = Math.toRadians((360f / dots) * i);
            int dx = (int) (Math.cos(rad) * radius);
            int dy = (int) (Math.sin(rad) * radius);
            
            // Draw a small 2x2 or 1.5x1.5 dot
            // We use -1 offsets to center the dot on the calculated point
            guiGraphics.fill(dx - 1, dy - 1, dx + 1, dy + 1, dotColor);
        }
        
        guiGraphics.pose().popPose();
    }
    
    public boolean isAutoInsertEnabled() {
        return Config.ENABLE_AUTO_IMPORT.getAsBoolean();
    }
    
    public void toggleAutoInsert() {
        Config.ENABLE_AUTO_IMPORT.set(!isAutoInsertEnabled());
        Config.ENABLE_AUTO_IMPORT.save();
    }
}
