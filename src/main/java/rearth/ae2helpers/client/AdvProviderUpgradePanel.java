package rearth.ae2helpers.client;

import appeng.api.upgrades.Upgrades;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.menu.AEBaseMenu;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.mixin.importcard.ScreenStyleAccessor;
import rearth.ae2helpers.util.IPatternProviderUpgradeHost;

import java.util.ArrayList;
import java.util.List;

// Shared by the (regular + small) Advanced AE pattern provider screen mixins to add the import card panel.
public final class AdvProviderUpgradePanel {

    private AdvProviderUpgradePanel() {
    }

    public static void install(ScreenStyle style, WidgetContainer widgets, AEBaseMenu menu) {
        var existingStyle = style.getWidget("upgrades");

        if (style instanceof ScreenStyleAccessor accessor) {
            WidgetStyle upgradeStyle = existingStyle;
            if (ModList.get().isLoaded("appflux") || ModList.get().isLoaded("mesoulcard")) {
                upgradeStyle = new WidgetStyle();
                upgradeStyle.setLeft(existingStyle.getLeft());
                upgradeStyle.setRight(existingStyle.getRight());
                upgradeStyle.setTop(existingStyle.getTop() + existingStyle.getHeight() + 32);
                upgradeStyle.setHeight(existingStyle.getHeight());
                upgradeStyle.setWidth(existingStyle.getWidth());
                upgradeStyle.setHideEdge(existingStyle.isHideEdge());
            }

            accessor.ae2helpers$getWidgets().put("importupgrades", upgradeStyle);
        }

        widgets.add("importupgrades", new UpgradesPanel(
          menu.getSlots(ae2helpers.IMPORT_UPGRADE),
          () -> ae2helpers$getCompatibleUpgrades(menu)
        ));
    }

    private static List<Component> ae2helpers$getCompatibleUpgrades(AEBaseMenu menu) {
        var list = new ArrayList<Component>();
        list.add(GuiText.CompatibleUpgrades.text());

        if (menu.getTarget() instanceof AdvPatternProviderLogicHost host
              && host.getLogic() instanceof IPatternProviderUpgradeHost upgradeHost) {
            var inventory = upgradeHost.ae2helpers$getUpgradeInventory();
            list.addAll(Upgrades.getTooltipLinesForMachine(inventory.getUpgradableItem()));
        }

        return list;
    }
}
