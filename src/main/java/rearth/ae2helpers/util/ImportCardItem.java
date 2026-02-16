package rearth.ae2helpers.util;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.PlayerMessages;
import appeng.items.materials.UpgradeCardItem;
import appeng.parts.crafting.PatternProviderPart;
import appeng.util.InteractionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.client.ImportCardClientHelper;

import java.util.List;

public class ImportCardItem extends UpgradeCardItem {
    
    public ImportCardItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        
        if (level.isClientSide) {
            ImportCardClientHelper.openScreen(stack);
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public @NotNull InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // similar to super method but only targets the custom mixin inventory
        var player = context.getPlayer();
        var hand = context.getHand();
        
        if (player != null && InteractionUtil.isInAlternateUseMode(player)) {
            var te = context.getLevel().getBlockEntity(context.getClickedPos());
            IUpgradeInventory upgrades = null;
            
            // ((PatternProviderPart) ((CableBusBlockEntity) te).selectPartWorld(context.getClickLocation()).part).getLogic() instanceof IPatternProviderUpgradeHost
            
            if (te instanceof CableBusBlockEntity be && be.selectPartWorld(context.getClickLocation()).part instanceof PatternProviderPart part && part.getLogic() instanceof IPatternProviderUpgradeHost upgradeHost) {
                upgrades = upgradeHost.ae2helpers$getUpgradeInventory();
            } else if (te instanceof PatternProviderBlockEntity provider && provider.getLogic() instanceof IPatternProviderUpgradeHost upgradeHost) {
                upgrades = upgradeHost.ae2helpers$getUpgradeInventory();
            }
            
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
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        
        tooltipComponents.add(ButtonToolTips.SupportedBy.text());
        tooltipComponents.add(Component.translatable("ae2helpers.importcard.pattern").withStyle(ChatFormatting.GRAY));
        
        tooltipComponents.add(Component.literal(""));
        
        var config = stack.getOrDefault(ae2helpers.IMPORT_CARD_CONFIG.get(), ImportCardConfig.DEFAULT);
        
        tooltipComponents.add(Component.translatable("ae2helpers.importcard.tooltip.mode").withStyle(ChatFormatting.GRAY)
                                .append(config.resultsOnly()
                                          ? Component.translatable("ae2helpers.importcard.tooltip.crafting_results").withStyle(ChatFormatting.GOLD)
                                          : Component.translatable("ae2helpers.importcard.tooltip.everything").withStyle(ChatFormatting.RED)));
        
        tooltipComponents.add(Component.translatable("ae2helpers.importcard.tooltip.sync").withStyle(ChatFormatting.GRAY)
                                .append(config.syncToGrid()
                                          ? Component.translatable("ae2helpers.importcard.tooltip.enabled").withStyle(ChatFormatting.GREEN)
                                          : Component.translatable("ae2helpers.importcard.tooltip.disabled").withStyle(ChatFormatting.RED)));
        
        var dir = config.overriddenDirection();
        var sideText = (dir == null)
                         ? Component.translatable("ae2helpers.importcard.direction.auto")
                         : Component.literal(dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1));
        
        tooltipComponents.add(Component.translatable("ae2helpers.importcard.tooltip.side").withStyle(ChatFormatting.GRAY)
                                .append(sideText.withStyle(ChatFormatting.AQUA)));
        
        tooltipComponents.add(Component.translatable("ae2helpers.importcard.tooltip.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
