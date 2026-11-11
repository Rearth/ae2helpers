package rearth.ae2helpers.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.network.UpdateImportCardPacket;
import rearth.ae2helpers.util.ImportCardConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class ImportCardScreen extends Screen {
    
    private final InteractionHand hand;
    private ImportCardConfig currentConfig;
    private Checkbox resultsBox;
    private CycleButton<Optional<Direction>> directionButton;
    
    public ImportCardScreen(ItemStack stack, InteractionHand hand) {
        super(Component.translatable("ae2helpers.importcard.screen.title"));
        this.hand = hand;
        this.currentConfig = stack.getOrDefault(ae2helpers.IMPORT_CARD_CONFIG.get(), ImportCardConfig.DEFAULT);
    }
    
    @Override
    protected void init() {
        super.init();
        
        var left = this.width / 2 - 110;
        var startY = this.height / 2 - 25;
        
        var resultsTooltip = Tooltip.create(Component.translatable("ae2helpers.importcard.resultsonly.tooltip"));
        
        resultsBox = Checkbox.builder(Component.translatable("ae2helpers.importcard.resultsonly"), font)
                           .pos(left, startY)
                           .selected(currentConfig.resultsOnly())
                           .onValueChange((box, val) -> updateConfig(val, currentConfig.overriddenDirection()))
                           .build();
        // Checkbox.Builder only applies its tooltip when the label overflows in 26.1.2.
        resultsBox.setTooltip(resultsTooltip);
        this.addRenderableWidget(resultsBox);
        
        var dirTooltip = Tooltip.create(Component.translatable("ae2helpers.importcard.direction.tooltip"));
        
        var options = new ArrayList<Optional<Direction>>();
        options.add(Optional.empty());
        options.addAll(Arrays.stream(Direction.values()).map(Optional::of).toList());
        
        directionButton = CycleButton.<Optional<Direction>>builder(opt -> getDirectionName(opt.orElse(null)), Optional.ofNullable(currentConfig.overriddenDirection()))
                          .withValues(options)
                          .withTooltip(val -> dirTooltip)
                          .create(left, startY + 30, 220, 20, Component.translatable("ae2helpers.importcard.direction"),
                            (btn, val) -> updateConfig(currentConfig.resultsOnly(), val.orElse(null)));
        
        this.addRenderableWidget(directionButton);
    }
    
    private Component getDirectionName(Direction dir) {
        if (dir == null) return Component.translatable("ae2helpers.importcard.direction.auto");
        return Component.literal(dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1));
    }
    
    private void updateConfig(boolean resultsOnly, Direction dir) {
        this.currentConfig = new ImportCardConfig(resultsOnly, dir);
        ClientPacketDistributor.sendToServer(new UpdateImportCardPacket(hand, currentConfig));
    }
    
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        extractWideTooltip(guiGraphics, resultsBox, "ae2helpers.importcard.resultsonly.tooltip", mouseX, mouseY);
        extractWideTooltip(guiGraphics, directionButton, "ae2helpers.importcard.direction.tooltip", mouseX, mouseY);
    }

    private void extractWideTooltip(GuiGraphicsExtractor graphics, AbstractWidget widget, String translationKey,
                                    int mouseX, int mouseY) {
        if (!widget.isHovered() && !(widget.isFocused() && minecraft.getLastInputType().isKeyboard())) return;
        // Keep the widget tooltip for narration, but replace vanilla's fixed 170-pixel text wrapping.
        var lines = font.split(Component.translatable(translationKey), Math.min(300, width - 24));
        var positioner = widget.isHovered() ? new MenuTooltipPositioner(widget.getRectangle())
                : new BelowOrAboveWidgetTooltipPositioner(widget.getRectangle());
        graphics.setTooltipForNextFrame(font, lines, positioner, mouseX, mouseY, true);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
