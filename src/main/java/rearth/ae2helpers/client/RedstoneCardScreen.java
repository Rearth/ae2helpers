package rearth.ae2helpers.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.network.UpdateRedstoneCardPacket;
import rearth.ae2helpers.util.RedstoneCardConfig;
import rearth.ae2helpers.util.RedstoneMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RedstoneCardScreen extends Screen {

    private static final List<Integer> PULSE_LENGTHS = List.of(1, 2, 4, 10, 20, 40);

    private final ItemStack stack;
    private RedstoneCardConfig currentConfig;

    public RedstoneCardScreen(ItemStack stack) {
        super(Component.translatable("ae2helpers.redstonecard.screen.title"));
        this.stack = stack;
        this.currentConfig = stack.getOrDefault(ae2helpers.REDSTONE_CARD_CONFIG.get(), RedstoneCardConfig.DEFAULT);
    }

    @Override
    protected void init() {
        super.init();

        var centerX = this.width / 2;
        var startY = this.height / 2 - 55;

        var modeTooltip = Tooltip.create(Component.translatable("ae2helpers.redstonecard.mode.tooltip"));

        var modeButton = CycleButton.<RedstoneMode>builder(this::getModeName)
                           .withValues(RedstoneMode.values())
                           .withTooltip(val -> modeTooltip)
                           .withInitialValue(currentConfig.mode())
                           .create(centerX - 80, startY, 200, 20, Component.translatable("ae2helpers.redstonecard.mode"),
                             (btn, val) -> updateConfig(currentConfig.strongSignal(), val, currentConfig.side(), currentConfig.pulseLength(), currentConfig.signalStrength()));
        this.addRenderableWidget(modeButton);

        var pulseTooltip = Tooltip.create(Component.translatable("ae2helpers.redstonecard.pulse.tooltip"));

        var pulseButton = CycleButton.<Integer>builder(v -> Component.translatable("ae2helpers.redstonecard.pulse.value", v))
                            .withValues(PULSE_LENGTHS)
                            .withTooltip(val -> pulseTooltip)
                            .withInitialValue(PULSE_LENGTHS.contains(currentConfig.pulseLength()) ? currentConfig.pulseLength() : RedstoneCardConfig.DEFAULT_PULSE_LENGTH)
                            .create(centerX - 80, startY + 25, 200, 20, Component.translatable("ae2helpers.redstonecard.pulse"),
                              (btn, val) -> updateConfig(currentConfig.strongSignal(), currentConfig.mode(), currentConfig.side(), val, currentConfig.signalStrength()));
        this.addRenderableWidget(pulseButton);

        var strongTooltip = Tooltip.create(Component.translatable("ae2helpers.redstonecard.strong.tooltip"));

        var strongBox = Checkbox.builder(Component.translatable("ae2helpers.redstonecard.strong"), font)
                          .pos(centerX - 80, startY + 53)
                          .selected(currentConfig.strongSignal())
                          .tooltip(strongTooltip)
                          .onValueChange((box, val) -> updateConfig(val, currentConfig.mode(), currentConfig.side(), currentConfig.pulseLength(), currentConfig.signalStrength()))
                          .build();
        this.addRenderableWidget(strongBox);

        var sideTooltip = Tooltip.create(Component.translatable("ae2helpers.redstonecard.side.tooltip"));

        var options = new ArrayList<Optional<Direction>>();
        options.add(Optional.empty());
        options.addAll(Arrays.stream(Direction.values()).map(Optional::of).toList());

        var sideButton = CycleButton.<Optional<Direction>>builder(opt -> getSideName(opt.orElse(null)))
                           .withValues(options)
                           .withTooltip(val -> sideTooltip)
                           .withInitialValue(Optional.ofNullable(currentConfig.side()))
                           .create(centerX - 80, startY + 78, 200, 20, Component.translatable("ae2helpers.redstonecard.side"),
                             (btn, val) -> updateConfig(currentConfig.strongSignal(), currentConfig.mode(), val.orElse(null), currentConfig.pulseLength(), currentConfig.signalStrength()));
        this.addRenderableWidget(sideButton);

        var strengthTooltip = Tooltip.create(Component.translatable("ae2helpers.redstonecard.strength.tooltip"));
        var strengthSlider = new StrengthSlider(centerX - 80, startY + 103, 200, 20, currentConfig.signalStrength());
        strengthSlider.setTooltip(strengthTooltip);
        this.addRenderableWidget(strengthSlider);
    }

    private class StrengthSlider extends AbstractSliderButton {
        StrengthSlider(int x, int y, int width, int height, int strength) {
            super(x, y, width, height, Component.empty(), (Math.max(1, Math.min(15, strength)) - 1) / 14.0);
            updateMessage();
        }

        private int strength() {
            return 1 + (int) Math.round(this.value * 14);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("ae2helpers.redstonecard.strength.value", strength()));
        }

        @Override
        protected void applyValue() {
            updateConfig(currentConfig.strongSignal(), currentConfig.mode(), currentConfig.side(), currentConfig.pulseLength(), strength());
        }
    }

    private Component getModeName(RedstoneMode mode) {
        return Component.translatable("ae2helpers.redstonecard.mode." + mode.getSerializedName());
    }

    private Component getSideName(Direction dir) {
        if (dir == null) return Component.translatable("ae2helpers.redstonecard.side.all");
        return Component.literal(dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1));
    }

    private void updateConfig(boolean strong, RedstoneMode mode, Direction side, int pulseLength, int signalStrength) {
        this.currentConfig = new RedstoneCardConfig(strong, mode, side, pulseLength, signalStrength);
        PacketDistributor.sendToServer(new UpdateRedstoneCardPacket(currentConfig));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
