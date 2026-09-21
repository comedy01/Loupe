package dev.loupe.zoom;

import dev.loupe.config.ZoomConfig;

public final class ZoomController {
    private static final double FIRST_DT = 1.0 / 60.0;

    private double progress;
    private long lastNanos;
    private boolean started;
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
