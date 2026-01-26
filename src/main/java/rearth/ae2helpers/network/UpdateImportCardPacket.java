package rearth.ae2helpers.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.ImportCardConfig;

public record UpdateImportCardPacket(ImportCardConfig config) implements CustomPacketPayload {
    
    public static final Type<UpdateImportCardPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ae2helpers.MODID, "update_import_card"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateImportCardPacket> STREAM_CODEC = StreamCodec.composite(
      ImportCardConfig.STREAM_CODEC, UpdateImportCardPacket::config,
      UpdateImportCardPacket::new
    );
    
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(UpdateImportCardPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                var heldItem = player.getMainHandItem();
                
                if (heldItem.is(ae2helpers.RESULT_IMPORT_CARD.get())) {
                    heldItem.set(ae2helpers.IMPORT_CARD_CONFIG.get(), packet.config);
                }
            }
        });
    }
}