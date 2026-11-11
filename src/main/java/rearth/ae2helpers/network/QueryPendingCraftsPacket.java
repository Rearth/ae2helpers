package rearth.ae2helpers.network;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEItemKey;
import appeng.menu.me.items.CraftingTermMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import rearth.ae2helpers.ae2helpers;

import java.util.HashMap;
import java.util.Map;

public record QueryPendingCraftsPacket(int containerId, int revision, Map<Integer, Ingredient> slots)
        implements CustomPacketPayload {
    public static final Type<QueryPendingCraftsPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ae2helpers.MODID, "query_pending_crafts"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QueryPendingCraftsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, QueryPendingCraftsPacket::containerId,
            ByteBufCodecs.VAR_INT, QueryPendingCraftsPacket::revision,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, Ingredient.CONTENTS_STREAM_CODEC, 9), QueryPendingCraftsPacket::slots,
            QueryPendingCraftsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static int activeSlots(ICraftingService crafting, Map<Integer, Ingredient> slots) {
        int active = 0;
        for (var cpu : crafting.getCpus()) {
            var job = cpu.getJobStatus();
            if (!cpu.isBusy() || job == null || job.crafting() == null || !(job.crafting().what() instanceof AEItemKey item)) continue;
            for (var slot : slots.entrySet()) {
                if (slot.getKey() >= 0 && slot.getKey() < 9 && item.matches(slot.getValue())) active |= 1 << slot.getKey();
            }
        }
        return active;
    }

    public static void handle(QueryPendingCraftsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof CraftingTermMenu menu)
                    || menu.containerId != packet.containerId || !menu.stillValid(player)) return;
            var node = menu.getGridNode();
            boolean known = node != null && node.isActive() && menu.getLinkStatus().connected();
            int active = known ? activeSlots(node.getGrid().getCraftingService(), packet.slots) : 0;
            context.reply(new PendingCraftsStatusPacket(packet.containerId, packet.revision, known, active));
        });
    }
}
