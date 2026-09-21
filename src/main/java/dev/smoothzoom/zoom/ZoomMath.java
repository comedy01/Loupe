package dev.smoothzoom.zoom;

public final class ZoomMath {
    public static final double MIN_AMOUNT = 1.25;
    public static final double MAX_AMOUNT = 10.0;
    public static final double DEFAULT_AMOUNT = 4.0;

    public static final double MIN_SCROLL_AMOUNT = 1.1;
    public static final double MAX_SCROLL_AMOUNT = 30.0;
    public static final double SCROLL_STEP = 1.15;
    private static final double MAX_SCROLL_NOTCHES = 5.0;

    public static final double MIN_SPEED = 1.0;
    public static final double MAX_SPEED = 20.0;
    public static final double DEFAULT_SPEED = 8.0;

    public static final double MIN_LOOK_SMOOTHING = 0.0;
    public static final double MAX_LOOK_SMOOTHING = 1.0;
    public static final double DEFAULT_LOOK_SMOOTHING = 0.6;
    private static final double LAG_PER_LOG_STEP = 0.08;
    private static final double MAX_LOOK_LAG = 0.2;

    private static final double MIN_FOV = 5.0;
    private static final double MAX_DT = 0.1;
    private static final double SNAP = 0.0005;

    private ZoomMath() {
    }

    public static boolean canZoom(boolean inWorld, boolean screenOpen, boolean keyBound) {
        return inWorld && !screenOpen && keyBound;
    }

    public static double clampAmount(double value) {
        return clamp(value, MIN_AMOUNT, MAX_AMOUNT, DEFAULT_AMOUNT);
    }

    public static double clampScrollAmount(double value) {
        return clamp(value, MIN_SCROLL_AMOUNT, MAX_SCROLL_AMOUNT, DEFAULT_AMOUNT);
    }

    public static double clampSpeed(double value) {
        return clamp(value, MIN_SPEED, MAX_SPEED, DEFAULT_SPEED);
    }

    public static double clampLookSmoothing(double value) {
        return clamp(value, MIN_LOOK_SMOOTHING, MAX_LOOK_SMOOTHING, DEFAULT_LOOK_SMOOTHING);
    }

    public static double scrollAmount(double current, double notches) {
        double base = clampScrollAmount(current);
        if (!Double.isFinite(notches)) {
            return base;
        }
        double bounded = Math.max(-MAX_SCROLL_NOTCHES, Math.min(MAX_SCROLL_NOTCHES, notches));
        return clampScrollAmount(base * Math.pow(SCROLL_STEP, bounded));
    }

    public static double advanceAmount(double current, double target, double speed, double deltaSeconds) {
        double from = clampScrollAmount(current);
        double to = clampScrollAmount(target);
        double dt = frameTime(deltaSeconds);
        if (dt <= 0.0) {
            return from;
        }
        double alpha = 1.0 - Math.exp(-clampSpeed(speed) * dt);
        double logFrom = Math.log(from);
        double logTo = Math.log(to);
        double next = logFrom + (logTo - logFrom) * alpha;
        if (Math.abs(logTo - next) <= SNAP) {
            return to;
        }
        return clampScrollAmount(Math.exp(next));
    }

    public static double advanceProgress(double current, boolean held, double speed, double deltaSeconds) {
        double progress = clamp01(current);
        double target = held ? 1.0 : 0.0;
        double dt = frameTime(deltaSeconds);
        if (dt <= 0.0) {
            return progress;
        }
        double alpha = 1.0 - Math.exp(-clampSpeed(speed) * dt);
        double next = progress + (target - progress) * alpha;
        if (Math.abs(target - next) <= SNAP) {
            return target;
        }
        return clamp01(next);
    }

    public static float applyToFov(float vanillaFov, double amount, double progress) {
        if (!Float.isFinite(vanillaFov) || vanillaFov <= 0.0F) {
            return vanillaFov;
        }
        double base = vanillaFov;
        double target = Math.min(base, Math.max(MIN_FOV, base / clampScrollAmount(amount)));
        double p = clamp01(progress);
        double eased = p * p * (3.0 - 2.0 * p);
        return (float) (base + (target - base) * eased);
    }

    public static int applyToFov(int vanillaFov, double amount, double progress) {
        return Math.round(applyToFov((float) vanillaFov, amount, progress));
    }

    public static double lookLag(double magnification, double smoothing) {
        double strength = clampLookSmoothing(smoothing);
        if (strength <= 0.0 || !Double.isFinite(magnification) || magnification <= 1.0) {
            return 0.0;
        }
        return Math.min(MAX_LOOK_LAG * strength, LAG_PER_LOG_STEP * strength * Math.log(magnification));
    }

    static double frameTime(double seconds) {
        return Double.isFinite(seconds) ? Math.max(0.0, Math.min(MAX_DT, seconds)) : 0.0;
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double clamp(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
