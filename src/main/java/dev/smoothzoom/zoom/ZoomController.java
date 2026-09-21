package dev.smoothzoom.zoom;

import dev.smoothzoom.config.ZoomConfig;

public final class ZoomController {
    private static final double FIRST_DT = 1.0 / 60.0;

    // 0 = zoomed out, 1 = zoomed in
    private double progress;
    private long lastNanos;
    private boolean started;
    // set by the wheel while the key is held, 0 means use the configured amount
    private double targetAmount;
    private double currentAmount;
    private double magnification = 1.0;

    public float modifyFov(float vanillaFov, boolean held, ZoomConfig config, long nowNanos) {
        double dt = started ? (nowNanos - lastNanos) / 1_000_000_000.0 : FIRST_DT;
        lastNanos = nowNanos;
        started = true;

        double configured = ZoomMath.clampAmount(config.zoomAmount());
        double speed = config.zoomSpeed();

        if (held) {
            if (!config.scrollToZoom() || targetAmount <= 0.0) {
                targetAmount = configured;
            }
            if (currentAmount <= 0.0) {
                currentAmount = targetAmount;
            }
        }

        progress = ZoomMath.advanceProgress(progress, held, speed, dt);

        if (!held && progress <= 0.0) {
            // fully out, the next press starts from the configured amount again
            targetAmount = 0.0;
            currentAmount = 0.0;
        } else if (currentAmount > 0.0) {
            currentAmount = ZoomMath.advanceAmount(currentAmount, targetAmount, speed, dt);
        }

        float zoomed = ZoomMath.applyToFov(vanillaFov, currentAmount > 0.0 ? currentAmount : configured, progress);
        boolean valid = Float.isFinite(vanillaFov) && Float.isFinite(zoomed) && vanillaFov > 0.0F && zoomed > 0.0F;
        magnification = valid ? Math.max(1.0, (double) vanillaFov / zoomed) : 1.0;
        return zoomed;
    }

    public boolean onScroll(double vertical, boolean held, ZoomConfig config) {
        if (!config.scrollToZoom() || !held || vertical == 0.0 || !Double.isFinite(vertical)) {
            return false;
        }
        double base = targetAmount > 0.0 ? targetAmount : ZoomMath.clampAmount(config.zoomAmount());
        targetAmount = ZoomMath.scrollAmount(base, vertical);
        return true;
    }

    public void reset() {
        progress = 0.0;
        lastNanos = 0L;
        started = false;
        targetAmount = 0.0;
        currentAmount = 0.0;
        magnification = 1.0;
    }

    public double progress() {
        return progress;
    }

    public double targetAmount() {
        return targetAmount;
    }

    public double currentAmount() {
        return currentAmount;
    }

    public double magnification() {
        return magnification;
    }
}
