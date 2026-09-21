package dev.loupe.zoom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.loupe.config.ZoomConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZoomControllerTest {
    private static final long FRAME_NANOS = 16_666_667L;
    private static final float VANILLA_FOV = 70.0F;

    private ZoomController controller;
    private ZoomConfig config;
    private long clock;

    @BeforeEach
    void setUp() {
        controller = new ZoomController();
        config = new ZoomConfig();
        clock = 1_000_000_000L;
    }

    private float frame(boolean held) {
        return frame(VANILLA_FOV, held);
    }

    private float frame(float vanilla, boolean held) {
        clock += FRAME_NANOS;
        return controller.modifyFov(vanilla, held, config, clock);
    }

    private float run(int frames, boolean held) {
        float fov = VANILLA_FOV;
        for (int i = 0; i < frames; i++) {
            fov = frame(held);
        }
        return fov;
    }

    @Test
    void notHeldKeepsVanillaFov() {
        assertEquals(70.5F, frame(70.5F, false));
        assertEquals(70.5F, frame(70.5F, false));
        assertEquals(0.0, controller.progress());
        assertEquals(0.0, controller.currentAmount());
    }

    @Test
    void holdAndRelease() {
        float zoomed = run(120, true);
        assertEquals(VANILLA_FOV / 4.0F, zoomed, 0.01F);
        assertEquals(1.0, controller.progress());

        float restored = run(120, false);
        assertEquals(VANILLA_FOV, restored, 0.0F);
        assertEquals(0.0, controller.progress());
    }

    @Test
    void easesIn() {
        float first = frame(true);
        assertTrue(first < VANILLA_FOV && first > VANILLA_FOV / 4.0F, "first frame is partway: " + first);
        float previous = first;
        for (int i = 0; i < 40; i++) {
            float next = frame(true);
            assertTrue(next <= previous, "FOV only shrinks while zooming in");
            previous = next;
        }
    }

    @Test
    void usesConfig() {
        config.setZoomAmount(2.0);
        assertEquals(VANILLA_FOV / 2.0F, run(120, true), 0.01F);

        setUp();
        config.setZoomSpeed(ZoomMath.MIN_SPEED);
        float slow = run(6, true);
        setUp();
        config.setZoomSpeed(ZoomMath.MAX_SPEED);
        float fast = run(6, true);
        assertTrue(fast < slow, "a higher speed setting zooms in sooner");
    }

    @Test
    void configChangeApplies() {
        run(120, true);
        run(120, false);
        config.setZoomAmount(8.0);
        assertEquals(VANILLA_FOV / 8.0F, run(120, true), 0.01F);
    }

    @Test
    void scrollChangesMagnification() {
        run(120, true);
        assertTrue(controller.onScroll(1.0, true, config));
        assertTrue(controller.onScroll(1.0, true, config));
        float zoomedIn = run(120, true);
        assertTrue(zoomedIn < VANILLA_FOV / 4.0F, "scroll up zooms in further");
        assertEquals(VANILLA_FOV / (4.0F * (float) Math.pow(ZoomMath.SCROLL_STEP, 2.0)), zoomedIn, 0.01F);

        for (int i = 0; i < 6; i++) {
            assertTrue(controller.onScroll(-1.0, true, config));
        }
        float zoomedOut = run(120, true);
        assertTrue(zoomedOut > VANILLA_FOV / 4.0F, "scroll down zooms out");
    }

    @Test
    void scrollEases() {
        run(120, true);
        controller.onScroll(5.0, true, config);
        float next = frame(true);
        assertTrue(next < VANILLA_FOV / 4.0F);
        assertTrue(next > VANILLA_FOV / (4.0F * (float) Math.pow(ZoomMath.SCROLL_STEP, 5.0)) + 0.5F);
    }

    @Test
    void scrollLimits() {
        run(120, true);
        for (int i = 0; i < 100; i++) {
            controller.onScroll(5.0, true, config);
        }
        assertEquals(ZoomMath.MAX_SCROLL_AMOUNT, controller.targetAmount(), 1.0e-9);
        assertEquals(5.0F, run(300, true), 1.0e-4F);

        for (int i = 0; i < 100; i++) {
            controller.onScroll(-5.0, true, config);
        }
        assertEquals(ZoomMath.MIN_SCROLL_AMOUNT, controller.targetAmount(), 1.0e-9);
        assertEquals(VANILLA_FOV / (float) ZoomMath.MIN_SCROLL_AMOUNT, run(300, true), 0.01F);
    }

    @Test
    void wheelIgnoredWhenNotHeld() {
        assertFalse(controller.onScroll(1.0, false, config));
        assertFalse(controller.onScroll(-1.0, false, config));
        assertEquals(0.0, controller.targetAmount());
        assertEquals(VANILLA_FOV, run(30, false), 0.0F);
    }

    @Test
    void wheelIgnoredWhenScrollOff() {
        config.setScrollToZoom(false);
        run(120, true);
        assertFalse(controller.onScroll(1.0, true, config), "the wheel must reach the hotbar");
        assertEquals(VANILLA_FOV / 4.0F, run(60, true), 0.01F);
    }

    @Test
    void invalidScroll() {
        run(30, true);
        double before = controller.targetAmount();
        assertFalse(controller.onScroll(0.0, true, config));
        assertFalse(controller.onScroll(Double.NaN, true, config));
        assertFalse(controller.onScroll(Double.POSITIVE_INFINITY, true, config));
        assertFalse(controller.onScroll(Double.NEGATIVE_INFINITY, true, config));
        assertEquals(before, controller.targetAmount());
    }

    @Test
    void hugeScrollBurst() {
        run(120, true);
        controller.onScroll(10_000.0, true, config);
        assertTrue(controller.targetAmount() < 4.0 * Math.pow(ZoomMath.SCROLL_STEP, 5.1));
    }

    @Test
    void scrollBeforeFirstFrame() {
        assertTrue(controller.onScroll(1.0, true, config));
        assertEquals(4.0 * ZoomMath.SCROLL_STEP, controller.targetAmount(), 1.0e-9);
        assertEquals(VANILLA_FOV / (4.0F * (float) ZoomMath.SCROLL_STEP), run(200, true), 0.01F);
    }

    @Test
    void resetsAfterZoomOut() {
        run(120, true);
        for (int i = 0; i < 5; i++) {
            controller.onScroll(2.0, true, config);
        }
        run(120, true);

        run(3, false);
        assertTrue(controller.currentAmount() > 4.0);
        assertTrue(controller.progress() > 0.0);

        run(240, false);
        assertEquals(0.0, controller.progress());
        assertEquals(0.0, controller.targetAmount());
        assertEquals(0.0, controller.currentAmount());
        assertEquals(VANILLA_FOV / 4.0F, run(120, true), 0.01F);
    }

    @Test
    void repressWhileEasingOut() {
        run(120, true);
        for (int i = 0; i < 4; i++) {
            controller.onScroll(1.0, true, config);
        }
        run(120, true);
        double scrolled = controller.currentAmount();
        assertTrue(scrolled > 6.0);

        run(5, false);
        assertTrue(controller.progress() > 0.0);
        run(1, true);
        assertEquals(scrolled, controller.targetAmount(), 1.0e-9);
        assertEquals(VANILLA_FOV / (float) scrolled, run(200, true), 0.01F);
    }

    @Test
    void scrollOffMidZoom() {
        run(120, true);
        controller.onScroll(3.0, true, config);
        run(120, true);
        config.setScrollToZoom(false);
        assertEquals(VANILLA_FOV / 4.0F, run(200, true), 0.01F);
    }

    @Test
    void reset() {
        run(120, true);
        controller.onScroll(2.0, true, config);
        controller.reset();
        assertEquals(0.0, controller.progress());
        assertEquals(0.0, controller.targetAmount());
        assertEquals(0.0, controller.currentAmount());
        assertEquals(VANILLA_FOV, controller.modifyFov(VANILLA_FOV, false, config, 5L), 0.0F);
    }

    @Test
    void fractionalFov() {
        float zoomed = run(120, true);
        assertEquals(VANILLA_FOV / 4.0F, zoomed, 0.01F);
        float fractional = frame(70.5F, true);
        assertEquals(70.5F / 4.0F, fractional, 0.01F);
    }

    @Test
    void hitchDoesNotJump() {
        run(2, true);
        double before = controller.progress();
        clock += 10_000_000_000L;
        controller.modifyFov(VANILLA_FOV, true, config, clock);
        double after = controller.progress();
        assertTrue(after - before < 0.75, "progress " + before + " -> " + after);
    }

    @Test
    void frameRateIndependent() {
        config.setZoomSpeed(4.0);
        double expectedProgress = 1.0 - Math.exp(-config.zoomSpeed() * 0.5);
        for (int fps : new int[] {30, 60, 144, 240}) {
            ZoomController c = new ZoomController();
            long t = 42L;
            long step = 1_000_000_000L / fps;
            int frames = fps / 2;
            // the first frame uses a nominal 1/60 s step, so prime the clock first
            c.modifyFov(VANILLA_FOV, false, config, t);
            for (int i = 0; i < frames; i++) {
                t += step;
                c.modifyFov(VANILLA_FOV, true, config, t);
            }
            assertEquals(expectedProgress, c.progress(), 0.005, fps + " fps");
        }
    }
}
