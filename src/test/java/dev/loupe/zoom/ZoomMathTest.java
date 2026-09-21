package dev.loupe.zoom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZoomMathTest {
    private static final double FRAME_60 = 1.0 / 60.0;

    @Test
    void canZoom() {
        assertTrue(ZoomMath.canZoom(true, false, true));
        assertFalse(ZoomMath.canZoom(true, true, true));
        assertFalse(ZoomMath.canZoom(false, false, true));
        assertFalse(ZoomMath.canZoom(true, false, false));
        assertFalse(ZoomMath.canZoom(false, true, false));
    }

    @Test
    void easesInAndOut() {
        double progress = 0.0;

        for (int i = 0; i < 60; i++) {
            progress = ZoomMath.advanceProgress(progress, true, ZoomMath.DEFAULT_SPEED, FRAME_60);
        }

        assertTrue(progress > 0.99);

        for (int i = 0; i < 60; i++) {
            progress = ZoomMath.advanceProgress(progress, false, ZoomMath.DEFAULT_SPEED, FRAME_60);
        }

        assertTrue(progress < 0.01);
    }

    @Test
    void progressIsMonotonic() {
        double progress = 0.0;
        for (int i = 0; i < 30; i++) {
            double next = ZoomMath.advanceProgress(progress, true, 5.0, FRAME_60);
            assertTrue(next >= progress && next <= 1.0);
            progress = next;
        }
        for (int i = 0; i < 30; i++) {
            double next = ZoomMath.advanceProgress(progress, false, 5.0, FRAME_60);
            assertTrue(next <= progress && next >= 0.0);
            progress = next;
        }
    }

    @Test
    void progressSnapsToEnds() {
        double in = 0.0;
        for (int i = 0; i < 600; i++) {
            in = ZoomMath.advanceProgress(in, true, ZoomMath.DEFAULT_SPEED, FRAME_60);
        }
        assertEquals(1.0, in);

        double out = 1.0;
        for (int i = 0; i < 600; i++) {
            out = ZoomMath.advanceProgress(out, false, ZoomMath.DEFAULT_SPEED, FRAME_60);
        }
        assertEquals(0.0, out);
    }

    @Test
    void progressFrameRateIndependent() {
        double speed = 4.0;
        double expected = 1.0 - Math.exp(-speed * 0.5);
        for (int fps : new int[] {20, 30, 60, 144, 240}) {
            double progress = 0.0;
            int frames = fps / 2;
            for (int i = 0; i < frames; i++) {
                progress = ZoomMath.advanceProgress(progress, true, speed, 1.0 / fps);
            }
            assertEquals(expected, progress, 1.0e-9, fps + " fps");
        }
    }

    @Test
    void longFramesAreCapped() {
        double capped = ZoomMath.advanceProgress(0.0, true, ZoomMath.DEFAULT_SPEED, 5.0);
        double tenth = ZoomMath.advanceProgress(0.0, true, ZoomMath.DEFAULT_SPEED, 0.1);
        assertEquals(tenth, capped, 1.0e-12);
        assertTrue(capped < 1.0);

        assertEquals(0.3, ZoomMath.advanceProgress(0.3, true, 8.0, 0.0));
        assertEquals(0.3, ZoomMath.advanceProgress(0.3, true, 8.0, -1.0));
        assertEquals(0.3, ZoomMath.advanceProgress(0.3, true, 8.0, Double.NaN));
        assertEquals(0.3, ZoomMath.advanceProgress(0.3, true, 8.0, Double.POSITIVE_INFINITY));
    }

    @Test
    void higherSpeedIsFaster() {
        double slow = ZoomMath.advanceProgress(0.0, true, ZoomMath.MIN_SPEED, FRAME_60);
        double fast = ZoomMath.advanceProgress(0.0, true, ZoomMath.MAX_SPEED, FRAME_60);
        assertTrue(fast > slow);
    }

    @Test
    void clamps() {
        assertEquals(ZoomMath.MAX_SPEED, ZoomMath.clampSpeed(1000.0));
        assertEquals(ZoomMath.MIN_SPEED, ZoomMath.clampSpeed(-5.0));
        assertEquals(ZoomMath.DEFAULT_SPEED, ZoomMath.clampSpeed(Double.NaN));
        assertEquals(ZoomMath.DEFAULT_SPEED, ZoomMath.clampSpeed(Double.POSITIVE_INFINITY));
        assertEquals(12.5, ZoomMath.clampSpeed(12.5));

        assertEquals(ZoomMath.MAX_AMOUNT, ZoomMath.clampAmount(100.0));
        assertEquals(ZoomMath.MIN_AMOUNT, ZoomMath.clampAmount(0.0));
        assertEquals(ZoomMath.DEFAULT_AMOUNT, ZoomMath.clampAmount(Double.NaN));
        assertEquals(2.5, ZoomMath.clampAmount(2.5));

        assertEquals(
                ZoomMath.advanceProgress(0.0, true, ZoomMath.MAX_SPEED, FRAME_60),
                ZoomMath.advanceProgress(0.0, true, 9999.0, FRAME_60));
    }

    @Test
    void magnifiesFov() {
        assertEquals(70, ZoomMath.applyToFov(70, 4.0, 0.0));
        assertEquals(18, ZoomMath.applyToFov(70, 4.0, 1.0));

        int middle = ZoomMath.applyToFov(70, 4.0, 0.5);

        assertTrue(middle > 18 && middle < 70);
    }

    @Test
    void keepsFractionalFov() {
        float result = ZoomMath.applyToFov(70.5F, 4.0, 1.0);

        assertTrue(result > 17.6F && result < 17.7F);
        assertEquals(70.5F, ZoomMath.applyToFov(70.5F, 4.0, 0.0));
    }

    @Test
    void fovFloor() {
        assertEquals(5.0F, ZoomMath.applyToFov(70.0F, 30.0, 1.0), 1.0e-4F);
        assertEquals(5.0F, ZoomMath.applyToFov(10.0F, 30.0, 1.0), 1.0e-4F);
        assertEquals(4.0F, ZoomMath.applyToFov(4.0F, 4.0, 1.0), 1.0e-4F);
    }

    @Test
    void invalidFov() {
        assertTrue(Float.isNaN(ZoomMath.applyToFov(Float.NaN, 4.0, 1.0)));
        assertEquals(0.0F, ZoomMath.applyToFov(0.0F, 4.0, 1.0));
        assertEquals(-10.0F, ZoomMath.applyToFov(-10.0F, 4.0, 1.0));
        assertEquals(70.0F, ZoomMath.applyToFov(70.0F, 4.0, Double.NaN), 1.0e-4F);
        float badAmount = ZoomMath.applyToFov(70.0F, Double.NaN, 1.0);
        assertTrue(Float.isFinite(badAmount) && badAmount > 0.0F);
    }

    @Test
    void scrollDirection() {
        double in = ZoomMath.scrollAmount(4.0, 1.0);
        double out = ZoomMath.scrollAmount(4.0, -1.0);
        assertTrue(in > 4.0);
        assertTrue(out < 4.0);
        assertEquals(4.0 * ZoomMath.SCROLL_STEP, in, 1.0e-9);
        assertEquals(4.0, ZoomMath.scrollAmount(in, -1.0), 1.0e-9);
        double half = ZoomMath.scrollAmount(4.0, 0.5);
        assertTrue(half > 4.0 && half < in);
    }

    @Test
    void scrollClamps() {
        assertEquals(ZoomMath.MAX_SCROLL_AMOUNT, ZoomMath.scrollAmount(29.0, 5.0), 1.0e-9);
        assertEquals(ZoomMath.MIN_SCROLL_AMOUNT, ZoomMath.scrollAmount(1.2, -5.0), 1.0e-9);
        assertEquals(4.0, ZoomMath.scrollAmount(4.0, Double.NaN), 1.0e-9);
        assertEquals(4.0, ZoomMath.scrollAmount(4.0, Double.POSITIVE_INFINITY), 1.0e-9);
        assertTrue(ZoomMath.scrollAmount(4.0, 1000.0) < 4.0 * Math.pow(ZoomMath.SCROLL_STEP, 5.1));
        assertEquals(ZoomMath.DEFAULT_AMOUNT, ZoomMath.scrollAmount(Double.NaN, 0.0), 1.0e-9);
    }

    @Test
    void amountEasesTowardTarget() {
        double amount = 4.0;
        double previous = amount;
        for (int i = 0; i < 12; i++) {
            amount = ZoomMath.advanceAmount(amount, 8.0, ZoomMath.DEFAULT_SPEED, FRAME_60);
            assertTrue(amount >= previous && amount <= 8.0);
            previous = amount;
        }
        assertTrue(amount > 4.0 && amount < 8.0, "still easing after a fifth of a second");
        for (int i = 0; i < 120; i++) {
            amount = ZoomMath.advanceAmount(amount, 8.0, ZoomMath.DEFAULT_SPEED, FRAME_60);
        }
        assertEquals(8.0, amount, 1.0e-9);
        assertEquals(8.0, ZoomMath.advanceAmount(8.0, 2.0, 8.0, 0.0), 1.0e-9, "no time, no movement");
    }

    @Test
    void scrollCanExceedConfiguredRange() {
        float fov = ZoomMath.applyToFov(70.0F, 12.0, 1.0);
        assertTrue(fov > 5.5F && fov < 6.0F);
        assertEquals(5.0F, ZoomMath.applyToFov(70.0F, 30.0, 1.0), 1.0e-4F);
    }
}
