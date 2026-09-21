package dev.smoothzoom.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.smoothzoom.zoom.ZoomMath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZoomConfigTest {
    @TempDir
    Path dir;

    private Path file() {
        return dir.resolve("config").resolve(ZoomConfig.FILE_NAME);
    }

    private void write(String json) throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), json, StandardCharsets.UTF_8);
    }

    @Test
    void defaults() {
        ZoomConfig config = new ZoomConfig();
        assertEquals(4.0, config.zoomAmount());
        assertEquals(8.0, config.zoomSpeed());
        assertTrue(config.scrollToZoom());
        assertEquals(0.6, config.lookSmoothing());
    }

    @Test
    void settersClamp() {
        ZoomConfig config = new ZoomConfig();

        config.setZoomAmount(100.0);
        assertEquals(ZoomMath.MAX_AMOUNT, config.zoomAmount());
        config.setZoomAmount(-3.0);
        assertEquals(ZoomMath.MIN_AMOUNT, config.zoomAmount());
        config.setZoomAmount(Double.NaN);
        assertEquals(ZoomMath.DEFAULT_AMOUNT, config.zoomAmount());
        config.setZoomAmount(6.5);
        assertEquals(6.5, config.zoomAmount());

        config.setZoomSpeed(-100.0);
        assertEquals(ZoomMath.MIN_SPEED, config.zoomSpeed());
        config.setZoomSpeed(500.0);
        assertEquals(ZoomMath.MAX_SPEED, config.zoomSpeed());
        config.setZoomSpeed(Double.POSITIVE_INFINITY);
        assertEquals(ZoomMath.DEFAULT_SPEED, config.zoomSpeed());

        config.setScrollToZoom(false);
        assertFalse(config.scrollToZoom());

        config.setLookSmoothing(5.0);
        assertEquals(ZoomMath.MAX_LOOK_SMOOTHING, config.lookSmoothing());
        config.setLookSmoothing(-1.0);
        assertEquals(ZoomMath.MIN_LOOK_SMOOTHING, config.lookSmoothing());
        config.setLookSmoothing(Double.NaN);
        assertEquals(ZoomMath.DEFAULT_LOOK_SMOOTHING, config.lookSmoothing());
        config.setLookSmoothing(0.35);
        assertEquals(0.35, config.lookSmoothing());
    }

    @Test
    void resetToDefaults() {
        ZoomConfig config = new ZoomConfig();
        config.setZoomAmount(9.0);
        config.setZoomSpeed(2.0);
        config.setScrollToZoom(false);
        config.setLookSmoothing(0.0);
        config.resetToDefaults();
        assertEquals(ZoomMath.DEFAULT_AMOUNT, config.zoomAmount());
        assertEquals(ZoomMath.DEFAULT_SPEED, config.zoomSpeed());
        assertTrue(config.scrollToZoom());
        assertEquals(ZoomMath.DEFAULT_LOOK_SMOOTHING, config.lookSmoothing());
    }

    @Test
    void saveAndLoad() throws IOException {
        ZoomConfig config = new ZoomConfig();
        config.setZoomAmount(6.25);
        config.setZoomSpeed(12.5);
        config.setScrollToZoom(false);
        config.setLookSmoothing(0.25);
        config.save(file());

        ZoomConfig reloaded = ZoomConfig.load(file());
        assertEquals(6.25, reloaded.zoomAmount());
        assertEquals(12.5, reloaded.zoomSpeed());
        assertFalse(reloaded.scrollToZoom());
        assertEquals(0.25, reloaded.lookSmoothing());
    }

    @Test
    void saveCreatesDirectories() throws IOException {
        new ZoomConfig().save(file());
        assertTrue(Files.isRegularFile(file()));
        assertFalse(Files.exists(file().resolveSibling(ZoomConfig.FILE_NAME + ".tmp")));
    }

    @Test
    void savedKeys() throws IOException {
        new ZoomConfig().save(file());
        String json = Files.readString(file(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"zoomAmount\": 4.0"), json);
        assertTrue(json.contains("\"zoomSpeed\": 8.0"), json);
        assertTrue(json.contains("\"scrollToZoom\": true"), json);
        assertTrue(json.contains("\"lookSmoothing\": 0.6"), json);
    }

    @Test
    void oldConfigWithoutLookSmoothing() throws IOException {
        write("{\"zoomAmount\": 3.5, \"zoomSpeed\": 6.0, \"scrollToZoom\": true}");
        ZoomConfig config = ZoomConfig.load(file());
        assertEquals(ZoomMath.DEFAULT_LOOK_SMOOTHING, config.lookSmoothing());
        assertEquals(3.5, config.zoomAmount());
    }

    @Test
    void missingFile() {
        ZoomConfig config = ZoomConfig.load(file());
        assertEquals(ZoomMath.DEFAULT_AMOUNT, config.zoomAmount());
        assertTrue(Files.isRegularFile(file()));
    }

    @Test
    void outOfRangeValues() throws IOException {
        write("{\"zoomAmount\": 500, \"zoomSpeed\": -4, \"scrollToZoom\": false}");
        ZoomConfig config = ZoomConfig.load(file());
        assertEquals(ZoomMath.MAX_AMOUNT, config.zoomAmount());
        assertEquals(ZoomMath.MIN_SPEED, config.zoomSpeed());
        assertFalse(config.scrollToZoom());
    }

    @Test
    void missingAndUnknownKeys() throws IOException {
        write("{\"zoomAmount\": 3.5, \"somethingFromAFutureVersion\": [1, 2, 3]}");
        ZoomConfig config = ZoomConfig.load(file());
        assertEquals(3.5, config.zoomAmount());
        assertEquals(ZoomMath.DEFAULT_SPEED, config.zoomSpeed());
        assertTrue(config.scrollToZoom());
    }

    @Test
    void nonFiniteNumbers() throws IOException {
        write("{\"zoomAmount\": NaN, \"zoomSpeed\": Infinity}");
        ZoomConfig config = ZoomConfig.load(file());
        assertEquals(ZoomMath.DEFAULT_AMOUNT, config.zoomAmount());
        assertEquals(ZoomMath.DEFAULT_SPEED, config.zoomSpeed());
    }

    @Test
    void brokenFileIsMovedAside() throws IOException {
        write("{ this is not json");
        ZoomConfig config = ZoomConfig.load(file());
        assertEquals(ZoomMath.DEFAULT_AMOUNT, config.zoomAmount());

        Path backup = file().resolveSibling(ZoomConfig.FILE_NAME + ".broken");
        assertEquals("{ this is not json", Files.readString(backup, StandardCharsets.UTF_8));
        assertEquals(ZoomMath.DEFAULT_AMOUNT, ZoomConfig.load(file()).zoomAmount());
    }

    @Test
    void wrongTypedValue() throws IOException {
        write("{\"zoomAmount\": \"lots\"}");
        assertEquals(ZoomMath.DEFAULT_AMOUNT, ZoomConfig.load(file()).zoomAmount());
        assertTrue(Files.exists(file().resolveSibling(ZoomConfig.FILE_NAME + ".broken")));
    }

    @Test
    void emptyFile() throws IOException {
        write("");
        ZoomConfig config = ZoomConfig.load(file());
        assertEquals(ZoomMath.DEFAULT_AMOUNT, config.zoomAmount());
        assertEquals(ZoomMath.DEFAULT_SPEED, config.zoomSpeed());
    }
}
