package com.dansplugins.detectionsystem.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for the {@code /aaf} dispatch point (issue #86).
 *
 * <p>No mock framework is configured in this project, so the {@link CommandSender} these tests
 * pass in is a {@link Proxy} over the Bukkit interface that records {@code sendMessage} calls and
 * answers {@code hasPermission} from a fixed verdict. That is enough to cover routing and the
 * usage messages, because {@link AafCommand} and the argument-count branches of the two
 * sub-commands touch nothing else.
 *
 * <p>What remains uncovered, deliberately: every branch that reaches {@code plugin.getServer()} —
 * the accounts and alts lookups themselves, and the online-player suggestions of the alts
 * completer. Those need a live server, not a richer fake. The plugin reference is therefore
 * {@code null} throughout, and each test stays on a path that never dereferences it.
 */
class AafCommandTest {

    private static final String AAF_USAGE = ChatColor.RED + "Usage: /aaf [accounts, alts]";

    private final AafCommand command = new AafCommand(null);

    @Test
    void noArgumentsPrintsTheUsageMessage() {
        RecordingSender sender = new RecordingSender(true);

        assertTrue(command.onCommand(sender.asCommandSender(), null, "aaf", new String[0]));

        assertEquals(List.of(AAF_USAGE), sender.messages);
    }

    @Test
    void unknownSubcommandPrintsTheUsageMessage() {
        RecordingSender sender = new RecordingSender(true);

        assertTrue(command.onCommand(sender.asCommandSender(), null, "aaf", new String[]{"nonsense"}));

        assertEquals(List.of(AAF_USAGE), sender.messages);
    }

    @Test
    void routesToTheAccountsCommandAndDropsTheSubcommandArgument() {
        // The accounts usage message is only sent when the sub-command sees zero arguments, so
        // seeing it proves both that /aaf accounts routed there and that "accounts" was stripped
        // from the argument array before the hand-off.
        RecordingSender sender = new RecordingSender(true);

        assertTrue(command.onCommand(sender.asCommandSender(), null, "aaf", new String[]{"accounts"}));

        assertEquals(List.of(ChatColor.RED + "Usage: /aaf accounts <ip>"), sender.messages);
    }

    @Test
    void routesToTheAltsCommandAndDropsTheSubcommandArgument() {
        RecordingSender sender = new RecordingSender(true);

        assertTrue(command.onCommand(sender.asCommandSender(), null, "aaf", new String[]{"alts"}));

        assertEquals(List.of(ChatColor.RED + "Usage: /aaf alts <player>"), sender.messages);
    }

    @Test
    void routesSubcommandNamesRegardlessOfCase() {
        RecordingSender accounts = new RecordingSender(true);
        RecordingSender alts = new RecordingSender(true);

        command.onCommand(accounts.asCommandSender(), null, "aaf", new String[]{"ACCOUNTS"});
        command.onCommand(alts.asCommandSender(), null, "aaf", new String[]{"AlTs"});

        assertEquals(List.of(ChatColor.RED + "Usage: /aaf accounts <ip>"), accounts.messages);
        assertEquals(List.of(ChatColor.RED + "Usage: /aaf alts <player>"), alts.messages);
    }

    @Test
    void routedSubcommandsStillCheckTheirOwnPermission() {
        // /aaf itself has no permission check; the sub-command it routes to is what refuses.
        RecordingSender sender = new RecordingSender(false);

        assertTrue(command.onCommand(sender.asCommandSender(), null, "aaf", new String[]{"accounts"}));

        assertEquals(List.of(ChatColor.RED + "You do not have permission to use this command."), sender.messages);
    }

    @Test
    void suggestsEverySubcommandWhenNothingHasBeenTyped() {
        RecordingSender sender = new RecordingSender(true);

        List<String> suggestions = command.onTabComplete(sender.asCommandSender(), null, "aaf", new String[0]);

        assertEquals(List.of("accounts", "alts"), suggestions);
    }

    @Test
    void filtersSubcommandSuggestionsByPrefix() {
        RecordingSender sender = new RecordingSender(true);

        assertEquals(List.of("accounts", "alts"),
                command.onTabComplete(sender.asCommandSender(), null, "aaf", new String[]{"a"}));
        assertEquals(List.of("accounts"),
                command.onTabComplete(sender.asCommandSender(), null, "aaf", new String[]{"acc"}));
    }

    @Test
    void filtersSubcommandSuggestionsCaseInsensitively() {
        RecordingSender sender = new RecordingSender(true);

        assertEquals(List.of("alts"),
                command.onTabComplete(sender.asCommandSender(), null, "aaf", new String[]{"ALT"}));
    }

    @Test
    void suggestsNothingForAPrefixNoSubcommandStartsWith() {
        RecordingSender sender = new RecordingSender(true);

        assertEquals(List.of(),
                command.onTabComplete(sender.asCommandSender(), null, "aaf", new String[]{"z"}));
    }

    @Test
    void suggestsNothingForTheArgumentsOfAnUnknownSubcommand() {
        RecordingSender sender = new RecordingSender(true);

        assertEquals(List.of(),
                command.onTabComplete(sender.asCommandSender(), null, "aaf", new String[]{"nonsense", "steve"}));
    }

    @Test
    void suggestsNothingForTheIpArgumentOfAccounts() {
        // Enumerating IPs here would reintroduce the disclosure /aaf ips was removed for
        // (issues #44 and #64), so the accounts completer must stay empty.
        RecordingSender sender = new RecordingSender(true);

        assertEquals(List.of(),
                command.onTabComplete(sender.asCommandSender(), null, "aaf", new String[]{"accounts", "1.2.3.4"}));
    }

    @Test
    void suggestsNothingForTheAltsPlayerArgumentWithoutPermission() {
        RecordingSender sender = new RecordingSender(false);

        assertEquals(List.of(),
                command.onTabComplete(sender.asCommandSender(), null, "aaf", new String[]{"alts", "ste"}));
    }

    @Test
    void suggestsNothingBeyondTheAltsPlayerArgument() {
        // /aaf alts takes a single argument; anything past it is delegated to the alts completer,
        // which stops before it would ask the server for online players.
        RecordingSender sender = new RecordingSender(true);

        assertEquals(List.of(),
                command.onTabComplete(sender.asCommandSender(), null, "aaf", new String[]{"alts", "steve", "extra"}));
    }

    @Test
    void neverSuggestsAnythingThatCouldBeAnIpAddress() {
        // Guards the project's primary failure mode: no completer branch reachable from /aaf may
        // hand back an address. The branches that need a live server are not covered here.
        RecordingSender sender = new RecordingSender(true);
        List<String[]> argumentLists = List.of(
                new String[0],
                new String[]{"a"},
                new String[]{"accounts"},
                new String[]{"accounts", "1.2.3.4"},
                new String[]{"alts", "steve", "extra"},
                new String[]{"nonsense", "steve"});

        for (String[] args : argumentLists) {
            List<String> suggestions = command.onTabComplete(sender.asCommandSender(), null, "aaf", args);
            assertTrue(suggestions.stream().allMatch(suggestion -> suggestion.equals("accounts") || suggestion.equals("alts")),
                    "unexpected suggestion for /aaf " + String.join(" ", args) + ": " + suggestions);
        }
    }

    /**
     * A {@link CommandSender} stand-in that records the messages sent to it. Built as a dynamic
     * proxy rather than an implementing class so that it does not have to be updated every time
     * the Spigot API gains a method.
     */
    private static final class RecordingSender implements InvocationHandler {

        private final boolean permitted;
        private final List<String> messages = new ArrayList<>();

        private RecordingSender(boolean permitted) {
            this.permitted = permitted;
        }

        private CommandSender asCommandSender() {
            return (CommandSender) Proxy.newProxyInstance(
                    CommandSender.class.getClassLoader(), new Class<?>[]{CommandSender.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "sendMessage":
                    for (Object argument : args) {
                        if (argument instanceof String message) {
                            messages.add(message);
                        } else if (argument instanceof String[] lines) {
                            messages.addAll(List.of(lines));
                        }
                    }
                    return null;
                case "hasPermission":
                    return permitted;
                case "getName":
                    return "RecordingSender";
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "RecordingSender";
                default:
                    // Anything else means a test wandered onto a path that needs a real server;
                    // failing loudly here is better than returning a plausible-looking null.
                    throw new UnsupportedOperationException(
                            "CommandSender." + method.getName() + " is not stubbed");
            }
        }
    }
}
