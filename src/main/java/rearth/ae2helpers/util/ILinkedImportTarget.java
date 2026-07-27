package rearth.ae2helpers.util;

// Implemented by the pattern provider logic (via mixin). A linked import bus calls this when it imports,
// so a provider whose result returns via that bus (e.g. at the end of a Create chain) keeps its
// redstone signal alive even though the result never passes through the provider itself.
public interface ILinkedImportTarget {
    void ae2helpers$onLinkedImport();
}
