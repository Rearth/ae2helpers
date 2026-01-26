package rearth.ae2helpers;

import appeng.api.ids.AECreativeTabIds;
import appeng.api.upgrades.Upgrades;
import appeng.items.materials.UpgradeCardItem;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import rearth.ae2helpers.network.FillCraftingSlotPacket;
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
      ITEMS.registerItem("result_import_card", ImportCardItem::new, new Item.Properties());
    
    
    public ae2helpers(IEventBus modEventBus, ModContainer modContainer) {
        
        modEventBus.addListener(this::commonSetup);
        
        COMPONENTS.register(modEventBus);
        ITEMS.register(modEventBus);
        
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ae2helpers) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        
        modEventBus.addListener(this::injectToAETab);
        modEventBus.addListener(this::registerPayloads);
        
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        
        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }
        
        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());
        
        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
        
        // ideally we'd define the machine(s) as target here, but that then breaks with other mods that add upgrades to the machine
        Upgrades.add(RESULT_IMPORT_CARD.get(), RESULT_IMPORT_CARD, 1, "gui.ae2helpers.import_card");
    }
    
    private void injectToAETab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == AECreativeTabIds.MAIN) {
            event.accept(RESULT_IMPORT_CARD);
        }
    }
    
    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("1");
        
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
    
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
