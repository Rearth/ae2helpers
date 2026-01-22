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
import java.util.Iterator;
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
    
    // Backoff logic
    @Unique private int ae2helpers$ticksSinceLastImport = 0;
    @Unique private int ae2helpers$currentPollDelay = 5; // Start fast
    
    // Backoff config
    @Unique private static final int AEHELPERS$MIN_DELAY = 5;
    @Unique private static final int AEHELPERS$MAX_DELAY = 100; // Cap at 5 seconds
    
    // Inject into pushPattern to record what we expect to receive
    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void ae2helpers$onPushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir) {
        // Only if the push was successful
        if (cir.getReturnValue()) {
            for (var output : patternDetails.getOutputs()) {
                if (output != null) {
                    ae2helpers$expectedResults.merge(output.what(), output.amount(), Long::sum);
                }
            }
            
            // Reset backoff to aggressive polling because a new machine cycle started
            ae2helpers$currentPollDelay = AEHELPERS$MIN_DELAY;
            ae2helpers$ticksSinceLastImport = 0;
            this.saveChanges();
            
            this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        }
    }
    
    @Inject(method = "doWork", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$onDoWork(CallbackInfoReturnable<Boolean> cir) {
        if (!this.mainNode.isActive()) return;
        
        // Only tick if we are actually waiting for something
        if (ae2helpers$expectedResults.isEmpty()) return;
        
        ae2helpers$ticksSinceLastImport++;
        
        if (ae2helpers$ticksSinceLastImport >= ae2helpers$currentPollDelay) {
            ae2helpers$ticksSinceLastImport = 0;
            
            boolean didWork = ae2helpers$doImportWork();
            ae2helpers.LOGGER.info("Import Success: " + didWork);
            
            if (didWork) {
                // We found items! Keep polling fast to grab the rest.
                ae2helpers$currentPollDelay = AEHELPERS$MIN_DELAY;
                cir.setReturnValue(true);
            } else {
                // No items found yet. Slow down checking to save Tick Time.
                ae2helpers$currentPollDelay = Math.min(AEHELPERS$MAX_DELAY, (int)(ae2helpers$currentPollDelay * 1.5));
            }
        }
    }
    
    @Inject(method = "hasWorkToDo", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$hasWorkToDo(CallbackInfoReturnable<Boolean> cir) {
        // Only keep the provider awake if we are explicitly waiting for results
        if (!ae2helpers$expectedResults.isEmpty()) {
            cir.setReturnValue(true);
        }
        
        cir.setReturnValue(true);
        
        // todo check if this conflicts with the original method?
    }
    
    @Inject(method = "clearContent", at = @At("HEAD"))
    private void ae2helpers$onClearContent(CallbackInfo ci) {
        this.ae2helpers$importStrategy = null;
        this.ae2helpers$currentSide = null;
        this.ae2helpers$expectedResults.clear();
    }
    
    // Save expected results so we don't lose them on restart
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
        
        // create import strategy (if new load or side config changed)
        if (this.ae2helpers$importStrategy == null || this.ae2helpers$currentSide != side) {
            var be = this.host.getBlockEntity();
            if (be == null || be.getLevel() == null) return false;
            
            var level = (ServerLevel) be.getLevel();
            var pos = be.getBlockPos();
            
            this.ae2helpers$importStrategy = StackWorldBehaviors.createImportFacade(
              level,
              pos.relative(side),
              side.getOpposite(),
              (key) -> true // filters the type of imports, e.g. items or fluids. We import all.
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
        
        // update expectations map based on actual imports
        var importedMap = context.getImportedItems();
        if (!importedMap.isEmpty()) {
            var changed = false;
            var it = ae2helpers$expectedResults.entrySet().iterator();
            
            while (it.hasNext()) {
                var entry = it.next();
                var key = entry.getKey();   // the imported resource kind
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