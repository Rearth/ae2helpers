package rearth.ae2helpers.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

// Stored on the redstone link card: the pattern provider it points at. side is set only when the
// provider is a cable part (null for a provider block).
public record ProviderLink(GlobalPos pos, @Nullable Direction side) {

    public static final Codec<ProviderLink> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      GlobalPos.CODEC.fieldOf("pos").forGetter(ProviderLink::pos),
      Direction.CODEC.optionalFieldOf("side").forGetter(l -> Optional.ofNullable(l.side()))
    ).apply(inst, (pos, side) -> new ProviderLink(pos, side.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProviderLink> STREAM_CODEC = StreamCodec.composite(
      GlobalPos.STREAM_CODEC, ProviderLink::pos,
      Direction.STREAM_CODEC.apply(ByteBufCodecs::optional), l -> Optional.ofNullable(l.side()),
      (pos, side) -> new ProviderLink(pos, side.orElse(null))
    );

    public BlockPos blockPos() {
        return pos.pos();
    }
}
