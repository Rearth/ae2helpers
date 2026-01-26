package rearth.ae2helpers.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record ImportCardConfig(boolean resultsOnly, boolean syncToGrid, @Nullable Direction overriddenDirection) {
    
    public static final ImportCardConfig DEFAULT = new ImportCardConfig(true, true, Optional.empty());
    
    public static final Codec<ImportCardConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      Codec.BOOL.fieldOf("results_only").forGetter(ImportCardConfig::resultsOnly),
      Codec.BOOL.fieldOf("sync_to_grid").forGetter(ImportCardConfig::syncToGrid),
      Direction.CODEC.optionalFieldOf("direction").forGetter(c -> Optional.ofNullable(c.overriddenDirection()))
    ).apply(inst, (res, sync, dir) -> new ImportCardConfig(res, sync, dir.orElse(null))));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ImportCardConfig> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, ImportCardConfig::resultsOnly,
      ByteBufCodecs.BOOL, ImportCardConfig::syncToGrid,
      Direction.STREAM_CODEC.apply(ByteBufCodecs::optional), c -> Optional.ofNullable(c.overriddenDirection()),
      ImportCardConfig::new
    );
    
    // Private constructor helper for the Optional -> Nullable conversion used by StreamCodec
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private ImportCardConfig(boolean resultsOnly, boolean syncToGrid, Optional<Direction> overriddenDirection) {
        this(resultsOnly, syncToGrid, overriddenDirection.orElse(null));
    }
    
}
