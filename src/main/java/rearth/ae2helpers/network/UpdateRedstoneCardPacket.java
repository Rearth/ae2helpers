package rearth.ae2helpers.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.RedstoneCardConfig;

public record UpdateRedstoneCardPacket(RedstoneCardConfig config) implements CustomPacketPayload {

    public static final Type<UpdateRedstoneCardPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ae2helpers.MODID, "update_redstone_card"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateRedstoneCardPacket> STREAM_CODEC = StreamCodec.composite(
      RedstoneCardConfig.STREAM_CODEC, UpdateRedstoneCardPacket::config,
      UpdateRedstoneCardPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateRedstoneCardPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                var heldItem = player.getMainHandItem();

                if (heldItem.is(ae2helpers.REDSTONE_CARD.get())) {
                    heldItem.set(ae2helpers.REDSTONE_CARD_CONFIG.get(), packet.config);
                }
            }
        });
    }
}
