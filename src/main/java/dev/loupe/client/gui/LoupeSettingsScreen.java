package dev.loupe.client.gui;

import com.mojang.serialization.Codec;
import dev.loupe.client.LoupeClient;
import dev.loupe.config.ZoomConfig;
import dev.loupe.zoom.ZoomMath;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.function.DoubleFunction;

public final class LoupeSettingsScreen extends OptionsSubScreen {
    private static final int WIDE_BUTTON = 310;

    public LoupeSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("loupe.options.title"));
    }

    @Override
    protected void addOptions() {
        ZoomConfig config = LoupeClient.config();

        list.addBig(slider(
                "loupe.options.amount",
                "loupe.options.amount.tooltip",
                ZoomMath.MIN_AMOUNT,
                ZoomMath.MAX_AMOUNT,
                0.25,
                config.zoomAmount(),
                value -> String.format(Locale.ROOT, "%.2fx", value),
                config::setZoomAmount));

        list.addBig(slider(
                "loupe.options.speed",
                "loupe.options.speed.tooltip",
                ZoomMath.MIN_SPEED,
                ZoomMath.MAX_SPEED,
                0.5,
                config.zoomSpeed(),
                value -> String.format(Locale.ROOT, "%.1f", value),
                config::setZoomSpeed));

        list.addBig(slider(
                "loupe.options.look",
                "loupe.options.look.tooltip",
                ZoomMath.MIN_LOOK_SMOOTHING,
                ZoomMath.MAX_LOOK_SMOOTHING,
                0.05,
                config.lookSmoothing(),
                value -> value <= 0.0
                        ? Component.translatable("options.off").getString()
                        : String.format(Locale.ROOT, "%.0f%%", value * 100.0),
                config::setLookSmoothing));

        list.addBig(OptionInstance.createBoolean(
                "loupe.options.scroll",
                OptionInstance.cachedConstantTooltip(Component.translatable("loupe.options.scroll.tooltip")),
                config.scrollToZoom(),
                config::setScrollToZoom));

        KeyMapping key = LoupeClient.zoomKey();
        Component keyLabel = key == null
                ? Component.translatable("loupe.options.key.unavailable")
                : Component.translatable("loupe.options.key", key.getTranslatedKeyMessage());
        Button keyButton = Button.builder(
                        keyLabel,
                        button -> minecraft.setScreenAndShow(new KeyBindsScreen(this, options)))
                .width(WIDE_BUTTON)
                .build();
        keyButton.active = key != null;
        list.addBig(keyButton);

        list.addBig(Button.builder(
                        Component.translatable("loupe.options.reset"),
                        button -> {
                            config.resetToDefaults();
                            rebuildWidgets();
                        })
                .width(WIDE_BUTTON)
                .build());
    }

    @Override
    public void removed() {
        super.removed();
        LoupeClient.saveConfig();
    }

    private static OptionInstance<Double> slider(
            String captionKey,
            String tooltipKey,
            double min,
            double max,
            double step,
            double initial,
            DoubleFunction<String> format,
            OptionInstance.ValueUpdateListener<Double> onChange) {

        SteppedRange range = new SteppedRange(min, max, step);
        return new OptionInstance<>(
                captionKey,
                OptionInstance.cachedConstantTooltip(Component.translatable(tooltipKey)),
                (caption, value) -> Component.translatable(
                        "options.generic_value", caption, Component.literal(format.apply(value))),
                range,
                range.snap(initial),
                onChange);
    }

    private record SteppedRange(double min, double max, double step)
            implements OptionInstance.SliderableValueSet<Double> {

        double snap(double value) {
            double clamped = Math.max(min, Math.min(max, value));
            double snapped = min + Math.round((clamped - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        @Override
        public double toSliderValue(Double value) {
            return (snap(value) - min) / (max - min);
        }

        @Override
        public Double fromSliderValue(double slider) {
            return snap(min + Math.max(0.0, Math.min(1.0, slider)) * (max - min));
        }

        @Override
        public Optional<Double> validateValue(Double value) {
            return value != null && value >= min && value <= max ? Optional.of(value) : Optional.empty();
        }

        @Override
        public Codec<Double> codec() {
            return Codec.doubleRange(min, max);
        }
    }
}
