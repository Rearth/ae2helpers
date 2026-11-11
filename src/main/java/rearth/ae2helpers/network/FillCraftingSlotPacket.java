package rearth.ae2helpers.network;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.CraftingTermMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record FillCraftingSlotPacket(int containerId, int slotIndex, AEKey what) implements CustomPacketPayload {
    
    public static final Type<FillCraftingSlotPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("ae2helpers", "fill_slot"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, FillCraftingSlotPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, FillCraftingSlotPacket::containerId,
      ByteBufCodecs.VAR_INT, FillCraftingSlotPacket::slotIndex,
      AEKey.STREAM_CODEC, FillCraftingSlotPacket::what,
      FillCraftingSlotPacket::new
    );
    
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(FillCraftingSlotPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof CraftingTermMenu menu && payload.what instanceof AEItemKey itemKey) {
                
                var craftingSlots = menu.getSlots(SlotSemantics.CRAFTING_GRID);
                if (menu.containerId != payload.containerId || !menu.stillValid(player) || payload.slotIndex < 0 || payload.slotIndex >= craftingSlots.size()) return;

                var node = menu.getGridNode();
                if (node == null || !node.isActive() || !menu.getLinkStatus().connected()) return;
                
                var targetSlot = craftingSlots.get(payload.slotIndex);
                
                if (targetSlot.hasItem() || !targetSlot.mayPlace(itemKey.toStack())) return;
                
                // extract from network
                long extracted = StorageHelper.poweredExtraction(
                  menu.getEnergySource(),
                  menu.getHost().getInventory(),
                  itemKey,
                  1,
                  menu.getActionSource()
                );
                
                // insert to slot
                if (extracted > 0) {
                    ItemStack stack = itemKey.toStack((int) extracted);
                    targetSlot.set(stack);
                    menu.broadcastChanges();
                }
            }
        });
    }
}
