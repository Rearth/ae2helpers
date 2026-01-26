package rearth.ae2helpers.util;

import appeng.items.materials.UpgradeCardItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.client.ImportCardScreen;

import java.util.List;

public class ImportCardItem extends UpgradeCardItem {
    
    public ImportCardItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        
        if (level.isClientSide) {
            if (!stack.has(ae2helpers.IMPORT_CARD_CONFIG)) {
                stack.set(ae2helpers.IMPORT_CARD_CONFIG, ImportCardConfig.DEFAULT);
            }
            
            Minecraft.getInstance().setScreen(new ImportCardScreen(stack));
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
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
