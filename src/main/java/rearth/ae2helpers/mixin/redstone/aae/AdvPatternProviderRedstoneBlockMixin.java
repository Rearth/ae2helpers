package rearth.ae2helpers.mixin.redstone.aae;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.pedroksl.advanced_ae.common.blocks.AdvPatternProviderBlock;
import net.pedroksl.advanced_ae.common.blocks.SmallAdvPatternProviderBlock;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import org.spongepowered.asm.mixin.Mixin;
import rearth.ae2helpers.util.IProviderRedstoneHost;
import rearth.ae2helpers.util.RedstoneEmission;

@Mixin({AdvPatternProviderBlock.class, SmallAdvPatternProviderBlock.class})
public abstract class AdvPatternProviderRedstoneBlockMixin extends Block {

    public AdvPatternProviderRedstoneBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof AdvPatternProviderLogicHost host
              && host.getLogic() instanceof IProviderRedstoneHost redstone) {
            return RedstoneEmission.weak(redstone, direction);
        }
        return 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof AdvPatternProviderLogicHost host
              && host.getLogic() instanceof IProviderRedstoneHost redstone) {
            return RedstoneEmission.strong(redstone, direction);
        }
        return 0;
    }
}
