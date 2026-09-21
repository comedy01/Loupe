package dev.loupe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProjectMetadataTest {
    private static JsonObject resourceJson(String name) throws IOException {
        InputStream in = ProjectMetadataTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(in, name + " must be on the classpath");
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    void modJson() throws IOException {
        JsonObject mod = resourceJson("fabric.mod.json");
        assertEquals("loupe", mod.get("id").getAsString());
        assertEquals("Loupe", mod.get("name").getAsString());
        assertEquals("client", mod.get("environment").getAsString());
        assertFalse(mod.get("version").getAsString().contains("$"), "version placeholder must be expanded");

        JsonObject entrypoints = mod.getAsJsonObject("entrypoints");
        assertEquals("dev.loupe.client.LoupeClient", entrypoints.getAsJsonArray("client").get(0).getAsString());

        JsonObject depends = mod.getAsJsonObject("depends");
        assertTrue(depends.has("fabric-key-mapping-api-v1") || depends.has("fabric-key-binding-api-v1"));
        assertFalse(depends.has("modmenu"), "Mod Menu must stay optional");
        assertTrue(mod.getAsJsonObject("suggests").has("modmenu"));
    }

    @Test
    void entrypointsAndMixinsExist() throws IOException {
        JsonObject mod = resourceJson("fabric.mod.json");
        JsonObject entrypoints = mod.getAsJsonObject("entrypoints");
        for (String key : entrypoints.keySet()) {
            for (JsonElement entry : entrypoints.getAsJsonArray(key)) {
                assertClassExists(entry.getAsString());
            }
        }

        JsonObject mixins = resourceJson("loupe.mixins.json");
        assertEquals("dev.loupe.mixin", mixins.get("package").getAsString());
        JsonArray client = mixins.getAsJsonArray("client");
        assertEquals(2, client.size());
        for (JsonElement entry : client) {
            assertClassExists("dev.loupe.mixin." + entry.getAsString());
        }
        assertFalse(mixins.has("mixins"), "all mixins are client-only");
        assertFalse(mixins.has("server"));
        assertTrue(mod.getAsJsonArray("mixins").get(0).getAsString().equals("loupe.mixins.json"));
    }

    private static void assertClassExists(String className) {
        String path = className.replace('.', '/') + ".class";
        assertNotNull(ProjectMetadataTest.class.getClassLoader().getResource(path), path + " must be compiled");
    }

    @Test
    void noOtherProjectReferences() throws IOException {
        String[] forbidden = {"rot" + "client", "rot-" + "client", "rot " + "client", "rot" + "tools", "fi." + "rot"};
        try (Stream<Path> files = Stream.of(Path.of("src/main"), Path.of("build.gradle"), Path.of("gradle.properties"),
                        Path.of("settings.gradle"), Path.of("README.md"), Path.of("LICENSE"), Path.of("docs"))
                .filter(Files::exists)
                .flatMap(ProjectMetadataTest::walk)) {
            files.filter(Files::isRegularFile).filter(file -> !file.toString().endsWith(".png") && !file.toString().endsWith(".jar")).forEach(file -> {
                String text;
                try {
                    text = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                } catch (IOException e) {
                    throw new IllegalStateException(file.toString(), e);
                }
                for (String token : forbidden) {
                    assertFalse(text.contains(token), file + " mentions \"" + token + "\"");
                }
            });
        }
    }

    private static Stream<Path> walk(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException e) {
            throw new IllegalStateException(root.toString(), e);
        }
    }
}
