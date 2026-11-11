package rearth.ae2helpers.mixin.importcard;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.parts.automation.StackWorldBehaviors;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.util.CraftingResultTracker;
import rearth.ae2helpers.util.IPatternProviderUpgradeHost;
import rearth.ae2helpers.util.ImportCardConfig;
import rearth.ae2helpers.util.PatternProviderImportContext;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Mixin(PatternProviderLogic.class)
public abstract class PatternProviderImportMixin implements IPatternProviderUpgradeHost {
    @Shadow @Final private IManagedGridNode mainNode;
    @Shadow @Final private IActionSource actionSource;
    @Shadow @Final private PatternProviderLogicHost host;
    @Shadow public abstract void saveChanges();
    @Shadow private void onStackReturnedToNetwork(GenericStack stack) {
        throw new AssertionError();
    }

    @Unique private final Map<Direction, StackImportStrategy> ae2helpers$importStrategies = new EnumMap<>(Direction.class);
    @Unique private final CraftingResultTracker ae2helpers$results = new CraftingResultTracker();
    @Unique private Direction ae2helpers$lastUsedConfigDirection;
    @Unique private IUpgradeInventory ae2helpers$upgradeSlots = UpgradeInventories.empty();
    @Unique private int ae2helpers$nextImportSide;
    @Unique private int ae2helpers$cyclesSinceLastCheck;
    @Unique private float ae2helpers$currentCycleDelay = 1;
    @Unique private static final int AEHELPERS$MAX_CYCLE_DELAY = 10;

    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/patternprovider/PatternProviderLogicHost;I)V", at = @At("TAIL"))
    private void ae2helpers$initUpgrade(IManagedGridNode mainNode, PatternProviderLogicHost host, int patternInventorySize, CallbackInfo ci) {
        ae2helpers$upgradeSlots = UpgradeInventories.forMachine(ae2helpers.RESULT_IMPORT_CARD, 1, this::ae2helpers$onUpgradeChanged);
    }

    @Unique
    private void ae2helpers$onUpgradeChanged() {
        if (!ae2helpers$hasImportCard()) ae2helpers$results.clear();
        ae2helpers$importStrategies.clear();
        ae2helpers$wake();
        saveChanges();
    }

    @Unique
    private void ae2helpers$wake() {
        ae2helpers$currentCycleDelay = 1;
        ae2helpers$cyclesSinceLastCheck = 0;
        mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    // Use AE2's success hook so each accepted push is counted once, including subclass paths using this hook.
    @Inject(method = "onPushPatternSuccess", at = @At("HEAD"))
    private void ae2helpers$onPushPattern(IPatternDetails pattern, CallbackInfo ci) {
        if (!ae2helpers$hasImportCard()) return;
        var level = host.getBlockEntity().getLevel();
        if (level == null) return;
        for (var output : pattern.getOutputs()) {
            if (output != null) ae2helpers$results.expect(output.what(), output.amount(), level.getGameTime());
        }
        ae2helpers$wake();
        saveChanges();
    }

    @Inject(method = "onStackReturnedToNetwork", at = @At("HEAD"))
    private void ae2helpers$onStackReturned(GenericStack stack, CallbackInfo ci) {
        var level = host.getBlockEntity().getLevel();
        if (level != null && ae2helpers$results.imported(stack.what(), stack.amount(), level.getGameTime())) saveChanges();
    }

    @Inject(method = "doWork", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$onDoWork(CallbackInfoReturnable<Boolean> cir) {
        if (!mainNode.isActive() || !ae2helpers$hasImportCard()) return;
        var config = ae2helpers$getConfig();
        if (config.resultsOnly() && ae2helpers$results.isEmpty()) return;
        if (++ae2helpers$cyclesSinceLastCheck < (int) ae2helpers$currentCycleDelay) return;
        ae2helpers$cyclesSinceLastCheck = 0;

        if (ae2helpers$doImportWork(config)) {
            ae2helpers$currentCycleDelay = 1;
            cir.setReturnValue(true);
        } else {
            ae2helpers$currentCycleDelay = Math.min(AEHELPERS$MAX_CYCLE_DELAY, ae2helpers$currentCycleDelay * 1.15f);
        }

        var level = host.getBlockEntity().getLevel();
        var grid = mainNode.getGrid();
        if (level != null && grid != null && ae2helpers$results.cleanup(level.getGameTime(),
                key -> grid.getCraftingService().getRequestedAmount(key) > 0)) {
            saveChanges();
        }
    }

    @Inject(method = "hasWorkToDo", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$hasWorkToDo(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && ae2helpers$hasImportCard()
                && (!ae2helpers$getConfig().resultsOnly() || !ae2helpers$results.isEmpty())) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean ae2helpers$doImportWork(ImportCardConfig config) {
        var targets = host.getTargets();
        var grid = mainNode.getGrid();
        var be = host.getBlockEntity();
        if (targets.isEmpty() || grid == null || !(be.getLevel() instanceof ServerLevel level)) return false;

        if (ae2helpers$lastUsedConfigDirection != config.overriddenDirection()) {
            ae2helpers$importStrategies.clear();
            ae2helpers$lastUsedConfigDirection = config.overriddenDirection();
        }
        ae2helpers$importStrategies.keySet().removeIf(side -> !targets.contains(side));
        var context = new PatternProviderImportContext(grid.getStorageService(), grid.getEnergyService(),
                actionSource, ae2helpers$results.keys(), config.resultsOnly());

        // All neighbours share one operation budget. Rotate the first side so a busy machine cannot starve others.
        var sides = Direction.values();
        int firstSide = ae2helpers$nextImportSide;
        ae2helpers$nextImportSide = (firstSide + 1) % sides.length;
        for (int i = 0; i < sides.length && context.hasOperationsLeft(); i++) {
            var side = sides[(firstSide + i) % sides.length];
            if (!targets.contains(side)) continue;
            var strategy = ae2helpers$importStrategies.computeIfAbsent(side, direction ->
                    StackWorldBehaviors.createImportFacade(level, be.getBlockPos().relative(direction),
                            config.overriddenDirection() == null ? direction.getOpposite() : config.overriddenDirection(),
                            type -> true));
            strategy.transfer(context);
        }

        var imported = context.getImportedItems();
        if (imported.isEmpty()) return false;
        for (var entry : imported.entrySet()) {
            // Use the same hook as AE2's return buffer: unlock result-gated crafts and update our tally once.
            onStackReturnedToNetwork(new GenericStack(entry.getKey(), entry.getValue()));
        }
        saveChanges();
        return true;
    }

    @Unique
    private ImportCardConfig ae2helpers$getConfig() {
        return ae2helpers$upgradeSlots.getStackInSlot(0).getOrDefault(ae2helpers.IMPORT_CARD_CONFIG.get(), ImportCardConfig.DEFAULT);
    }

    @Unique
    private boolean ae2helpers$hasImportCard() {
        return ae2helpers$upgradeSlots.getStackInSlot(0).is(ae2helpers.RESULT_IMPORT_CARD.get());
    }

    @Inject(method = "clearContent", at = @At("HEAD"))
    private void ae2helpers$onClearContent(CallbackInfo ci) {
        ae2helpers$importStrategies.clear();
        ae2helpers$results.clear();
        ae2helpers$upgradeSlots.clear();
    }

    @Inject(method = "addDrops", at = @At("TAIL"))
    private void ae2helpers$dropUpgrade(List<ItemStack> drops, CallbackInfo ci) {
        for (var stack : ae2helpers$upgradeSlots) {
            if (!stack.isEmpty()) drops.add(stack);
        }
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void ae2helpers$writeToNBT(ValueOutput output, CallbackInfo ci) {
        ae2helpers$results.write(output);
        ae2helpers$upgradeSlots.writeToNBT(output, "ae2helperupgrades");
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void ae2helpers$readFromNBT(ValueInput input, CallbackInfo ci) {
        ae2helpers$upgradeSlots.readFromNBT(input, "ae2helperupgrades");
        ae2helpers$results.read(input);
        ae2helpers$importStrategies.clear();
    }

    @Override
    public IUpgradeInventory ae2helpers$getUpgradeInventory() {
        return ae2helpers$upgradeSlots;
    }
}
