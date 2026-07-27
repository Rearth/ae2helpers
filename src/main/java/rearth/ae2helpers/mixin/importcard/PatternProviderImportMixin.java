package rearth.ae2helpers.mixin.importcard;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
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
import rearth.ae2helpers.util.ILinkedImportTarget;
import rearth.ae2helpers.util.IPatternProviderUpgradeHost;
import rearth.ae2helpers.util.IProviderRedstoneHost;
import rearth.ae2helpers.util.ImportCardConfig;
import rearth.ae2helpers.util.PatternProviderImportContext;
import rearth.ae2helpers.util.RedstoneCardConfig;
import rearth.ae2helpers.util.RedstoneMode;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(PatternProviderLogic.class)
public abstract class PatternProviderImportMixin implements IPatternProviderUpgradeHost, IProviderRedstoneHost, ILinkedImportTarget {

    @Shadow @Final private IManagedGridNode mainNode;
    @Shadow @Final private IActionSource actionSource;
    @Shadow @Final private PatternProviderLogicHost host;
    @Shadow public abstract void saveChanges();
    
    // cached import strategy per provider target side
    @Unique private final Map<Direction, StackImportStrategy> ae2helpers$importStrategies = new EnumMap<>(Direction.class);

    // used to detect config changes (invalidates all cached strategies)
    @Unique private Direction ae2helpers$lastUsedConfigDirection;
    
    @Unique private IUpgradeInventory ae2helpers$upgradeSlots = UpgradeInventories.empty();
    @Unique private final Map<AEKey, Long> ae2helpers$expectedResults = new HashMap<>();

    // Keys the crafting service has actually tracked (getRequestedAmount > 0) at least once.
    // Only then is a later "0" a trustworthy "craft finished" signal.
    @Unique private final java.util.Set<AEKey> ae2helpers$confirmedCrafts = new java.util.HashSet<>();

    // redstone emission state, recomputed each tick; neighbor updates fire only on change
    @Unique private boolean ae2helpers$emitting = false;
    @Unique private boolean ae2helpers$emittingStrong = false;
    @Unique private boolean ae2helpers$wasCraftingActive = false;
    @Unique private long ae2helpers$pulseEndTick = 0;
    @Unique private long ae2helpers$lastActiveTick = Long.MIN_VALUE;
    @Unique private static final long AEHELPERS$REDSTONE_LINGER = 60;

    // outputs this provider is producing for the current craft; lets the redstone signal bridge the gaps
    // between pattern pushes (expectedResults briefly empties while AE2 hasn't re-pushed the next pattern)
    // as long as AE2 still requests the output, so Pulse mode doesn't fire a spurious finish pulse mid-craft.
    @Unique private final java.util.Set<AEKey> ae2helpers$activeCraftKeys = new java.util.HashSet<>();

    @Unique private int ae2helpers$cyclesSinceLastCheck = 0;
    @Unique private float ae2helpers$currentCycleDelay = 1f;
    @Unique private static final int AEHELPERS$MAX_CYCLE_DELAY = 10;

