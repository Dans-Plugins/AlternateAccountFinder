package com.dansplugins.detectionsystem.listeners;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlayerJoinListenerTest {

    private final Logger logger = Logger.getLogger(PlayerJoinListenerTest.class.getName());

    @Test
    void parsesAllValidUuids() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        List<UUID> result = PlayerJoinListener.parseValidUuids(List.of(first.toString(), second.toString()), logger);

        assertEquals(List.of(first, second), result);
    }

    @Test
    void skipsMalformedEntryButKeepsTheRest() {
        // A bad entry must not abort the whole list (issue #66) — the well-formed
        // entries before and after it should still come through.
        UUID before = UUID.randomUUID();
        UUID after = UUID.randomUUID();

        List<UUID> result = PlayerJoinListener.parseValidUuids(
                List.of(before.toString(), "not-a-uuid", after.toString()), logger);

        assertEquals(List.of(before, after), result);
    }

    @Test
    void returnsEmptyListWhenNoEntriesAreValid() {
        List<UUID> result = PlayerJoinListener.parseValidUuids(List.of("nope", "also-not-a-uuid"), logger);

        assertEquals(List.of(), result);
    }

    @Test
    void returnsEmptyListForEmptyInput() {
        List<UUID> result = PlayerJoinListener.parseValidUuids(List.of(), logger);

        assertEquals(List.of(), result);
    }

    // The listener reads notify-users with the one-argument getStringList, and Bukkit registers
    // the jar's config.yml as the defaults behind the file on disk. These pin which spellings of
    // "nobody" in an operator's file actually reach nobody, against YamlConfiguration itself; the
    // Bukkit wiring of the defaults is not exercised here, since it needs a running server.

    @Test
    void anEmptyListInTheOperatorsFileNotifiesNobody() throws InvalidConfigurationException {
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.loadFromString("notify-users: []\n");
        onDisk.setDefaults(bundledConfig());

        assertEquals(List.of(), onDisk.getStringList("notify-users"));
    }

    @Test
    void aMissingKeyFallsBackToTheBundledExampleUuids() {
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.setDefaults(bundledConfig());

        List<String> recipients = onDisk.getStringList("notify-users");

        assertFalse(recipients.isEmpty());
        assertEquals(bundledConfig().getStringList("notify-users"), recipients);
    }

    @Test
    void aKeyLeftWithNoValueFallsBackToTheBundledExampleUuids() throws InvalidConfigurationException {
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.loadFromString("notify-users:\n");
        onDisk.setDefaults(bundledConfig());

        List<String> recipients = onDisk.getStringList("notify-users");

        assertFalse(recipients.isEmpty());
        assertEquals(bundledConfig().getStringList("notify-users"), recipients);
    }

    private static YamlConfiguration bundledConfig() {
        InputStream bundled = PlayerJoinListenerTest.class.getResourceAsStream("/config.yml");
        assertNotNull(bundled, "the bundled config.yml is missing from the classpath");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8));
    }
}
