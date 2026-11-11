package rearth.ae2helpers.client;

import appeng.api.stacks.AEItemKey;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.CraftingTermMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.ae2helpers.Config;
import rearth.ae2helpers.network.FillCraftingSlotPacket;
import rearth.ae2helpers.network.QueryPendingCraftsPacket;
import rearth.ae2helpers.network.PendingCraftsStatusPacket;
import rearth.ae2helpers.util.PendingCraftingSlots;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AutoCraftingWatcher {
    public static final AutoCraftingWatcher INSTANCE = new AutoCraftingWatcher();

    private final PendingCraftingSlots pending = new PendingCraftingSlots();

    private PendingCraftingSlots.Terminal terminal(CraftingTermMenu menu) {
        var level = Minecraft.getInstance().level;
        if (level == null || menu.getLocator() == null) return null;
        return new PendingCraftingSlots.Terminal(level.dimension(), menu.getLocator());
    }

    public void setPending(CraftingTermMenu menu, Map<Integer, Ingredient> recipeMap, Set<Integer> slotsToWatch) {
        pending.replace(terminal(menu), recipeMap, slotsToWatch);
    }

    public void clear(CraftingTermMenu menu) {
        pending.clear(terminal(menu));
    }

    public void clearAll() {
        pending.clearAll();
    }

    public void receiveStatus(PendingCraftsStatusPacket status) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !(minecraft.player.containerMenu instanceof CraftingTermMenu menu)
                || menu.containerId != status.containerId()) return;
        var terminal = terminal(menu);
        var recipe = pending.get(terminal);
        if (recipe == null || recipe.revision() != status.revision()) return;
        recipe.updateCraftingStatus(status.known(), status.activeSlots(), minecraft.level.getGameTime());
        if (recipe.slots().isEmpty()) pending.clear(terminal);
    }

    public void onTick(MEStorageScreen<?> screen) {
        if (!isAutoInsertEnabled()) {
            clearAll();
            return;
        }
        // Closed terminals keep their requests, but only the open terminal can send transfers.
        if (!(screen.getMenu() instanceof CraftingTermMenu menu)) return;
        var terminal = terminal(menu);
        var recipe = pending.get(terminal);
        if (recipe == null) return;

        var craftingSlots = menu.getSlots(SlotSemantics.CRAFTING_GRID);
        // Acknowledgements and manual fills should clear requests even during retry cooldown.
        recipe.slots().keySet().removeIf(index -> index >= craftingSlots.size() || craftingSlots.get(index).hasItem());
        if (recipe.slots().isEmpty()) {
            pending.clear(terminal);
            return;
        }
        if (recipe.shouldQueryStatus()) {
            ClientPacketDistributor.sendToServer(new QueryPendingCraftsPacket(menu.containerId, recipe.revision(), Map.copyOf(recipe.slots())));
        }
        if (!recipe.readyToCheck()) return;
        var repo = menu.getClientRepo();
        if (repo == null) return;

        for (var entry : recipe.slots().entrySet()) {
            int slotIndex = entry.getKey();
            for (var candidate : repo.getByIngredient(entry.getValue())) {
                if (candidate.getStoredAmount() > 0 && candidate.getWhat() instanceof AEItemKey item) {
                    ClientPacketDistributor.sendToServer(new FillCraftingSlotPacket(menu.containerId, slotIndex, item));
                    // Wait for the server's slot update before removing the ghost. Retry if stock was taken meanwhile.
                    recipe.delayRetry();
                    return;
                }
            }
        }
    }

    private Ingredient ghostIngredient(CraftingTermMenu menu, Slot slot) {
        if (!isAutoInsertEnabled() || slot == null || slot.hasItem()) return null;
        var recipe = pending.get(terminal(menu));
        if (recipe == null) return null;
        int index = menu.getSlots(SlotSemantics.CRAFTING_GRID).indexOf(slot);
        return recipe.slots().get(index);
    }

    public boolean cancelGhost(MEStorageScreen<?> screen, double mouseX, double mouseY) {
        if (!(screen.getMenu() instanceof CraftingTermMenu menu)) return false;
        for (var slot : menu.getSlots(SlotSemantics.CRAFTING_GRID)) {
            int x = screen.getGuiLeft() + slot.x;
            int y = screen.getGuiTop() + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16
                    && ghostIngredient(menu, slot) != null) {
                int index = menu.getSlots(SlotSemantics.CRAFTING_GRID).indexOf(slot);
                return pending.remove(terminal(menu), index);
            }
        }
        return false;
    }

    private ItemStack displayStack(Ingredient ingredient) {
        var level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;
        var stacks = ingredient.display().resolveForStacks(SlotDisplayContext.fromLevel(level));
        if (stacks.isEmpty()) return ItemStack.EMPTY;
        return stacks.get((int) ((level.getGameTime() / 30) % stacks.size()));
    }

    public void renderGhosts(MEStorageScreen<?> screen, GuiGraphicsExtractor guiGraphics, Slot slot) {
        if (!(screen.getMenu() instanceof CraftingTermMenu menu)) return;
        var ingredient = ghostIngredient(menu, slot);
        if (ingredient == null) return;
        var stack = displayStack(ingredient);
        if (!stack.isEmpty()) guiGraphics.item(stack, slot.x, slot.y);
        guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x608B8B8B);
        double angle = (System.currentTimeMillis() % 1000) * Math.PI / 500;
        for (int i = 0; i < 6; i++) {
            double radians = angle + i * Math.PI / 3;
            int x = slot.x + 8 + (int) (Math.cos(radians) * 5);
            int y = slot.y + 8 + (int) (Math.sin(radians) * 5);
            guiGraphics.fill(x - 1, y - 1, x + 1, y + 1, ((255 - i * 33) << 24) | 0xFFFFFF);
        }
    }

    public boolean renderTooltip(MEStorageScreen<?> screen, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (!(screen.getMenu() instanceof CraftingTermMenu menu)) return false;
        var ingredient = ghostIngredient(menu, screen.getSlotUnderMouse());
        if (ingredient == null) return false;
        var lines = new ArrayList<Component>();
        var stack = displayStack(ingredient);
        if (!stack.isEmpty()) lines.add(stack.getHoverName());
        lines.add(Component.translatable("ae2helpers.crafthelper.pending").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("ae2helpers.crafthelper.remove_ghost").withStyle(ChatFormatting.GRAY));
        guiGraphics.setTooltipForNextFrame(Minecraft.getInstance().font, lines, Optional.empty(), mouseX, mouseY);
        return true;
    }

    public boolean isAutoInsertEnabled() {
        return Config.ENABLE_AUTO_IMPORT.getAsBoolean();
    }

    public void toggleAutoInsert() {
        Config.ENABLE_AUTO_IMPORT.set(!isAutoInsertEnabled());
        Config.ENABLE_AUTO_IMPORT.save();
        if (!isAutoInsertEnabled()) clearAll();
    }
}
