package com.dansplugins.detectionsystem.encryption;

import java.util.regex.Pattern;

/**
 * Decides what a value stored in the {@code address} column actually is, so the startup
 * migration can tell an unmigrated plaintext IP apart from ciphertext it simply cannot read.
 * <p>
 * A failed decrypt on its own is <em>not</em> evidence of plaintext: ciphertext written under a
 * different key also fails to decrypt. Encrypting such a value would double-encrypt it and
 * destroy the row in a new, irreversible way. This classifier therefore only reports
 * {@link Classification#PLAINTEXT_IP} when the value is both undecryptable with the current key
 * <em>and</em> parses as an IPv4 or IPv6 literal; anything else is {@link Classification#UNRECOGNIZED}
 * and must be left untouched.
 */
public final class StoredAddressClassifier {

    /**
     * What a stored {@code address} value turned out to be.
     */
    public enum Classification {
        /** Null, empty, or whitespace — nothing to migrate. */
        BLANK,
        /** Decryptable with the current key, so it is already in the encrypted format. */
        ENCRYPTED,
        /** Not decryptable with the current key, but a valid IP literal — safe to encrypt. */
        PLAINTEXT_IP,
        /**
         * Neither decryptable with the current key nor a valid IP literal. Most likely ciphertext
         * written under a key that is no longer present. Must be left as-is.
         */
        UNRECOGNIZED
    }

    private static final Pattern IPV4 = Pattern.compile(
            "(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])(?:\\.(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}");
    private static final Pattern IPV6_GROUP = Pattern.compile("[0-9A-Fa-f]{1,4}");

    private final IpEncryption ipEncryption;

    public StoredAddressClassifier(IpEncryption ipEncryption) {
        this.ipEncryption = ipEncryption;
    }

    /**
     * Classifies a value read from the {@code address} column.
     *
     * @param storedAddress the stored value; may be null
     * @return how the migration should treat this value
     */
    public Classification classify(String storedAddress) {
        if (storedAddress == null || storedAddress.trim().isEmpty()) {
            return Classification.BLANK;
        }

        if (ipEncryption.isEncrypted(storedAddress)) {
            return Classification.ENCRYPTED;
        }

        if (isIpLiteral(storedAddress)) {
            return Classification.PLAINTEXT_IP;
        }

        return Classification.UNRECOGNIZED;
    }

    /**
     * Checks whether a string is an IPv4 or IPv6 literal, purely by syntax.
     * <p>
     * This is deliberately not implemented with {@link java.net.InetAddress#getByName(String)}:
     * that method falls through to a DNS lookup for anything it cannot parse as a literal, which
     * would turn a startup scan over the login table into a burst of name resolutions.
     *
     * The match is exact: surrounding whitespace makes a value unrecognized rather than plaintext,
     * so the migration reports it instead of storing the ciphertext of a padded string that no
     * later lookup would match.
     *
     * @param value the value to check; may be null
     * @return true if the value is a well-formed IP literal
     */
    public static boolean isIpLiteral(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        return IPV4.matcher(value).matches() || isIpv6Literal(value);
    }

    private static boolean isIpv6Literal(String value) {
        String address = value;

        // A scope/zone id (e.g. "fe80::1%eth0") is part of the literal but not of the address itself.
        int zoneIndex = address.indexOf('%');
        if (zoneIndex >= 0) {
            if (zoneIndex == 0 || zoneIndex == address.length() - 1) {
                return false;
            }
            address = address.substring(0, zoneIndex);
        }

        if (address.indexOf(':') < 0) {
            return false;
        }

        int compressionIndex = address.indexOf("::");
        boolean compressed = compressionIndex >= 0;
        if (compressed && address.indexOf("::", compressionIndex + 1) >= 0) {
            // "::" may appear at most once.
            return false;
        }

        String head = compressed ? address.substring(0, compressionIndex) : address;
        String tail = compressed ? address.substring(compressionIndex + 2) : "";

        // An embedded IPv4 suffix ("::ffff:192.168.1.1") is always the last group of the literal.
        int headGroups = countGroups(head, !compressed);
        int tailGroups = compressed ? countGroups(tail, true) : 0;
        if (headGroups < 0 || tailGroups < 0) {
            return false;
        }

        int totalGroups = headGroups + tailGroups;
        // "::" stands for at least one omitted group, so a compressed literal is always short of 8.
        return compressed ? totalGroups <= 7 : totalGroups == 8;
    }

    /**
     * Counts the 16-bit groups in one side of an IPv6 literal.
     *
     * @param part                the colon-separated side; may be empty
     * @param allowTrailingIpv4   whether the final group may be an embedded IPv4 literal (worth two groups)
     * @return the number of groups, or -1 if the side is malformed
     */
    private static int countGroups(String part, boolean allowTrailingIpv4) {
        if (part.isEmpty()) {
            return 0;
        }

        String[] tokens = part.split(":", -1);
        int groups = 0;
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            boolean last = i == tokens.length - 1;

            if (last && allowTrailingIpv4 && token.indexOf('.') >= 0) {
                if (!IPV4.matcher(token).matches()) {
                    return -1;
                }
                groups += 2;
                continue;
            }

            if (!IPV6_GROUP.matcher(token).matches()) {
                return -1;
            }
            groups++;
        }
        return groups;
    }
}
