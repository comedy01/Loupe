package dev.loupe.client.gui;

import dev.loupe.client.GameScreens;
import dev.loupe.client.LoupeClient;
import dev.loupe.config.ZoomConfig;
import dev.loupe.zoom.ZoomMath;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public final class LoupeSettingsScreen extends OptionsSubScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public LoupeSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("loupe.options.title"));
    }

    @Override
    protected void addOptions() {
        ZoomConfig config = LoupeClient.config();

        AbstractWidget amount = new StepSlider(
                "loupe.options.amount",
                "loupe.options.amount.tooltip",
                ZoomMath.MIN_AMOUNT,
                ZoomMath.MAX_AMOUNT,
                0.25,
                config.zoomAmount(),
                value -> String.format(Locale.ROOT, "%.2fx", value),
                config::setZoomAmount);

        AbstractWidget speed = new StepSlider(
                "loupe.options.speed",
                "loupe.options.speed.tooltip",
                ZoomMath.MIN_SPEED,
                ZoomMath.MAX_SPEED,
                0.5,
                config.zoomSpeed(),
                value -> String.format(Locale.ROOT, "%.1f", value),
                config::setZoomSpeed);

        AbstractWidget look = new StepSlider(
                "loupe.options.look",
                "loupe.options.look.tooltip",
                ZoomMath.MIN_LOOK_SMOOTHING,
                ZoomMath.MAX_LOOK_SMOOTHING,
                0.05,
                config.lookSmoothing(),
                value -> value <= 0.0
                        ? Component.translatable("options.off").getString()
                        : String.format(Locale.ROOT, "%.0f%%", value * 100.0),
                config::setLookSmoothing);

        list.addSmall(List.of(amount, speed));
        list.addSmall(List.of(look, scrollButton(config)));
        list.addSmall(List.of(keyButton(), resetButton(config)));
    }

    @Override
    public void removed() {
        super.removed();
        LoupeClient.saveConfig();
    }

    private AbstractWidget scrollButton(ZoomConfig config) {
        return Button.builder(scrollLabel(config), button -> {
                    config.setScrollToZoom(!config.scrollToZoom());
                    button.setMessage(scrollLabel(config));
                })
                .width(WIDTH)
                .tooltip(Tooltip.create(Component.translatable("loupe.options.scroll.tooltip")))
                .build();
    }

    private static Component scrollLabel(ZoomConfig config) {
        Component state = Component.translatable(config.scrollToZoom() ? "options.on" : "options.off");
        return Component.translatable("options.generic_value", Component.translatable("loupe.options.scroll"), state);
    }

    private AbstractWidget keyButton() {
        KeyMapping key = LoupeClient.zoomKey();
        Component label = key == null
                ? Component.translatable("loupe.options.key.unavailable")
                : Component.translatable("loupe.options.key", key.getTranslatedKeyMessage());
        Button button = Button.builder(label, pressed -> GameScreens.open(minecraft, new KeyBindsScreen(this, options)))
                .width(WIDTH)
                .build();
        button.active = key != null;
        return button;
    }

    private AbstractWidget resetButton(ZoomConfig config) {
        return Button.builder(Component.translatable("loupe.options.reset"), button -> {
                    config.resetToDefaults();
                    GameScreens.open(minecraft, new LoupeSettingsScreen(lastScreen, options));
                })
                .width(WIDTH)
                .build();
    }

    private static final class StepSlider extends AbstractSliderButton {
        private final String captionKey;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<String> format;
        private final DoubleConsumer onChange;

        StepSlider(
                String captionKey,
                String tooltipKey,
                double min,
                double max,
                double step,
                double initial,
                DoubleFunction<String> format,
                DoubleConsumer onChange) {

            super(0, 0, WIDTH, HEIGHT, Component.empty(), 0.0);
            this.captionKey = captionKey;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.onChange = onChange;
            this.value = (snap(initial) - min) / (max - min);
            setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
            updateMessage();
        }

        private double snap(double raw) {
            double clamped = Math.max(min, Math.min(max, raw));
            double snapped = min + Math.round((clamped - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        private double current() {
            return snap(min + value * (max - min));
        }

        @Override
        protected void updateMessage() {
            Component shown = Component.literal(format.apply(current()));
            setMessage(Component.translatable("options.generic_value", Component.translatable(captionKey), shown));
        }

        @Override
        protected void applyValue() {
            double snapped = current();
            value = (snapped - min) / (max - min);
            onChange.accept(snapped);
        }
    }
}
