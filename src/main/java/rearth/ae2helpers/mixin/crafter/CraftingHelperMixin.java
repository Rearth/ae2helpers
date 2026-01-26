package rearth.ae2helpers.mixin.crafter;

import appeng.integration.modules.itemlists.CraftingHelper;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.util.CraftingRecipeUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.client.AutoCraftingWatcher;

import java.util.Arrays;
import java.util.HashMap;

@Mixin(value = CraftingHelper.class, remap = false)
public class CraftingHelperMixin {
    
    @Inject(method = "performTransfer", at = @At("HEAD"))
    private static void onPerformTransfer(CraftingTermMenu menu, ResourceLocation recipeId, Recipe<?> recipe, boolean craftMissing, CallbackInfo ci) {
        if (craftMissing && AutoCraftingWatcher.INSTANCE.isAutoInsertEnabled()) {
            var ingredients = CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
            
            var slotToIngredientMap = new HashMap<Integer, Ingredient>();
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient ingredient = ingredients.get(i);
                if (!ingredient.isEmpty()) {
                    slotToIngredientMap.put(i, ingredient);
                }
            }
            
            var missingResult = menu.findMissingIngredients(slotToIngredientMap);
            
            if (!missingResult.craftableSlots().isEmpty()) {
                for (var missing : missingResult.craftableSlots()) {
                    var ingredient = slotToIngredientMap.get(missing);
                    ae2helpers.LOGGER.info("mis: " + missing + " | " + Arrays.stream(ingredient.getItems()).findFirst());
                    AutoCraftingWatcher.INSTANCE.setPending(
                      slotToIngredientMap,
                      missingResult.craftableSlots()
                    );
                }
            }
        }
    }
}