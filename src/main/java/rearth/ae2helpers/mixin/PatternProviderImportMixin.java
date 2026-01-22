package rearth.ae2helpers.mixin;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.parts.automation.StackWorldBehaviors;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.PatternProviderImportContext;

import java.util.HashMap;
import java.util.Map;

@Mixin(PatternProviderLogic.class)
public abstract class PatternProviderImportMixin {
    
    @Shadow @Final private IManagedGridNode mainNode;
    @Shadow @Final private IActionSource actionSource;
    @Shadow @Final private PatternProviderLogicHost host;
    
    @Shadow public abstract void saveChanges();
    
    @Unique private StackImportStrategy ae2helpers$importStrategy;
    @Unique private Direction ae2helpers$currentSide;
    
    // Crafting result logic
    @Unique private final Map<AEKey, Long> ae2helpers$expectedResults = new HashMap<>();
    
    // Backoff logic: count invocations ("cycles") of doWork.
    // If we return false, AE2 sleeps for ~5 ticks. So 1 cycle ~= 5 ticks.
    @Unique private int ae2helpers$cyclesSinceLastCheck = 0;
    @Unique private float ae2helpers$currentCycleDelay = 1f;
    
    // Backoff config: Max 10 cycles * 5 ticks = 100 ticks (2.5 seconds)
    @Unique private static final int AEHELPERS$MAX_CYCLE_DELAY = 10;
    
    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void ae2helpers$onPushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            for (var output : patternDetails.getOutputs()) {
                if (output != null) {
                    ae2helpers$expectedResults.merge(output.what(), output.amount(), Long::sum);
                }
            }
            
            // Reset backoff to check immediately
            ae2helpers$currentCycleDelay = 1;
            ae2helpers$cyclesSinceLastCheck = 0;
            this.saveChanges();
            
            // Wake up the node to ensure doWork is called soon
            this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        }
    }
    
    // Checks all expected results, and lowers/removes them if there's no craft pending that needs them (e.g. cancelled / missed)
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
                ae2helpers.LOGGER.info("Removed pending request");
            } else if (entry.getValue() > totalRequested) {
                entry.setValue(totalRequested);
                changed = true;
                ae2helpers.LOGGER.info("Lowered pending request to: " + totalRequested);
            }
        }
        
        if (changed) this.saveChanges();
    }
    
    @Inject(method = "doWork", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$onDoWork(CallbackInfoReturnable<Boolean> cir) {
        if (!this.mainNode.isActive()) return;
        
        // Stop if we aren't expecting anything
        if (ae2helpers$expectedResults.isEmpty()) return;
        
        ae2helpers$cyclesSinceLastCheck++;
        
        if (ae2helpers$cyclesSinceLastCheck >= ae2helpers$currentCycleDelay) {
            ae2helpers$cyclesSinceLastCheck = 0;
            
            var didWork = ae2helpers$doImportWork();
            ae2helpers.LOGGER.info("Import Success: {}", didWork);
            
            if (didWork) {
                // We moved items! Force URGENT tick (return true) to move the rest immediately.
                ae2helpers$currentCycleDelay = 1;
                cir.setReturnValue(true);
            } else {
                // Found nothing. Increase delay (Exponential Backoff).
                // We do NOT return true here. We let AE2 sleep the node for ~5 ticks.
                ae2helpers$currentCycleDelay = Math.min(AEHELPERS$MAX_CYCLE_DELAY, ae2helpers$currentCycleDelay * 1.2f);
            }
            
            // sync with crafting service after operations (this just needs to be done periodic, but we still want to try to import
            // things once, even if the craft was cancelled
            ae2helpers$syncWithCraftingService();
        }
    }
    
    @Inject(method = "hasWorkToDo", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$hasWorkToDo(CallbackInfoReturnable<Boolean> cir) {
        // If AE2 thinks it's done (false), but we have expectations, force true.
        if (!cir.getReturnValue() && !ae2helpers$expectedResults.isEmpty()) {
            cir.setReturnValue(true);
        }
    }
    
    @Inject(method = "clearContent", at = @At("HEAD"))
    private void ae2helpers$onClearContent(CallbackInfo ci) {
        this.ae2helpers$importStrategy = null;
        this.ae2helpers$currentSide = null;
        this.ae2helpers$expectedResults.clear();
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
    }
    
    @Unique
    private boolean ae2helpers$doImportWork() {
        var targets = this.host.getTargets();
        if (targets.isEmpty()) return false;
        
        var side = targets.iterator().next();
        
        if (this.ae2helpers$importStrategy == null || this.ae2helpers$currentSide != side) {
            var be = this.host.getBlockEntity();
            if (be == null || be.getLevel() == null) return false;
            
            var level = (ServerLevel) be.getLevel();
            var pos = be.getBlockPos();
            
            this.ae2helpers$importStrategy = StackWorldBehaviors.createImportFacade(
              level,
              pos.relative(side),
              side.getOpposite(),
              (type) -> true // Allow all KeyTypes (Fluid/Item), filter in Context
            );
            this.ae2helpers$currentSide = side;
        }
        
        var context = new PatternProviderImportContext(
          this.mainNode.getGrid().getStorageService(),
          this.mainNode.getGrid().getEnergyService(),
          this.actionSource,
          this.ae2helpers$expectedResults
        );
        
        this.ae2helpers$importStrategy.transfer(context);
        
        var importedMap = context.getImportedItems();
        if (!importedMap.isEmpty()) {
            var changed = false;
            var it = ae2helpers$expectedResults.entrySet().iterator();
            
            while (it.hasNext()) {
                var entry = it.next();
                var key = entry.getKey(); // the imported resource kind
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
            return true;
        }
        
        return false;
    }
}