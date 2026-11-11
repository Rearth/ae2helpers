package rearth.ae2helpers.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.client.AutoCraftingWatcher;

public record PendingCraftsStatusPacket(int containerId, int revision, boolean known, int activeSlots)
        implements CustomPacketPayload {
    public static final Type<PendingCraftsStatusPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ae2helpers.MODID, "pending_crafts_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PendingCraftsStatusPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PendingCraftsStatusPacket::containerId,
            ByteBufCodecs.VAR_INT, PendingCraftsStatusPacket::revision,
            ByteBufCodecs.BOOL, PendingCraftsStatusPacket::known,
            ByteBufCodecs.VAR_INT, PendingCraftsStatusPacket::activeSlots,
            PendingCraftsStatusPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PendingCraftsStatusPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> AutoCraftingWatcher.INSTANCE.receiveStatus(packet));
    }
}
