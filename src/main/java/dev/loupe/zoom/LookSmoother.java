package dev.loupe.zoom;

public final class LookSmoother {
    private static final double FIRST_DT = 1.0 / 60.0;
    private static final double MAX_GAP = 0.25;
    private static final double DEAD_ZONE = 1.0E-6;

    private double x1;
    private double y1;
    private double x2;
    private double y2;
    private double outX;
    private double outY;
    private long lastNanos;
    private boolean started;

    public void advance(double dx, double dy, double magnification, double smoothing, long nowNanos) {
        double dt = FIRST_DT;
        if (started) {
            dt = (nowNanos - lastNanos) / 1_000_000_000.0;
            if (dt > MAX_GAP) {
                clear();
                dt = FIRST_DT;
            }
        }
        lastNanos = nowNanos;
        started = true;

        if (!Double.isFinite(dx)) {
            dx = 0.0;
        }
        if (!Double.isFinite(dy)) {
            dy = 0.0;
        }

        double lag = ZoomMath.lookLag(magnification, smoothing);
        if (lag <= 0.0) {
            outX = dx + x1 + x2;
            outY = dy + y1 + y2;
            clear();
            return;
        }

        double k = 1.0 - Math.exp(-ZoomMath.frameTime(dt) / lag);

        x1 += dx;
        y1 += dy;
        double passX = x1 * k;
        double passY = y1 * k;
        x1 -= passX;
        y1 -= passY;

        x2 += passX;
        y2 += passY;
        outX = x2 * k;
        outY = y2 * k;
        x2 -= outX;
        y2 -= outY;

        x1 = settle(x1);
        y1 = settle(y1);
        x2 = settle(x2);
        y2 = settle(y2);
    }

    public double x() {
        return outX;
    }

    public double y() {
        return outY;
    }

    public void reset() {
        clear();
        outX = 0.0;
        outY = 0.0;
        lastNanos = 0L;
        started = false;
    }

    private void clear() {
        x1 = 0.0;
        y1 = 0.0;
        x2 = 0.0;
        y2 = 0.0;
    }

    private static double settle(double value) {
        return Math.abs(value) < DEAD_ZONE ? 0.0 : value;
    }
}
