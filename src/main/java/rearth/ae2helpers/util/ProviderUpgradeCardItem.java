package rearth.ae2helpers.util;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.localization.PlayerMessages;
import appeng.items.materials.UpgradeCardItem;
import appeng.parts.crafting.PatternProviderPart;
import appeng.util.InteractionUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

// Shared base for ae2helpers upgrade cards: lets the card be sneak-clicked into a pattern provider's
// custom upgrade inventory (added by our mixins), covering AE2/ExtendedAE/ExpandedAE + Advanced AE.
public abstract class ProviderUpgradeCardItem extends UpgradeCardItem {

    public ProviderUpgradeCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        var hand = context.getHand();

        if (player != null && InteractionUtil.isInAlternateUseMode(player)) {
            var te = context.getLevel().getBlockEntity(context.getClickedPos());
            var upgrades = ae2helpers$findUpgradeInventory(te, context);

            if (upgrades != null) {
                var heldStack = player.getItemInHand(hand);

                boolean isFull = true;
                for (int i = 0; i < upgrades.size(); i++) {
                    if (upgrades.getStackInSlot(i).isEmpty()) {
                        isFull = false;
                        break;
                    }
                }
                if (isFull) {
                    player.displayClientMessage(PlayerMessages.MaxUpgradesInstalled.text(), true);
                    return InteractionResult.FAIL;
                }

                var maxInstalled = upgrades.getMaxInstalled(heldStack.getItem());
                var installed = upgrades.getInstalledUpgrades(heldStack.getItem());
                if (maxInstalled <= 0) {
                    player.displayClientMessage(PlayerMessages.UnsupportedUpgrade.text(), true);
                    return InteractionResult.FAIL;
                } else if (installed >= maxInstalled) {
                    player.displayClientMessage(PlayerMessages.MaxUpgradesOfTypeInstalled.text(), true);
                    return InteractionResult.FAIL;
                }

                if (player.getCommandSenderWorld().isClientSide()) {
                    return InteractionResult.SUCCESS;
                }

                player.setItemInHand(hand, upgrades.addItems(heldStack));
                return InteractionResult.sidedSuccess(player.getCommandSenderWorld().isClientSide());
            }
        }

        return super.onItemUseFirst(stack, context);
    }

    private static IUpgradeInventory ae2helpers$findUpgradeInventory(BlockEntity te, UseOnContext context) {
        if (te instanceof CableBusBlockEntity be
              && be.selectPartWorld(context.getClickLocation()).part instanceof PatternProviderPart part
              && part.getLogic() instanceof IPatternProviderUpgradeHost upgradeHost) {
            return upgradeHost.ae2helpers$getUpgradeInventory();
        }
        if (te instanceof PatternProviderBlockEntity provider && provider.getLogic() instanceof IPatternProviderUpgradeHost upgradeHost) {
            return upgradeHost.ae2helpers$getUpgradeInventory();
        }
        if (te != null && ModList.get().isLoaded("advanced_ae")) {
            return AdvancedAeCardCompat.getUpgradeInventory(te, context.getClickLocation());
        }
        return null;
    }
}
