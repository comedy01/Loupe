package dev.loupe.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import dev.loupe.zoom.ZoomMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ZoomConfig {
    public static final String FILE_NAME = "loupe.json";
    public static final boolean DEFAULT_SCROLL_TO_ZOOM = true;

    private static final Logger LOGGER = LoggerFactory.getLogger("loupe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("zoomAmount")
    private double zoomAmount = ZoomMath.DEFAULT_AMOUNT;

    @SerializedName("zoomSpeed")
    private double zoomSpeed = ZoomMath.DEFAULT_SPEED;

    @SerializedName("scrollToZoom")
    private boolean scrollToZoom = DEFAULT_SCROLL_TO_ZOOM;

    @SerializedName("lookSmoothing")
    private double lookSmoothing = ZoomMath.DEFAULT_LOOK_SMOOTHING;

    public double zoomAmount() {
        return zoomAmount;
    }

    public void setZoomAmount(double value) {
        zoomAmount = ZoomMath.clampAmount(value);
    }

    public double zoomSpeed() {
        return zoomSpeed;
    }

    public void setZoomSpeed(double value) {
        zoomSpeed = ZoomMath.clampSpeed(value);
    }

    public boolean scrollToZoom() {
        return scrollToZoom;
    }

    public void setScrollToZoom(boolean value) {
        scrollToZoom = value;
    }

    public double lookSmoothing() {
        return lookSmoothing;
    }

    public void setLookSmoothing(double value) {
        lookSmoothing = ZoomMath.clampLookSmoothing(value);
    }

    public void resetToDefaults() {
        zoomAmount = ZoomMath.DEFAULT_AMOUNT;
        zoomSpeed = ZoomMath.DEFAULT_SPEED;
        scrollToZoom = DEFAULT_SCROLL_TO_ZOOM;
        lookSmoothing = ZoomMath.DEFAULT_LOOK_SMOOTHING;
    }

    private void sanitize() {
        setZoomAmount(zoomAmount);
        setZoomSpeed(zoomSpeed);
        setLookSmoothing(lookSmoothing);
    }

    public static ZoomConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            ZoomConfig fresh = new ZoomConfig();
            fresh.saveQuietly(file);
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ZoomConfig loaded = GSON.fromJson(reader, ZoomConfig.class);
            if (loaded == null) {
                throw new JsonParseException("config file is empty");
            }
            loaded.sanitize();
            return loaded;
        } catch (IOException | RuntimeException e) {
            // gson throws several unrelated exception types on bad content
            LOGGER.warn("Could not read {}; using defaults. {}", file, e.toString());
            moveAside(file);
            ZoomConfig fresh = new ZoomConfig();
            fresh.saveQuietly(file);
            return fresh;
        }
    }

    public void save(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void saveQuietly(Path file) {
        try {
            save(file);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(
                    file,
                    file.resolveSibling(file.getFileName() + ".broken"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up unreadable config {}: {}", file, e.toString());
        }
    }
}
