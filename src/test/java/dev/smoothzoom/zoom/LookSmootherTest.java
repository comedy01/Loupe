package dev.smoothzoom.zoom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.smoothzoom.config.ZoomConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LookSmootherTest {
    private static final long FRAME_60 = 1_000_000_000L / 60L;
    private static final double ZOOM = 4.0;
    private static final double SMOOTHING = ZoomMath.DEFAULT_LOOK_SMOOTHING;

    private LookSmoother smoother;
    private long now;

    @BeforeEach
    void setUp() {
        smoother = new LookSmoother();
        now = 5_000_000_000L;
    }

    private double frame(double dx, double dy, double magnification, double smoothing, long step) {
        now += step;
        smoother.advance(dx, dy, magnification, smoothing, now);
        return smoother.x();
    }

    private double drive(double dxPerSecond, double seconds, long step) {
        double total = 0.0;
        double stepSeconds = step / 1_000_000_000.0;
        int frames = (int) Math.round(seconds / stepSeconds);
        for (int i = 0; i < frames; i++) {
            total += frame(dxPerSecond * stepSeconds, 0.0, ZOOM, SMOOTHING, step);
        }
        return total;
    }

    @Test
    void passesThroughWhenNotZoomed() {
        assertEquals(3.5, frame(3.5, -2.0, 1.0, SMOOTHING, FRAME_60), 1e-12);
        assertEquals(-2.0, smoother.y(), 1e-12);
        assertEquals(0.0, frame(0.0, 0.0, 1.0, SMOOTHING, FRAME_60), 1e-12);
    }

    @Test
    void passesThroughWhenOff() {
        assertEquals(3.5, frame(3.5, 0.0, 10.0, 0.0, FRAME_60), 1e-12);
    }

    @Test
    void doesNotJumpWhenZoomed() {
        double first = frame(10.0, 0.0, ZOOM, SMOOTHING, FRAME_60);
        assertTrue(first > 0.0, "the view should start to move");
        assertTrue(first < 1.0, "the first frame must be a small fraction of the mouse movement: " + first);
    }

    @Test
    void rampsUp() {
        double previous = 0.0;
        double previousStep = 0.0;
        for (int i = 0; i < 6; i++) {
            double out = frame(1.0, 0.0, ZOOM, SMOOTHING, FRAME_60);
            assertTrue(out > previous, "speed should keep rising, frame " + i + ": " + out);
            if (i > 0 && i < 3) {
                assertTrue(out - previous > previousStep - 1e-12, "acceleration should build, frame " + i);
            }
            previousStep = out - previous;
            previous = out;
        }
        for (int i = 0; i < 120; i++) {
            previous = frame(1.0, 0.0, ZOOM, SMOOTHING, FRAME_60);
        }
        assertEquals(1.0, previous, 1e-3, "a steady mouse speed is eventually followed exactly");
    }

    @Test
    void glidesToStop() {
        for (int i = 0; i < 120; i++) {
            frame(1.0, 0.0, ZOOM, SMOOTHING, FRAME_60);
        }
        double previous = smoother.x();
        int glideFrames = 0;
        while (glideFrames < 600) {
            double out = frame(0.0, 0.0, ZOOM, SMOOTHING, FRAME_60);
            assertTrue(out <= previous + 1e-12, "speed must not rise while gliding out");
            assertTrue(out >= 0.0);
            previous = out;
            if (out == 0.0) {
                break;
            }
            glideFrames++;
        }
        assertTrue(glideFrames > 10, "should take several frames to feather out, took " + glideFrames);
        assertTrue(glideFrames < 120, "should not drift for seconds, took " + glideFrames);
        assertEquals(0.0, smoother.x(), 0.0);
    }

    @Test
    void keepsTotalRotation() {
        double input = 0.0;
        double output = 0.0;
        for (int i = 0; i < 20; i++) {
            double dx = 2.0 + (i % 3);
            input += dx;
            output += frame(dx, 0.0, ZOOM, SMOOTHING, FRAME_60);
        }
        for (int i = 0; i < 600; i++) {
            output += frame(0.0, 0.0, ZOOM, SMOOTHING, FRAME_60);
        }
        assertEquals(input, output, input * 1e-4);
    }

    @Test
    void frameRateIndependent() {
        double at30 = drive(20.0, 0.3, 1_000_000_000L / 30L);
        smoother.reset();
        double at60 = drive(20.0, 0.3, FRAME_60);
        smoother.reset();
        double at240 = drive(20.0, 0.3, 1_000_000_000L / 240L);
        assertEquals(at60, at30, at60 * 0.08);
        assertEquals(at60, at240, at60 * 0.08);
    }

    @Test
    void lagGrowsWithZoom() {
        double low = ZoomMath.lookLag(2.0, SMOOTHING);
        double mid = ZoomMath.lookLag(4.0, SMOOTHING);
        double high = ZoomMath.lookLag(10.0, SMOOTHING);
        assertTrue(low > 0.0 && low < mid && mid < high, low + " " + mid + " " + high);
        assertEquals(0.0, ZoomMath.lookLag(1.0, SMOOTHING));
        assertEquals(0.0, ZoomMath.lookLag(0.5, SMOOTHING));
        assertEquals(0.0, ZoomMath.lookLag(Double.NaN, SMOOTHING));
    }

    @Test
    void lagIsCapped() {
        double capped = ZoomMath.lookLag(1000.0, 1.0);
        assertTrue(capped > 0.0 && capped <= 0.2 + 1e-12, "capped at " + capped);
        assertTrue(ZoomMath.lookLag(4.0, 1.0) > ZoomMath.lookLag(4.0, 0.3));
    }

    @Test
    void flushesWhenZoomEnds() {
        double input = 0.0;
        double output = 0.0;
        for (int i = 0; i < 10; i++) {
            input += 5.0;
            output += frame(5.0, 0.0, ZOOM, SMOOTHING, FRAME_60);
        }
        output += frame(0.0, 0.0, 1.0, SMOOTHING, FRAME_60);
        output += frame(0.0, 0.0, 1.0, SMOOTHING, FRAME_60);
        assertEquals(input, output, 1e-9);
    }

    @Test
    void dropsStaleMotion() {
        for (int i = 0; i < 10; i++) {
            frame(5.0, 0.0, ZOOM, SMOOTHING, FRAME_60);
        }
        double out = frame(0.0, 0.0, ZOOM, SMOOTHING, 2_000_000_000L);
        assertEquals(0.0, out, 1e-12);
        assertEquals(0.0, smoother.y(), 1e-12);
    }

    @Test
    void resetDropsMotion() {
        for (int i = 0; i < 10; i++) {
            frame(5.0, 0.0, ZOOM, SMOOTHING, FRAME_60);
        }
        smoother.reset();
        assertEquals(0.0, frame(0.0, 0.0, ZOOM, SMOOTHING, FRAME_60), 0.0);
    }

    @Test
    void invalidInput() {
        frame(Double.NaN, Double.POSITIVE_INFINITY, ZOOM, SMOOTHING, FRAME_60);
        assertEquals(0.0, smoother.x(), 0.0);
        assertEquals(0.0, smoother.y(), 0.0);
        assertTrue(frame(1.0, 0.0, ZOOM, SMOOTHING, FRAME_60) > 0.0);
        assertTrue(Double.isFinite(frame(1.0, 0.0, Double.NaN, Double.NaN, FRAME_60)));
    }

    @Test
    void axesAreIndependent() {
        smoother.advance(4.0, -2.0, ZOOM, SMOOTHING, now + FRAME_60);
        assertEquals(-smoother.y() * 2.0, smoother.x(), 1e-12);
        assertTrue(smoother.x() > 0.0 && smoother.y() < 0.0);
    }

    @Test
    void magnificationFollowsZoom() {
        ZoomController controller = new ZoomController();
        ZoomConfig config = new ZoomConfig();
        assertEquals(1.0, controller.magnification(), 0.0);
        long t = 1_000_000_000L;
        controller.modifyFov(70.0F, false, config, t);
        assertEquals(1.0, controller.magnification(), 1e-9);
        double previous = 1.0;
        for (int i = 0; i < 30; i++) {
            t += FRAME_60;
            controller.modifyFov(70.0F, true, config, t);
            assertTrue(controller.magnification() >= previous, "magnification should rise while zooming in");
            previous = controller.magnification();
        }
        for (int i = 0; i < 300; i++) {
            t += FRAME_60;
            controller.modifyFov(70.0F, true, config, t);
        }
        assertEquals(config.zoomAmount(), controller.magnification(), 1e-3);
        for (int i = 0; i < 600; i++) {
            t += FRAME_60;
            controller.modifyFov(70.0F, false, config, t);
        }
        assertEquals(1.0, controller.magnification(), 1e-9);
    }
}
