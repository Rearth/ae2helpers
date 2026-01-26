package rearth.ae2helpers.mixin.importcard;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.parts.automation.StackWorldBehaviors;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.IPatternProviderUpgradeHost;
import rearth.ae2helpers.util.ImportCardConfig;
import rearth.ae2helpers.util.PatternProviderImportContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(PatternProviderLogic.class)
public abstract class PatternProviderImportMixin implements IPatternProviderUpgradeHost {
    
    @Shadow @Final private IManagedGridNode mainNode;
    @Shadow @Final private IActionSource actionSource;
    @Shadow @Final private PatternProviderLogicHost host;
    @Shadow public abstract void saveChanges();
    
    @Unique private StackImportStrategy ae2helpers$importStrategy;
    @Unique private Direction ae2helpers$currentSide;
    
    // used to detect config changes
    @Unique private Direction ae2helpers$lastUsedConfigDirection;
    
    @Unique private IUpgradeInventory ae2helpers$upgradeSlots = UpgradeInventories.empty();
    @Unique private final Map<AEKey, Long> ae2helpers$expectedResults = new HashMap<>();
    
    @Unique private int ae2helpers$cyclesSinceLastCheck = 0;
    @Unique private float ae2helpers$currentCycleDelay = 1f;
    @Unique private static final int AEHELPERS$MAX_CYCLE_DELAY = 10;
    
    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/patternprovider/PatternProviderLogicHost;I)V",at = @At("TAIL"))
    private void initUpgrade(IManagedGridNode mainNode, PatternProviderLogicHost host, int patternInventorySize, CallbackInfo ci) {
        this.ae2helpers$upgradeSlots = UpgradeInventories.forMachine(ae2helpers.RESULT_IMPORT_CARD, 1, this::saveChanges);
    }
    
    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void ae2helpers$onPushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            if (!ae2helpers$hasImportCard()) return;
            
            for (var output : patternDetails.getOutputs()) {
                if (output != null) {
                    ae2helpers$expectedResults.merge(output.what(), output.amount(), Long::sum);
                }
            }
            
            ae2helpers$currentCycleDelay = 1;
            ae2helpers$cyclesSinceLastCheck = 0;
            this.saveChanges();
            
            this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        }
    }
    
    @Unique
    private void ae2helpers$syncWithCraftingService() {
        var grid = this.mainNode.getGrid();
        if (grid == null) return;
        
        var craftingService = grid.getCraftingService();
        if (craftingService == null) return;
        
        var it = ae2helpers$expectedResults.entrySet().iterator();
        var changed = false;
        
        while (it.hasNext()) {
            var entry = it.next();
            var totalRequested = craftingService.getRequestedAmount(entry.getKey());
            
            if (totalRequested <= 0) {
                it.remove();
                changed = true;
            } else if (entry.getValue() > totalRequested) {
                entry.setValue(totalRequested);
                changed = true;
            }
        }
        
        if (changed) this.saveChanges();
    }
    
    @Inject(method = "doWork", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$onDoWork(CallbackInfoReturnable<Boolean> cir) {
        if (!this.mainNode.isActive()) return;
        
        if (!ae2helpers$hasImportCard()) {
            if (!ae2helpers$expectedResults.isEmpty()) {
                ae2helpers$expectedResults.clear();
                this.saveChanges();
            }
            return;
        }
        
        ae2helpers.LOGGER.debug("Doing work");
        
        var config = ae2helpers$getConfig();
        
        // If we are in "Result Only" mode AND have no expectations, we sleep.
        // If "Result Only" is FALSE (Import Everything), we must run even if map is empty.
        if (config.resultsOnly() && ae2helpers$expectedResults.isEmpty()) return;
        
        ae2helpers$cyclesSinceLastCheck++;
        
        if (ae2helpers$cyclesSinceLastCheck >= (int) ae2helpers$currentCycleDelay) {
            ae2helpers$cyclesSinceLastCheck = 0;
            
            var didWork = ae2helpers$doImportWork(config);
            ae2helpers.LOGGER.debug("Import Success: {}", didWork);
            
            if (didWork) {
                ae2helpers$currentCycleDelay = 1;
                cir.setReturnValue(true);
            } else {
                ae2helpers$currentCycleDelay = Math.min(AEHELPERS$MAX_CYCLE_DELAY, ae2helpers$currentCycleDelay * 1.15f);
            }
            
            // Sync only if there is data AND config allows it
            if (!ae2helpers$expectedResults.isEmpty() && config.syncToGrid()) {
                ae2helpers$syncWithCraftingService();
            }
        }
    }
    
    @Inject(method = "hasWorkToDo", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$hasWorkToDo(CallbackInfoReturnable<Boolean> cir) {
        // If AE2 thinks it's asleep, check if we need to wake up
        if (!cir.getReturnValue() && ae2helpers$hasImportCard()) {
            var config = ae2helpers$getConfig();
            // Wake up if: "Import All" mode is ON, OR we have specific results waiting
            if (!config.resultsOnly() || !ae2helpers$expectedResults.isEmpty()) {
                cir.setReturnValue(true);
            }
        }
    }
    
    @Unique
    private boolean ae2helpers$doImportWork(ImportCardConfig config) {
        var targets = this.host.getTargets();
        if (targets.isEmpty()) return false;
        if (targets.size() != 1) {
            this.ae2helpers$importStrategy = null;
            this.ae2helpers$currentSide = null;
            return false;
        }
        
        var side = targets.iterator().next();
        
        var targetSide = config.overriddenDirection();
        if (targetSide == null) targetSide = side.getOpposite();
        
        // Check if strategy needs recreation:
        // 1. Provider rotated (currentSide != side)
        // 2. Strategy is null
        // 3. Config changed (lastUsed != targetSide)
        if (this.ae2helpers$importStrategy == null
              || this.ae2helpers$currentSide != side
              || this.ae2helpers$lastUsedConfigDirection != targetSide) {
            
            var be = this.host.getBlockEntity();
            if (be == null || be.getLevel() == null) return false;
            
            var level = (ServerLevel) be.getLevel();
            var pos = be.getBlockPos();
            
            this.ae2helpers$importStrategy = StackWorldBehaviors.createImportFacade(
              level,
              pos.relative(side),
              targetSide,
              (type) -> true
            );
            this.ae2helpers$currentSide = side;
            this.ae2helpers$lastUsedConfigDirection = targetSide;
        }
        
        var context = new PatternProviderImportContext(
          this.mainNode.getGrid().getStorageService(),
          this.mainNode.getGrid().getEnergyService(),
          this.actionSource,
          this.ae2helpers$expectedResults,
          config.resultsOnly() // Pass the mode
        );
        
        this.ae2helpers$importStrategy.transfer(context);
        
        var importedMap = context.getImportedItems();
        if (!importedMap.isEmpty()) {
            // always track results/expectations in case config is changed
            if (!ae2helpers$expectedResults.isEmpty()) {
                var changed = false;
                var it = ae2helpers$expectedResults.entrySet().iterator();
                
                while (it.hasNext()) {
                    var entry = it.next();
                    var key = entry.getKey();
                    var expected = entry.getValue();
                    
                    var actuallyImported = importedMap.getOrDefault(key, 0L);
                    
                    if (actuallyImported > 0) {
                        var remaining = expected - actuallyImported;
                        if (remaining <= 0) {
                            it.remove();
                        } else {
                            entry.setValue(remaining);
                        }
                        changed = true;
                    }
                }
                
                if (changed) {
                    this.saveChanges();
                }
            }
            return true;
        }
        
        return false;
    }
    
    @Unique
    private ImportCardConfig ae2helpers$getConfig() {
        if (!ae2helpers$hasImportCard()) return ImportCardConfig.DEFAULT;
        var stack = ae2helpers$upgradeSlots.getStackInSlot(0);
        return stack.getOrDefault(ae2helpers.IMPORT_CARD_CONFIG.get(), ImportCardConfig.DEFAULT);
    }
    
    @Unique
    private boolean ae2helpers$hasImportCard() {
        return !ae2helpers$upgradeSlots.getStackInSlot(0).isEmpty()
                 && ae2helpers$upgradeSlots.getStackInSlot(0).is(ae2helpers.RESULT_IMPORT_CARD.get());
    }
    
    @Inject(method = "clearContent", at = @At("HEAD"))
    private void ae2helpers$onClearContent(CallbackInfo ci) {
        this.ae2helpers$importStrategy = null;
        this.ae2helpers$currentSide = null;
        this.ae2helpers$expectedResults.clear();
        this.ae2helpers$upgradeSlots.clear();
    }
    
    @Inject(method = "addDrops", at = @At("TAIL"))
    private void dropUpgrade(List<ItemStack> drops, CallbackInfo ci) {
        for (var slot : this.ae2helpers$upgradeSlots) {
            if (!slot.isEmpty()) {
                drops.add(slot);
            }
        }
    }
    
    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void ae2helpers$writeToNBT(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (!ae2helpers$expectedResults.isEmpty()) {
            var list = new ListTag();
            ae2helpers$expectedResults.forEach((key, amount) -> {
                list.add(GenericStack.writeTag(registries, new GenericStack(key, amount)));
            });
            tag.put("ae2helpers_expected_results", list);
        }
        ae2helpers$upgradeSlots.writeToNBT(tag, "ae2helperupgrades", registries);
    }
    
    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void ae2helpers$readFromNBT(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        ae2helpers$expectedResults.clear();
        if (tag.contains("ae2helpers_expected_results")) {
            var list = tag.getList("ae2helpers_expected_results", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                var stack = GenericStack.readTag(registries, list.getCompound(i));
                if (stack != null) {
                    ae2helpers$expectedResults.put(stack.what(), stack.amount());
                }
            }
        }
        ae2helpers$upgradeSlots.readFromNBT(tag, "ae2helperupgrades", registries);
    }
    
    @Override
    public IUpgradeInventory ae2helpers$getUpgradeInventory() {
        return ae2helpers$upgradeSlots;
    }
}