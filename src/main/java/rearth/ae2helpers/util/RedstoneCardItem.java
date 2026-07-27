package rearth.ae2helpers.util;

import appeng.core.localization.ButtonToolTips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import rearth.ae2helpers.client.RedstoneCardClientHelper;

import java.util.List;

public class RedstoneCardItem extends ProviderUpgradeCardItem {

    public RedstoneCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);

        if (level.isClientSide) {
            RedstoneCardClientHelper.openScreen(stack);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(ButtonToolTips.SupportedBy.text());
        tooltipComponents.add(Component.translatable("ae2helpers.redstonecard.pattern").withStyle(ChatFormatting.GRAY));

        tooltipComponents.add(Component.literal(""));

        var config = stack.getOrDefault(rearth.ae2helpers.ae2helpers.REDSTONE_CARD_CONFIG.get(), RedstoneCardConfig.DEFAULT);

        tooltipComponents.add(Component.translatable("ae2helpers.redstonecard.tooltip.mode").withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable("ae2helpers.redstonecard.mode." + config.mode().getSerializedName()).withStyle(ChatFormatting.GOLD)));

        if (config.mode() == RedstoneMode.PULSE) {
            tooltipComponents.add(Component.translatable("ae2helpers.redstonecard.tooltip.pulse").withStyle(ChatFormatting.GRAY)
                                    .append(Component.translatable("ae2helpers.redstonecard.pulse.value", config.pulseLength()).withStyle(ChatFormatting.GOLD)));
        }

        tooltipComponents.add(Component.translatable("ae2helpers.redstonecard.tooltip.signal").withStyle(ChatFormatting.GRAY)
                                .append(config.strongSignal()
                                          ? Component.translatable("ae2helpers.redstonecard.tooltip.strong").withStyle(ChatFormatting.GOLD)
                                          : Component.translatable("ae2helpers.redstonecard.tooltip.weak").withStyle(ChatFormatting.AQUA))
                                .append(Component.translatable("ae2helpers.redstonecard.tooltip.strength", config.signalStrength()).withStyle(ChatFormatting.GRAY)));

        var side = config.side();
        var sideText = (side == null)
                         ? Component.translatable("ae2helpers.redstonecard.side.all")
                         : Component.literal(side.getName().substring(0, 1).toUpperCase() + side.getName().substring(1));
        tooltipComponents.add(Component.translatable("ae2helpers.redstonecard.tooltip.side").withStyle(ChatFormatting.GRAY)
                                .append(sideText.withStyle(ChatFormatting.AQUA)));

        tooltipComponents.add(Component.translatable("ae2helpers.redstonecard.tooltip.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
