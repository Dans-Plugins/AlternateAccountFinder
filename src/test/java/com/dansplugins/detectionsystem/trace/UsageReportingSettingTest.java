package com.dansplugins.detectionsystem.trace;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An operator who sets {@code usage-reporting.enabled: false} must find reporting off, every
 * time, whatever the plugin does with config.yml afterwards.
 */
class UsageReportingSettingTest {

    @TempDir
    Path folder;

    private File configFile(String contents) throws IOException {
        File file = folder.resolve("config.yml").toFile();
        write(file, contents);
        return file;
    }

    private static void write(File file, String contents) throws IOException {
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    /** What JavaPlugin.getConfig() loads: the file, with the jar's config.yml as defaults. */
    private YamlConfiguration loadLikeBukkit(File file) throws IOException {
        // Bukkit's loadConfiguration() logs a file it cannot parse and returns it empty.
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            // left empty, as Bukkit does
        }
        try (InputStream bundled = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }
        return config;
    }

    /** What the plugin does on every start: copy the defaults in, stamp the version, save. */
    private void startupSave(File file) throws IOException {
        YamlConfiguration config = loadLikeBukkit(file);
        config.options().copyDefaults(true);
        config.set("version", "9.9.9");
        if (UsageReportingSetting.prepareSave(config, file, false)) {
            config.save(file);
        }
    }

    @Test
    void anUpgradeKeepsReportingOff() throws IOException {
        File file = configFile("version: 1.0.0\nusage-reporting:\n  enabled: false\n");

        startupSave(file);

        assertEquals(false, UsageReportingSetting.readFromDisk(file));
        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
    }

    @Test
    void aFileWithoutTheBlockReportsByDefault() throws IOException {
        File file = configFile("version: 1.0.0\n");

        assertTrue(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
    }

    @Test
    void aQuotedFalseTurnsReportingOff() throws IOException {
        File file = configFile("usage-reporting:\n  enabled: \"false\"\n");
        // Bukkit's getBoolean() returns the bundled default, true, for the string "false".
        assertTrue(loadLikeBukkit(file).getBoolean(UsageReportingSetting.ENABLED_KEY));

        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
    }

    @Test
    void anUnrecognisedValueTurnsReportingOff() throws IOException {
        File file = configFile("usage-reporting:\n  enabled: disabled\n");

        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
    }

    @Test
    void aStaleLoadedConfigCannotOverwriteAnOptOutMadeWhileRunning() throws IOException {
        File file = configFile("usage-reporting:\n  enabled: true\n");
        YamlConfiguration loaded = loadLikeBukkit(file);
        // The operator turns reporting off by hand while the server runs.
        write(file, "usage-reporting:\n  enabled: false\n");

        // Something later saves the loaded, stale config.
        loaded.set("some-other-option", true);
        assertTrue(UsageReportingSetting.prepareSave(loaded, file, false));
        loaded.save(file);

        assertEquals(false, UsageReportingSetting.readFromDisk(file));
    }

    @Test
    void theOperatorCanStillTurnReportingBackOnWithACommand() throws IOException {
        File file = configFile("usage-reporting:\n  enabled: false\n");
        YamlConfiguration loaded = loadLikeBukkit(file);

        loaded.set(UsageReportingSetting.ENABLED_KEY, true);
        assertTrue(UsageReportingSetting.prepareSave(loaded, file, true));
        loaded.save(file);

        assertEquals(true, UsageReportingSetting.readFromDisk(file));
    }

    @Test
    void aFileBukkitCannotParseIsNeverOverwrittenAndReportingStaysOff() throws IOException {
        String broken = "usage-reporting:\n  enabled: false\n\tother: [\n";
        File file = configFile(broken);
        // Bukkit loads it as empty, so every getter falls through to the bundled defaults.
        assertTrue(loadLikeBukkit(file).getBoolean(UsageReportingSetting.ENABLED_KEY));

        assertTrue(UsageReportingSetting.isUnparseable(file));
        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
        startupSave(file);
        assertEquals(broken, read(file), "the operator's file must be left as it is");
    }

    @Test
    void aMissingFileIsNotAnOptOut() {
        File file = folder.resolve("absent.yml").toFile();

        assertNull(UsageReportingSetting.readFromDisk(file));
        assertFalse(UsageReportingSetting.turnedOffOnDisk(file));
        assertFalse(UsageReportingSetting.isUnparseable(file));
        assertTrue(UsageReportingSetting.prepareSave(new YamlConfiguration(), file, false));
    }
}
