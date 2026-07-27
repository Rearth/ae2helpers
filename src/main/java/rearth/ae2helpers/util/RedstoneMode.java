package rearth.ae2helpers.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum RedstoneMode implements StringRepresentable {
    ACTIVE("active"),
    INVERTED("inverted"),
    PULSE("pulse");

    private final String name;

    RedstoneMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static final Codec<RedstoneMode> CODEC = StringRepresentable.fromEnum(RedstoneMode::values);

    public static final StreamCodec<ByteBuf, RedstoneMode> STREAM_CODEC =
      ByteBufCodecs.VAR_INT.map(i -> values()[i], RedstoneMode::ordinal);
}
