package rearth.ae2helpers;

import appeng.api.ids.AECreativeTabIds;
import appeng.api.upgrades.Upgrades;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import rearth.ae2helpers.network.FillCraftingSlotPacket;
import rearth.ae2helpers.network.QueryPendingCraftsPacket;
import rearth.ae2helpers.network.PendingCraftsStatusPacket;
import rearth.ae2helpers.network.UpdateImportCardPacket;
import rearth.ae2helpers.util.ImportCardConfig;
import rearth.ae2helpers.util.ImportCardItem;

@Mod(ae2helpers.MODID)
public class ae2helpers {
    
    public static final SlotSemantic IMPORT_UPGRADE = SlotSemantics.register("IMPORT_UPGRADE", false);
    
    public static final String MODID = "ae2helpers";
    
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ae2helpers.MODID);
    
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ImportCardConfig>> IMPORT_CARD_CONFIG =
      COMPONENTS.register("import_card_config", () -> DataComponentType.<ImportCardConfig>builder()
                                                        .persistent(ImportCardConfig.CODEC)
                                                        .networkSynchronized(ImportCardConfig.STREAM_CODEC)
                                                        .cacheEncoding()
                                                        .build());
    
    public static final DeferredItem<Item> RESULT_IMPORT_CARD =
      ITEMS.registerItem("result_import_card", ImportCardItem::new);
    
    public ae2helpers(IEventBus modEventBus, ModContainer modContainer) {
        
        modEventBus.addListener(this::commonSetup);
        
        COMPONENTS.register(modEventBus);
        ITEMS.register(modEventBus);
        
        modEventBus.addListener(this::injectToAETab);
        modEventBus.addListener(this::registerPayloads);
        
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        
        // ideally we'd define the machine(s) as target here, but that then breaks with other mods that add upgrades to the machine
        event.enqueueWork(() -> Upgrades.add(RESULT_IMPORT_CARD.get(), RESULT_IMPORT_CARD, 1, "gui.ae2helpers.import_card"));
        
    }
    
    private void injectToAETab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == AECreativeTabIds.MAIN) {
            event.accept(RESULT_IMPORT_CARD);
        }
    }
    
    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("4");
        
        registrar.playToServer(QueryPendingCraftsPacket.TYPE, QueryPendingCraftsPacket.STREAM_CODEC, QueryPendingCraftsPacket::handle);
        registrar.playToClient(PendingCraftsStatusPacket.TYPE, PendingCraftsStatusPacket.STREAM_CODEC, PendingCraftsStatusPacket::handle);

        registrar.playToServer(
          FillCraftingSlotPacket.TYPE,
          FillCraftingSlotPacket.STREAM_CODEC,
          FillCraftingSlotPacket::handle
        );
        
        registrar.playToServer(
          UpdateImportCardPacket.TYPE,
          UpdateImportCardPacket.STREAM_CODEC,
          UpdateImportCardPacket::handle
        );
    }
}
