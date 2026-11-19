package com.dansplugins.detectionsystem.trace;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Decides whether {@code usage-reporting.enabled} allows reporting, and keeps an operator's
 * opt-out from ever being turned back on by the plugin.
 * <p>
 * Fails closed: only a value that clearly means "on" ({@link TraceClient#allowsReporting(Object)})
 * turns reporting on, and config.yml as it is on disk right now is read directly, so neither a
 * stale copy of the config held in memory nor Bukkit's fallback to the bundled defaults can
 * override an {@code enabled: false} the operator wrote.
 */
public final class UsageReportingSetting {
    public static final String ENABLED_KEY = "usage-reporting.enabled";

    /** The message to log, as SEVERE, when {@link #prepareSave} refuses to write config.yml. */
    public static final String UNPARSEABLE_MESSAGE = "config.yml could not be read, so it was not overwritten. "
            + "Fix the YAML error in it and restart; until then the defaults are used, and usage reporting is off.";

    private UsageReportingSetting() {
    }

    /**
     * The value of {@code usage-reporting.enabled} written in {@code configFile}, with no defaults
     * applied; {@code null} when the file or the key is missing. A file that exists but cannot be
     * parsed reads as {@link Boolean#FALSE}: Bukkit loads such a file as empty, which would fall
     * back to the bundled {@code true}, but what the operator wrote in it cannot be known.
     */
    public static Object readFromDisk(File configFile) {
        if (configFile == null || !configFile.isFile()) {
            return null;
        }
        YamlConfiguration onDisk = new YamlConfiguration();
        try {
            onDisk.load(configFile);
        } catch (Exception e) {
            return Boolean.FALSE;
        }
        Object value = onDisk.get(ENABLED_KEY);
        if (value == null && hasBlankEnabledLine(configFile)) {
            return Boolean.FALSE;
        }
        return value;
    }

    /**
     * Whether {@code configFile} has an {@code enabled:} line with no value (or only a comment)
     * inside its top-level {@code usage-reporting:} block. Bukkit drops a key with no value when
     * it loads the file, so the bundled {@code true} would win; but an operator who blanked the
     * switch did not ask for reporting, so it counts as off.
     */
    static boolean hasBlankEnabledLine(File configFile) {
        java.util.List<String> lines;
        try {
            lines = java.nio.file.Files.readAllLines(configFile.toPath(), java.nio.charset.StandardCharsets.ISO_8859_1);
        } catch (Exception e) {
            return false;
        }
        boolean inBlock = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            boolean topLevel = !Character.isWhitespace(line.charAt(0));
            if (topLevel) {
                inBlock = trimmed.matches("usage-reporting\\s*:\\s*(#.*)?");
                continue;
            }
            if (inBlock && trimmed.matches("enabled\\s*:\\s*(#.*)?")) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code configFile} exists but cannot be parsed as YAML. */
    public static boolean isUnparseable(File configFile) {
        if (configFile == null || !configFile.isFile()) {
            return false;
        }
        try {
            new YamlConfiguration().load(configFile);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /** Whether {@code configFile} on disk turns reporting off. */
    public static boolean turnedOffOnDisk(File configFile) {
        Object onDisk = readFromDisk(configFile);
        return onDisk != null && !TraceClient.allowsReporting(onDisk);
    }

    /**
     * Whether reporting is on: both the loaded {@code config} (whose one-argument get() falls
     * through to the bundled defaults when the file has no block) and {@code configFile} on disk
     * must allow it.
     */
    public static boolean isEnabled(ConfigurationSection config, File configFile) {
        return !turnedOffOnDisk(configFile) && TraceClient.allowsReporting(config.get(ENABLED_KEY));
    }

    /**
     * Carries an opt-out written in {@code configFile} into {@code config}, so that saving
     * {@code config} over the file cannot turn reporting back on. {@code config} may be older than
     * the file: an operator can set {@code enabled: false} by hand while the server is running.
     */
    public static void keepOptOutFromDisk(Configuration config, File configFile) {
        if (turnedOffOnDisk(configFile)) {
            config.set(ENABLED_KEY, false);
        }
    }

    /**
     * Gets {@code config} ready to be saved over {@code configFile}, and says whether it may be.
     * <p>
     * {@code false} means the file must not be written: it exists but cannot be parsed, so Bukkit
     * loaded it as empty and saving would replace everything the operator wrote, an opt-out
     * included, with the bundled defaults. Otherwise an opt-out found in the file on disk is
     * carried into {@code config} first, unless {@code switchSetByOperator}: the operator has just
     * set {@code usage-reporting.enabled} with a command, the one way the plugin may turn it on.
     */
    public static boolean prepareSave(Configuration config, File configFile, boolean switchSetByOperator) {
        if (isUnparseable(configFile)) {
            return false;
        }
        if (!switchSetByOperator) {
            keepOptOutFromDisk(config, configFile);
        }
        return true;
    }
}
