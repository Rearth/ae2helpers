package rearth.ae2helpers.mixin.crafter;

import appeng.integration.modules.itemlists.CraftingHelper;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.util.CraftingRecipeUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.ae2helpers.client.AutoCraftingWatcher;

import java.util.HashMap;

@Mixin(value = CraftingHelper.class, remap = false)
public class CraftingHelperMixin {
    
    @Inject(method = "performTransfer", at = @At("HEAD"))
    private static void ae2extras$onPerformTransfer(CraftingTermMenu menu, ResourceKey<Recipe<?>> recipeId, Recipe<?> recipe, boolean craftMissing, CallbackInfo ci) {
        AutoCraftingWatcher.INSTANCE.clear(menu);
        if (craftMissing && AutoCraftingWatcher.INSTANCE.isAutoInsertEnabled()) {
            var ingredients = CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
            
            var slotToIngredientMap = new HashMap<Integer, Ingredient>();
            for (int i = 0; i < ingredients.size(); i++) {
                final int slot = i;
                ingredients.get(i).ifPresent(ingredient -> slotToIngredientMap.put(slot, ingredient));
            }
            
            var missingResult = menu.findMissingIngredients(slotToIngredientMap);
            
            if (!missingResult.craftableSlots().isEmpty()) {
                AutoCraftingWatcher.INSTANCE.setPending(menu, slotToIngredientMap, missingResult.craftableSlots());
            }
        }
    }
}
