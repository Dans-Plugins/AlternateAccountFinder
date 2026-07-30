package com.dansplugins.detectionsystem.commands;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Presents Minecraft account names, which Bukkit reports as nullable:
 * {@link org.bukkit.OfflinePlayer#getName()} returns {@code null} for an account the server has
 * no cached name for. Dereferencing it directly made tab-completion throw and printed the literal
 * string {@code null} into command output (see issue #74).
 * <p>
 * Neither helper ever falls back to the address an account logged in from — an address must not
 * reach command output (see issue #44).
 */
public final class PlayerNames {

    private PlayerNames() {
    }

    /**
     * Returns the account's name, or its UUID when the server does not know the name, so output
     * always identifies the account by something a moderator can act on.
     *
     * @param name          the account's name; may be null
     * @param minecraftUuid the account's UUID, used when the name is unknown
     * @return the name, or the UUID as a string
     */
    public static String displayName(String name, UUID minecraftUuid) {
        return name != null ? name : minecraftUuid.toString();
    }

    /**
     * Returns the names that start with {@code prefix}, compared case-insensitively. Accounts
     * whose name is unknown are skipped rather than throwing, so one uncached account cannot
     * break the whole suggestion list. An empty prefix matches every known name.
     *
     * @param names  the candidate names; individual entries may be null
     * @param prefix the prefix typed so far
     * @return the matching names, in the order they were given
     */
    public static List<String> matchingNames(Collection<String> names, String prefix) {
        String lowercasePrefix = prefix.toLowerCase();
        return names.stream()
                .filter(Objects::nonNull)
                .filter(name -> name.toLowerCase().startsWith(lowercasePrefix))
                .toList();
    }
}
