package rearth.ae2helpers.util;

import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.util.prioritylist.IPartitionList;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PatternProviderImportContext implements StackTransferContext {
    private final IStorageService internalStorage;
    private final IEnergySource energySource;
    private final IActionSource actionSource;
    
    // tracks remaining crafting results
    private final Map<AEKey, Long> expectedResults;
    // what was actually moved yet
    private final Map<AEKey, Long> importedItems = new HashMap<>();
    
    private final int initialOperations;
    private int operationsRemaining;
    
    public PatternProviderImportContext(IStorageService internalStorage,
                                        IEnergySource energySource,
                                        IActionSource actionSource,
                                        Map<AEKey, Long> expectedResults) {
        this.internalStorage = internalStorage;
        this.energySource = energySource;
        this.actionSource = actionSource;
        this.expectedResults = expectedResults;
        
        initialOperations = 64;
        operationsRemaining = 64;
    }
    
    public Map<AEKey, Long> getImportedItems() {
        return importedItems;
    }
    
    @Override
    public IStorageService getInternalStorage() {
        return internalStorage;
    }
    
    @Override
    public IEnergySource getEnergySource() {
        return energySource;
    }
    
    @Override
    public IActionSource getActionSource() {
        return actionSource;
    }
    
    @Override
    public int getOperationsRemaining() {
        return operationsRemaining;
    }
    
    @Override
    public void setOperationsRemaining(int operationsRemaining) {
        this.operationsRemaining = operationsRemaining;
    }
    
    @Override
    public void reduceOperationsRemaining(long inserted) {
        this.operationsRemaining -= (int) inserted;
    }
    
    @Override
    public boolean hasOperationsLeft() {
        return operationsRemaining > 0;
    }
    
    @Override
    public boolean hasDoneWork() {
        return initialOperations > operationsRemaining;
    }
    
    @Override
    public boolean isKeyTypeEnabled(AEKeyType space) {
        return true;
    }
    
    @Override
    public boolean isInFilter(AEKey key) {
        return expectedResults.containsKey(key);
    }
    
    @Override
    public @Nullable IPartitionList getFilter() {
        return null;
    }
    
    @Override
    public void setInverted(boolean inverted) {
    }
    
    @Override
    public boolean isInverted() {
        return false;
    }
    
    @Override
    public boolean canInsert(AEItemKey what, long amount) {
        long inserted = internalStorage.getInventory().insert(
          what,
          amount,
          Actionable.SIMULATE,
          actionSource);
        
        if (inserted > 0) {
            importedItems.merge(what, inserted, Long::sum);
            return true;
        }
        return false;
    }
}