package rearth.ae2helpers.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.ImportCardConfig;

public record UpdateImportCardPacket(InteractionHand hand, ImportCardConfig config) implements CustomPacketPayload {
    
    public static final Type<UpdateImportCardPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ae2helpers.MODID, "update_import_card"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateImportCardPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL.map(offhand -> offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, hand -> hand == InteractionHand.OFF_HAND), UpdateImportCardPacket::hand,
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
                var heldItem = player.getItemInHand(packet.hand);
                
                if (heldItem.is(ae2helpers.RESULT_IMPORT_CARD.get())) {
                    heldItem.set(ae2helpers.IMPORT_CARD_CONFIG.get(), packet.config);
                }
            }
        });
    }
}
