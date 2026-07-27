package rearth.ae2helpers.mixin.redstone;

import appeng.block.crafting.PatternProviderBlock;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import rearth.ae2helpers.util.IProviderRedstoneHost;
import rearth.ae2helpers.util.RedstoneEmission;

// Makes the pattern provider block emit a redstone signal while the redstone card is installed and one
// of its patterns is being crafted. The added methods override the inherited BlockBehaviour defaults.
@Mixin(PatternProviderBlock.class)
public abstract class PatternProviderRedstoneBlockMixin extends Block {

    public PatternProviderRedstoneBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof PatternProviderLogicHost host
              && host.getLogic() instanceof IProviderRedstoneHost redstone) {
            return RedstoneEmission.weak(redstone, direction);
        }
        return 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof PatternProviderLogicHost host
              && host.getLogic() instanceof IProviderRedstoneHost redstone) {
            return RedstoneEmission.strong(redstone, direction);
        }
        return 0;
    }
}
