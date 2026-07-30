package com.dansplugins.detectionsystem.commands;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerNamesTest {

    @Test
    void usesTheNameWhenTheServerKnowsIt() {
        assertEquals("Steve", PlayerNames.displayName("Steve", UUID.randomUUID()));
    }

    @Test
    void fallsBackToTheUuidWhenTheNameIsUnknown() {
        // OfflinePlayer.getName() is null for an account with no cached name; concatenating it
        // straight into a message printed the literal string "null" (issue #74).
        UUID uuid = UUID.randomUUID();

        assertEquals(uuid.toString(), PlayerNames.displayName(null, uuid));
    }

    @Test
    void skipsUnknownNamesWhenMatching() {
        // The regression this exists for: a single account with no cached name used to make the
        // whole /aaf alts completer throw an NPE on name.toLowerCase() (issue #74).
        List<String> names = Arrays.asList("Steve", null, "Steven");

        assertEquals(List.of("Steve", "Steven"), PlayerNames.matchingNames(names, "ste"));
    }

    @Test
    void matchesPrefixesCaseInsensitively() {
        List<String> names = List.of("Steve", "steven", "Alex");

        assertEquals(List.of("Steve", "steven"), PlayerNames.matchingNames(names, "STE"));
        assertEquals(List.of("Alex"), PlayerNames.matchingNames(names, "a"));
    }

    @Test
    void returnsEveryKnownNameForAnEmptyPrefix() {
        List<String> names = Arrays.asList("Steve", null, "Alex");

        assertEquals(List.of("Steve", "Alex"), PlayerNames.matchingNames(names, ""));
    }

    @Test
    void returnsNoNamesWhenNothingMatches() {
        assertEquals(List.of(), PlayerNames.matchingNames(List.of("Steve", "Alex"), "z"));
    }

    @Test
    void returnsNoNamesForAnEmptyCandidateList() {
        assertEquals(List.of(), PlayerNames.matchingNames(List.of(), "ste"));
    }

    @Test
    void keepsTheOrderOfTheCandidateList() {
        List<String> names = List.of("Steven", "Steve", "Stevens");

        assertEquals(List.of("Steven", "Steve", "Stevens"), PlayerNames.matchingNames(names, "steve"));
    }
}
