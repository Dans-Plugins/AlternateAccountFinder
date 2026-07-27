package com.dansplugins.detectionsystem.listeners;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
