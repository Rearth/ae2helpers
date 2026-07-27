package rearth.ae2helpers.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record RedstoneCardConfig(boolean strongSignal, RedstoneMode mode, @Nullable Direction side, int pulseLength, int signalStrength) {

    public static final int DEFAULT_PULSE_LENGTH = 4;
    public static final int DEFAULT_SIGNAL_STRENGTH = 15;

    public static final RedstoneCardConfig DEFAULT = new RedstoneCardConfig(false, RedstoneMode.ACTIVE, (Direction) null, DEFAULT_PULSE_LENGTH, DEFAULT_SIGNAL_STRENGTH);

    public static final Codec<RedstoneCardConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      Codec.BOOL.fieldOf("strong_signal").forGetter(RedstoneCardConfig::strongSignal),
      RedstoneMode.CODEC.fieldOf("mode").forGetter(RedstoneCardConfig::mode),
      Direction.CODEC.optionalFieldOf("side").forGetter(c -> Optional.ofNullable(c.side())),
      Codec.INT.optionalFieldOf("pulse_length", DEFAULT_PULSE_LENGTH).forGetter(RedstoneCardConfig::pulseLength),
      Codec.INT.optionalFieldOf("signal_strength", DEFAULT_SIGNAL_STRENGTH).forGetter(RedstoneCardConfig::signalStrength)
    ).apply(inst, (strong, mode, side, pulse, strength) -> new RedstoneCardConfig(strong, mode, side.orElse(null), pulse, strength)));

    public static final StreamCodec<RegistryFriendlyByteBuf, RedstoneCardConfig> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, RedstoneCardConfig::strongSignal,
      RedstoneMode.STREAM_CODEC, RedstoneCardConfig::mode,
      Direction.STREAM_CODEC.apply(ByteBufCodecs::optional), c -> Optional.ofNullable(c.side()),
      ByteBufCodecs.VAR_INT, RedstoneCardConfig::pulseLength,
      ByteBufCodecs.VAR_INT, RedstoneCardConfig::signalStrength,
      RedstoneCardConfig::new
    );

    public RedstoneCardConfig {
        signalStrength = Math.max(1, Math.min(15, signalStrength));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private RedstoneCardConfig(boolean strongSignal, RedstoneMode mode, Optional<Direction> side, int pulseLength, int signalStrength) {
        this(strongSignal, mode, side.orElse(null), pulseLength, signalStrength);
    }
}
