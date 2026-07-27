package rearth.ae2helpers.util;

import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.localization.ButtonToolTips;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.items.materials.UpgradeCardItem;
import appeng.util.InteractionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.ae2helpers.ae2helpers;

import java.util.List;

public class RedstoneLinkCard extends UpgradeCardItem {

    public RedstoneLinkCard(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (player != null && InteractionUtil.isInAlternateUseMode(player)) {
            var level = context.getLevel();
            var be = level.getBlockEntity(context.getClickedPos());
            var link = ae2helpers$makeLink(be, level, context.getClickLocation());
            if (link != null) {
                if (!level.isClientSide) {
                    stack.set(ae2helpers.REDSTONE_LINK.get(), link);
                    player.displayClientMessage(Component.translatable("ae2helpers.redstonelink.linked",
                      link.blockPos().toShortString()).withStyle(ChatFormatting.GREEN), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.onItemUseFirst(stack, context);
    }

    @Nullable
    private static ProviderLink ae2helpers$makeLink(BlockEntity be, Level level, Vec3 hit) {
        if (be instanceof PatternProviderLogicHost) {
            return new ProviderLink(GlobalPos.of(level.dimension(), be.getBlockPos()), null);
        }
        if (be instanceof CableBusBlockEntity cb) {
            var selected = cb.selectPartWorld(hit);
            if (selected.part instanceof PatternProviderLogicHost) {
                return new ProviderLink(GlobalPos.of(level.dimension(), be.getBlockPos()), selected.side);
            }
        }
        if (be != null && ModList.get().isLoaded("advanced_ae")) {
            return AdvancedAeLinkCompat.makeLink(be, level, hit);
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(ButtonToolTips.SupportedBy.text());
        tooltipComponents.add(Component.translatable("ae2helpers.redstonelink.pattern").withStyle(ChatFormatting.GRAY));

        var link = stack.get(ae2helpers.REDSTONE_LINK.get());
        if (link != null) {
            tooltipComponents.add(Component.translatable("ae2helpers.redstonelink.tooltip.linked", link.blockPos().toShortString()).withStyle(ChatFormatting.AQUA));
        } else {
            tooltipComponents.add(Component.translatable("ae2helpers.redstonelink.tooltip.unlinked").withStyle(ChatFormatting.RED));
        }
        tooltipComponents.add(Component.translatable("ae2helpers.redstonelink.tooltip.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
