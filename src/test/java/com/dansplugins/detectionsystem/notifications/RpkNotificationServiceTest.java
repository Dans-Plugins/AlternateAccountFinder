package com.dansplugins.detectionsystem.notifications;

import com.rpkit.notifications.bukkit.notification.RPKNotificationService;
import com.rpkit.players.bukkit.profile.RPKProfile;
import com.rpkit.players.bukkit.profile.RPKThinProfile;
import com.rpkit.players.bukkit.profile.minecraft.RPKMinecraftProfile;
import com.rpkit.players.bukkit.profile.minecraft.RPKMinecraftProfileService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the RPKit delivery path (issue #106), which previously dereferenced whatever RPKit
 * handed back and threw a {@link NullPointerException} on a Bukkit async thread when RPKit had
 * nothing to hand back.
 *
 * <p>No mock framework is configured in this project, so each RPKit collaborator here is a
 * {@link Proxy} over its interface, as {@code AafCommandTest} does for {@code CommandSender}.
 * The tests exercise {@code deliverNotification} directly — the scheduling around it in
 * {@code sendNotification} needs a live server and stays uncovered, as does the
 * {@code Services.INSTANCE} lookup that supplies the two services.
 */
class RpkNotificationServiceTest {

    private static final UUID RECIPIENT = UUID.fromString("0a9fa342-3139-49d7-8acb-fcf4d9c1f0ef");

    private final RecordingLogger logger = new RecordingLogger();

    @Test
    void warnsAndSkipsWhenTheMinecraftProfileServiceIsUnavailable() {
        RecordingNotificationService notifications = new RecordingNotificationService();

        RpkNotificationService.deliverNotification(
                null, notifications.asService(), RECIPIENT, "title", "body", logger.asLogger());

        assertEquals(List.of(), notifications.created);
        assertEquals(1, logger.warnings.size());
        assertTrue(logger.warnings.get(0).contains(RECIPIENT.toString()), logger.warnings.get(0));
    }

    @Test
    void warnsAndSkipsWhenTheNotificationServiceIsUnavailable() {
        RecordingProfileService profiles = new RecordingProfileService(minecraftProfileFor(profile()));

        RpkNotificationService.deliverNotification(
                profiles.asService(), null, RECIPIENT, "title", "body", logger.asLogger());

        assertEquals(1, logger.warnings.size());
        assertTrue(logger.warnings.get(0).contains(RECIPIENT.toString()), logger.warnings.get(0));
    }

    @Test
    void warnsAndSkipsWhenRpkitHasNoMinecraftProfileForTheRecipient() {
        RecordingProfileService profiles = new RecordingProfileService(null);
        RecordingNotificationService notifications = new RecordingNotificationService();

        RpkNotificationService.deliverNotification(
                profiles.asService(), notifications.asService(), RECIPIENT, "title", "body", logger.asLogger());

        assertEquals(List.of(), notifications.created);
        assertEquals(1, logger.warnings.size());
        assertTrue(logger.warnings.get(0).contains(RECIPIENT.toString()), logger.warnings.get(0));
    }

    @Test
    void skipsARecipientWhoseMinecraftProfileIsNotLinkedToAProfile() {
        // A thin profile is an ordinary RPKit state for an unlinked account, and the notification
        // library has nothing to address in that case; it is skipped rather than thrown from.
        RPKThinProfile thinProfile = (RPKThinProfile) Proxy.newProxyInstance(
                RPKThinProfile.class.getClassLoader(),
                new Class<?>[]{RPKThinProfile.class},
                (proxy, method, args) -> unstubbed(method));
        RecordingProfileService profiles = new RecordingProfileService(minecraftProfileFor(thinProfile));
        RecordingNotificationService notifications = new RecordingNotificationService();

        RpkNotificationService.deliverNotification(
                profiles.asService(), notifications.asService(), RECIPIENT, "title", "body", logger.asLogger());

        assertEquals(List.of(), notifications.created);
    }

    @Test
    void deliversTheNotificationWhenRpkitSuppliesEverything() {
        RPKProfile profile = profile();
        RecordingProfileService profiles = new RecordingProfileService(minecraftProfileFor(profile));
        RecordingNotificationService notifications = new RecordingNotificationService();

        RpkNotificationService.deliverNotification(
                profiles.asService(), notifications.asService(), RECIPIENT, "Steve - potential alts",
                "Steve is potentially an alt of: Alex", logger.asLogger());

        assertEquals(List.of(RECIPIENT), profiles.requested);
        assertEquals(List.of(List.of(profile, "Steve - potential alts", "Steve is potentially an alt of: Alex")),
                notifications.created);
        assertEquals(List.of(), logger.warnings);
    }

    @Test
    void noWarningEverNamesAnAddress() {
        // The project's primary failure mode: nothing on this path may disclose an IP address.
        // Every skip branch is walked, and only the recipient account may appear in the output.
        RecordingProfileService noProfile = new RecordingProfileService(null);
        RecordingNotificationService notifications = new RecordingNotificationService();

        RpkNotificationService.deliverNotification(
                null, notifications.asService(), RECIPIENT, "title", "body", logger.asLogger());
        RpkNotificationService.deliverNotification(
                noProfile.asService(), null, RECIPIENT, "title", "body", logger.asLogger());
        RpkNotificationService.deliverNotification(
                noProfile.asService(), notifications.asService(), RECIPIENT, "title", "body", logger.asLogger());

        assertEquals(3, logger.warnings.size());
        for (String warning : logger.warnings) {
            assertFalse(warning.matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+.*"), warning);
            assertTrue(warning.contains(RECIPIENT.toString()), warning);
        }
    }

    private static RPKProfile profile() {
        return (RPKProfile) Proxy.newProxyInstance(
                RPKProfile.class.getClassLoader(),
                new Class<?>[]{RPKProfile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "RPKProfile";
                    default -> unstubbed(method);
                });
    }

    private static RPKMinecraftProfile minecraftProfileFor(RPKThinProfile profile) {
        return (RPKMinecraftProfile) Proxy.newProxyInstance(
                RPKMinecraftProfile.class.getClassLoader(),
                new Class<?>[]{RPKMinecraftProfile.class},
                (proxy, method, args) -> method.getName().equals("getProfile") ? profile : unstubbed(method));
    }

    private static Object unstubbed(Method method) {
        // Reaching anything else means a test wandered onto a path that needs a real RPKit
        // installation; failing loudly beats handing back a plausible-looking null.
        throw new UnsupportedOperationException(method.getDeclaringClass().getSimpleName()
                + "." + method.getName() + " is not stubbed");
    }

    /** Records the recipients asked for, and answers with one fixed profile (possibly none). */
    private static final class RecordingProfileService implements InvocationHandler {

        private final RPKMinecraftProfile minecraftProfile;
        private final List<UUID> requested = new ArrayList<>();

        private RecordingProfileService(RPKMinecraftProfile minecraftProfile) {
            this.minecraftProfile = minecraftProfile;
        }

        private RPKMinecraftProfileService asService() {
            return (RPKMinecraftProfileService) Proxy.newProxyInstance(
                    RPKMinecraftProfileService.class.getClassLoader(),
                    new Class<?>[]{RPKMinecraftProfileService.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("getMinecraftProfile") && args[0] instanceof UUID uuid) {
                requested.add(uuid);
                return CompletableFuture.completedFuture(minecraftProfile);
            }
            return unstubbed(method);
        }
    }

    /** Records every {@code createNotification} call as its argument list. */
    private static final class RecordingNotificationService implements InvocationHandler {

        private final List<List<Object>> created = new ArrayList<>();

        private RPKNotificationService asService() {
            return (RPKNotificationService) Proxy.newProxyInstance(
                    RPKNotificationService.class.getClassLoader(),
                    new Class<?>[]{RPKNotificationService.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("createNotification")) {
                created.add(List.of(args));
                return CompletableFuture.completedFuture(null);
            }
            return unstubbed(method);
        }
    }

    /** A {@link Logger} that keeps its warnings to itself, and to the assertions above. */
    private static final class RecordingLogger extends Handler {

        private final List<String> warnings = new ArrayList<>();

        private Logger asLogger() {
            Logger logger = Logger.getLogger(RpkNotificationServiceTest.class.getName() + "." + System.nanoTime());
            logger.setUseParentHandlers(false);
            logger.addHandler(this);
            return logger;
        }

        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                warnings.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