    // last game tick something was actually imported; used so we keep importing a machine's surplus
    // even after AE2 already considers the craft finished (only drop expectations once it stops yielding).
    @Unique private long ae2helpers$lastImportTick = Long.MIN_VALUE;
    @Unique private static final long AEHELPERS$IMPORT_IDLE_TIMEOUT = 200;

    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/patternprovider/PatternProviderLogicHost;I)V",at = @At("TAIL"))
    private void ae2helpers$initUpgrade(IManagedGridNode mainNode, PatternProviderLogicHost host, int patternInventorySize, CallbackInfo ci) {
        this.ae2helpers$upgradeSlots = UpgradeInventories.forMachine(ae2helpers.RESULT_IMPORT_CARD, 2, this::ae2helpers$onUpgradesChanged);
    }

    @Unique
    private void ae2helpers$onUpgradesChanged() {
        this.saveChanges();
        // recompute emission on the spot so a card change (esp. pulling the redstone card) resets the signal
        // immediately, instead of relying on the device to tick doWork again - which may never happen if it sleeps
        ae2helpers$updateRedstone();
        // wake the device so the redstone state gets re-checked after a card change
        this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }
    
    // AE2 calls onPushPatternSuccess at the end of every successful pushPattern. Hooking here rather than
    // pushPattern's RETURN also covers AppliedCreate's Mechanical Crafting provider, which bypasses
    // super.pushPattern entirely and invokes onPushPatternSuccess via reflection - so its crafts register too.
    @Inject(method = "onPushPatternSuccess", at = @At("HEAD"))
    private void ae2helpers$onPushPattern(IPatternDetails patternDetails, CallbackInfo ci) {
        // track pending outputs for either card: the import card pulls them, the redstone card only needs
        // to know something is still being produced (decremented by imports or the provider's own returns).
        if (!ae2helpers$hasImportCard() && !ae2helpers$hasRedstoneCard()) return;

        for (var output : patternDetails.getOutputs()) {
            if (output != null) {
                ae2helpers$expectedResults.merge(output.what(), output.amount(), Long::sum);
                ae2helpers$activeCraftKeys.add(output.what());
            }
        }

        ae2helpers$currentCycleDelay = 1;
        ae2helpers$cyclesSinceLastCheck = 0;

        // treat a fresh push as recent activity so the idle timeout doesn't drop the new expectation
        var be = this.host.getBlockEntity();
        if (be != null && be.getLevel() != null) ae2helpers$lastImportTick = be.getLevel().getGameTime();

        this.saveChanges();

        this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    // case 2: the machine pushes its output back through the provider's own return path -> count it here
    // so the redstone card works standalone (and the import card reduces its target accordingly).
    // case 3: the result comes back via a separate import bus that has a redstone link card pointing at us
    @Override
    public void ae2helpers$onLinkedImport() {
        var be = this.host.getBlockEntity();
        if (be != null && be.getLevel() != null) ae2helpers$lastImportTick = be.getLevel().getGameTime();
    }

    @Inject(method = "onStackReturnedToNetwork", at = @At("HEAD"))
    private void ae2helpers$onStackReturned(GenericStack stack, CallbackInfo ci) {
        if (stack == null || ae2helpers$expectedResults.isEmpty()) return;
        ae2helpers$expectedResults.computeIfPresent(stack.what(), (k, v) -> {
            var remaining = v - stack.amount();
            return remaining > 0 ? remaining : null;
        });
        var be = this.host.getBlockEntity();
        if (be != null && be.getLevel() != null) ae2helpers$lastImportTick = be.getLevel().getGameTime();
    }

    @Unique
    private void ae2helpers$syncWithCraftingService() {
        var grid = this.mainNode.getGrid();
        if (grid == null) return;
        
        var craftingService = grid.getCraftingService();
        if (craftingService == null) return;

        var be = this.host.getBlockEntity();
        var gameTime = (be != null && be.getLevel() != null) ? be.getLevel().getGameTime() : 0L;
        // lazily anchor the idle timer (never set / not restored from NBT) to avoid a MIN_VALUE overflow
        if (ae2helpers$lastImportTick == Long.MIN_VALUE) ae2helpers$lastImportTick = gameTime;
        var idleTicks = gameTime - ae2helpers$lastImportTick;

        // forget confirmations for keys we no longer expect
        ae2helpers$confirmedCrafts.retainAll(ae2helpers$expectedResults.keySet());

        var it = ae2helpers$expectedResults.entrySet().iterator();
        var changed = false;

        while (it.hasNext()) {
            var entry = it.next();
            var key = entry.getKey();
            var totalRequested = craftingService.getRequestedAmount(key);

            if (totalRequested > 0) {
                // AE2 still tracks the craft; do NOT cap the expectation to it. The provider already pushed
                // the inputs, so the machine will produce the full recorded amount regardless of what AE2
                // still "wants" (it finishes the job once the request is delivered, leaving surplus behind).
                ae2helpers$confirmedCrafts.add(key);
            } else if (ae2helpers$confirmedCrafts.contains(key) && idleTicks > AEHELPERS$IMPORT_IDLE_TIMEOUT) {
                // Only give up once AE2 finished the craft AND the machine stopped yielding for a while.
                it.remove();
                ae2helpers$confirmedCrafts.remove(key);
                changed = true;
            }
        }

        if (changed) this.saveChanges();
    }
    
    @Inject(method = "doWork", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$onDoWork(CallbackInfoReturnable<Boolean> cir) {
        // must run first: this inject cancels doWork on a successful import, which would skip any later
        // RETURN inject - so the redstone update has to happen here, before setReturnValue.
        ae2helpers$updateRedstone();

        if (!this.mainNode.isActive()) return;
        
        if (!ae2helpers$hasImportCard()) {
            if (ae2helpers$hasRedstoneCard()) {
                // redstone card alone: no importing, but keep the return tracking cleaned up
                if (!ae2helpers$expectedResults.isEmpty()) ae2helpers$syncWithCraftingService();
            } else if (!ae2helpers$expectedResults.isEmpty()) {
                ae2helpers$expectedResults.clear();
                ae2helpers$confirmedCrafts.clear();
                this.saveChanges();
            }
            return;
        }

        var config = ae2helpers$getConfig();
        
        // If we are in "Result Only" mode AND have no expectations, we sleep.
        // If "Result Only" is FALSE (Import Everything), we must run even if map is empty.
        if (config.resultsOnly() && ae2helpers$expectedResults.isEmpty()) return;
        
        ae2helpers$cyclesSinceLastCheck++;
        
        if (ae2helpers$cyclesSinceLastCheck >= (int) ae2helpers$currentCycleDelay) {
            ae2helpers$cyclesSinceLastCheck = 0;
            
            var didWork = ae2helpers$doImportWork(config);
            
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
        if (cir.getReturnValue()) return;

        // stay awake only while there is redstone work (a craft is pending or a pulse is still running),
        // then sleep like the import card - the device is re-alerted on the next pushPattern / card change
        if (ae2helpers$hasRedstoneWork()) {
            cir.setReturnValue(true);
            return;
        }

        // If AE2 thinks it's asleep, check if we need to wake up
        if (ae2helpers$hasImportCard()) {
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

        var be = this.host.getBlockEntity();
        if (be == null || be.getLevel() == null) return false;

        var level = (ServerLevel) be.getLevel();
        var pos = be.getBlockPos();

        var overriddenSide = config.overriddenDirection();

        // Which neighbours to import from: the explicit "Import Side", or every side the provider pushes to.
        // With an override the result is pulled from that side even if the provider pushes somewhere else.
        Collection<Direction> importSides = overriddenSide != null ? List.of(overriddenSide) : targets;

        // Config direction changed -> drop all cached strategies so they get rebuilt for the new side.
        if (this.ae2helpers$lastUsedConfigDirection != overriddenSide) {
            this.ae2helpers$importStrategies.clear();
            this.ae2helpers$lastUsedConfigDirection = overriddenSide;
        }

        // Drop cached strategies for sides we no longer import from (push direction / override changed).
        this.ae2helpers$importStrategies.keySet().removeIf(side -> !importSides.contains(side));

        var context = new PatternProviderImportContext(
          this.mainNode.getGrid().getStorageService(),
          this.mainNode.getGrid().getEnergyService(),
          this.actionSource,
          this.ae2helpers$expectedResults,
          config.resultsOnly() // Pass the mode
        );

        // Only the side(s) with a real machine will yield anything; in "results only" mode the context
        // filter additionally restricts to expected results.
        for (var side : importSides) {
            var strategy = this.ae2helpers$importStrategies.computeIfAbsent(side, s ->
                StackWorldBehaviors.createImportFacade(level, pos.relative(s), s.getOpposite(), (type) -> true));

            strategy.transfer(context);

            if (!context.hasOperationsLeft()) break;
        }

        var importedMap = context.getImportedItems();

        if (!importedMap.isEmpty()) {
            ae2helpers$lastImportTick = level.getGameTime();
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
        var stack = ae2helpers$findCard(ae2helpers.RESULT_IMPORT_CARD.get());
        if (stack == null) return ImportCardConfig.DEFAULT;
        return stack.getOrDefault(ae2helpers.IMPORT_CARD_CONFIG.get(), ImportCardConfig.DEFAULT);
    }

    @Unique
    private net.minecraft.world.item.ItemStack ae2helpers$findCard(net.minecraft.world.item.Item card) {
        for (int i = 0; i < ae2helpers$upgradeSlots.size(); i++) {
            var stack = ae2helpers$upgradeSlots.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(card)) return stack;
        }
        return null;
    }

    @Unique
    private boolean ae2helpers$hasImportCard() {
        return ae2helpers$findCard(ae2helpers.RESULT_IMPORT_CARD.get()) != null;
    }

    @Unique
    private boolean ae2helpers$hasRedstoneCard() {
        return ae2helpers$findCard(ae2helpers.REDSTONE_CARD.get()) != null;
    }

    @Override
    public boolean ae2helpers$isEmittingRedstone() {
        return ae2helpers$emitting;
    }

    @Override
    public boolean ae2helpers$isEmittingStrongRedstone() {
        return ae2helpers$emittingStrong;
    }

    @Override
    public Direction ae2helpers$getRedstoneSide() {
        var config = ae2helpers$getRedstoneConfig();
        return config == null ? null : config.side();
    }

    @Override
    public int ae2helpers$getRedstoneSignalStrength() {
        var config = ae2helpers$getRedstoneConfig();
        return config == null ? RedstoneCardConfig.DEFAULT_SIGNAL_STRENGTH : config.signalStrength();
    }

    @Unique
    private RedstoneCardConfig ae2helpers$getRedstoneConfig() {
        var stack = ae2helpers$findCard(ae2helpers.REDSTONE_CARD.get());
        if (stack == null) return null;
        return stack.getOrDefault(ae2helpers.REDSTONE_CARD_CONFIG.get(), RedstoneCardConfig.DEFAULT);
    }

    @Unique
    private boolean ae2helpers$isCraftingActive() {
        // driven by what THIS provider pushed and is still waiting to come back (populated in pushPattern,
        // reduced by imports / provider returns / a linked import bus, expired by the idle timeout). While that
        // map briefly empties between pattern pushes, bridge the gap as long as AE2 still requests our output -
        // otherwise Pulse mode would see a false craft-end edge and fire a spurious finish pulse mid-craft.
        if (!ae2helpers$expectedResults.isEmpty()) return true;
        return !ae2helpers$activeCraftKeys.isEmpty() && ae2helpers$stillCraftingViaGrid();
    }

    @Unique
    private boolean ae2helpers$stillCraftingViaGrid() {
        var grid = this.mainNode.getGrid();
        if (grid == null) return false;
        var craftingService = grid.getCraftingService();
        if (craftingService == null) return false;
        for (var key : ae2helpers$activeCraftKeys) {
            if (craftingService.isRequesting(key)) return true;
        }
        return false;
    }

    @Unique
    private boolean ae2helpers$hasRedstoneWork() {
        if (!ae2helpers$hasRedstoneCard()) return false;
        // stay awake while crafting - incl. the gaps between pattern pushes bridged via the crafting service,
        // so updateRedstone keeps refreshing lastActiveTick and Pulse doesn't fire a false mid-craft finish
        if (ae2helpers$isCraftingActive()) return true;
        var be = this.host.getBlockEntity();
        var now = (be != null && be.getLevel() != null) ? be.getLevel().getGameTime() : 0L;
        if (ae2helpers$pulseEndTick > now) return true;
        // stay awake through the post-craft linger so its expiry is processed (esp. the Pulse-mode end pulse,
        // which otherwise never fires because updateRedstone stops running once the device sleeps)
        if (ae2helpers$lastActiveTick != Long.MIN_VALUE) {
            var sinceActive = now - ae2helpers$lastActiveTick;
            if (sinceActive >= 0 && sinceActive < AEHELPERS$REDSTONE_LINGER) return true;
        }
        // stay awake until the emission has settled to its idle value (handles the craft-end transition
        // regardless of tick ordering); once settled the block keeps reporting the stored state while asleep
        return ae2helpers$emitting != ae2helpers$idleEmitting();
    }

    @Unique
    private boolean ae2helpers$idleEmitting() {
        var config = ae2helpers$getRedstoneConfig();
        return config != null && config.mode() == RedstoneMode.INVERTED;
    }

    @Unique
    private void ae2helpers$updateRedstone() {
        var be = this.host.getBlockEntity();
        var level = be != null ? be.getLevel() : null;
        var config = ae2helpers$getRedstoneConfig();

        boolean newEmitting;
        if (config == null || level == null) {
            newEmitting = false;
            ae2helpers$wasCraftingActive = false;
        } else {
            var active = ae2helpers$isCraftingActive();
            if (active) {
                ae2helpers$lastActiveTick = level.getGameTime();
            } else {
                // craft truly finished (nothing pending and AE2 no longer requests our output) -> reset so the
                // next craft starts a clean active window (and one finish pulse fires after the linger)
                ae2helpers$activeCraftKeys.clear();
            }
            // keep the state steady for a short linger after crafting stops, so back-to-back crafts don't
            // rapidly toggle the signal (every toggle is a neighbor update -> avoids TPS churn with many machines)
            var effectiveActive = active;
            if (!active && ae2helpers$lastActiveTick != Long.MIN_VALUE) {
                var sinceActive = level.getGameTime() - ae2helpers$lastActiveTick;
                if (sinceActive >= 0 && sinceActive < AEHELPERS$REDSTONE_LINGER) effectiveActive = true;
            }
            switch (config.mode()) {
                case INVERTED -> newEmitting = !effectiveActive;
                case PULSE -> {
                    if (effectiveActive != ae2helpers$wasCraftingActive) {
                        ae2helpers$pulseEndTick = level.getGameTime() + Math.max(1, config.pulseLength());
                    }
                    newEmitting = level.getGameTime() < ae2helpers$pulseEndTick;
                }
                default -> newEmitting = effectiveActive;
            }
            ae2helpers$wasCraftingActive = effectiveActive;
        }
        var newStrong = newEmitting && config != null && config.strongSignal();

        if (newEmitting == ae2helpers$emitting && newStrong == ae2helpers$emittingStrong) return;
        ae2helpers$emitting = newEmitting;
        ae2helpers$emittingStrong = newStrong;

        if (level == null || level.isClientSide) return;
        level.updateNeighborsAt(be.getBlockPos(), be.getBlockState().getBlock());
    }
    
    @Inject(method = "clearContent", at = @At("HEAD"))
    private void ae2helpers$onClearContent(CallbackInfo ci) {
        this.ae2helpers$importStrategies.clear();
        this.ae2helpers$lastUsedConfigDirection = null;
        this.ae2helpers$expectedResults.clear();
        this.ae2helpers$confirmedCrafts.clear();
        this.ae2helpers$activeCraftKeys.clear();
        this.ae2helpers$upgradeSlots.clear();
    }
    
    @Inject(method = "addDrops", at = @At("TAIL"))
    private void ae2helpers$dropUpgrade(List<ItemStack> drops, CallbackInfo ci) {
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
        // restored expectations were confirmed crafts when saved; re-seed so the idle timeout can retire
        // them if their craft job no longer exists after the reload (else the signal could stick forever)
        ae2helpers$confirmedCrafts.clear();
        ae2helpers$confirmedCrafts.addAll(ae2helpers$expectedResults.keySet());
        ae2helpers$upgradeSlots.readFromNBT(tag, "ae2helperupgrades", registries);
    }
    
    @Override
    public IUpgradeInventory ae2helpers$getUpgradeInventory() {
        return ae2helpers$upgradeSlots;
    }
}