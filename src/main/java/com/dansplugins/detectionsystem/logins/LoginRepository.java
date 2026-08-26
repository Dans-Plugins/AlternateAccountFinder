package com.dansplugins.detectionsystem.logins;

import static com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.max;

import com.dansplugins.detectionsystem.encryption.IpEncryption;
import com.dansplugins.detectionsystem.jooq.tables.AafLoginRecord;
import com.dansplugins.detectionsystem.jooq.tables.records.AafLoginRecordRecord;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Result;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class LoginRepository {

    private final DSLContext dsl;
    private final IpEncryption ipEncryption;

    public LoginRepository(DSLContext dsl, IpEncryption ipEncryption) {
        this.dsl = dsl;
        this.ipEncryption = ipEncryption;
    }

    /**
     * Returns every account that has logged in from {@code ip}, most recently seen first.
     *
     * <p>The order is what a moderator reads the list in, so it is defined here rather than left to
     * the database: rows come back ordered by last login descending, with the account UUID breaking
     * ties so two accounts last seen at the same moment do not swap places between runs. The
     * collector has to preserve that order too — {@code Collectors.toMap} defaults to a
     * {@link java.util.HashMap}, which would put the list back into hash order (see issue #104).
     *
     * <p>The address is matched as ciphertext, which works because {@link IpEncryption} is
     * deterministic.
     */
    public AddressAccountInfo getAddressInfo(InetAddress ip) {
        String encryptedAddress = ipEncryption.encrypt(ip.getHostAddress());
        Result<AafLoginRecordRecord> result = dsl.selectFrom(AAF_LOGIN_RECORD)
                .where(AAF_LOGIN_RECORD.ADDRESS.eq(encryptedAddress))
                .orderBy(AAF_LOGIN_RECORD.LAST_LOGIN.desc(), AAF_LOGIN_RECORD.MINECRAFT_UUID.asc())
                .fetch();

        return new AddressAccountInfo(
                ip,
                result.stream()
                        .collect(
                                Collectors.toMap(
                                        record -> UUID.fromString(record.getMinecraftUuid()),
                                        record -> new AccountInfo(
                                                record.getLogins(),
                                                record.getFirstLogin(),
                                                record.getLastLogin()
                                        ),
                                        // (address, minecraft_uuid) is the PK, so one account
                                        // cannot appear twice for one address; keep the first row
                                        // rather than letting toMap throw if that is ever violated.
                                        (existing, replacement) -> existing,
                                        LinkedHashMap::new
                                )
                        )
        );
    }

    public AccountAddressInfo getAccountInfo(UUID minecraftUuid) {
        Result<AafLoginRecordRecord> result = dsl.selectFrom(AAF_LOGIN_RECORD)
                .where(AAF_LOGIN_RECORD.MINECRAFT_UUID.eq(minecraftUuid.toString()))
                .fetch();

        return new AccountAddressInfo(
                minecraftUuid,
                result.stream()
                        .map(record -> {
                            try {
                                String decryptedAddress = ipEncryption.decrypt(record.getAddress());
                                InetAddress address = InetAddress.getByName(decryptedAddress);
                                AddressInfo info = new AddressInfo(
                                        record.getLogins(),
                                        record.getFirstLogin(),
                                        record.getLastLogin()
                                );
                                return new AbstractMap.SimpleEntry<>(address, info);
                            } catch (UnknownHostException exception) {
                                throw new RuntimeException("Invalid IP address in database", exception);
                            } catch (RuntimeException exception) {
                                throw new RuntimeException("Failed to decrypt IP for UUID " + minecraftUuid + ": " + exception.getMessage(), exception);
                            }
                        })
                        // (minecraft_uuid, address) is the PK so duplicates shouldn't be possible,
                        // but use a merge function rather than letting toMap throw if the invariant
                        // is ever violated (e.g. corrupted data).
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (existing, replacement) -> existing
                        ))
        );
    }

    /**
     * Returns each other account that has logged in from at least one of this account's addresses,
     * most recently seen first.
     *
     * <p>The join produces one row per shared address, so an account sharing several addresses
     * would otherwise be listed once per address in command output and join notifications. Grouping
     * by the candidate account collapses those rows to one, as the earlier {@code SELECT DISTINCT}
     * did, and additionally yields the timestamp the ordering needs: {@code SELECT DISTINCT} cannot
     * be ordered by a column outside its select list, so the grouped form is what expresses
     * "newest first" here (see issues #80 and #104).
     *
     * <p>The timestamp ordered on is the newest login among the rows that matched — that is, the
     * last time the candidate was seen on an address it shares with {@code minecraftUuid}, rather
     * than the last time it was seen at all. The account UUID breaks ties.
     */
    public List<UUID> getPotentialAlts(UUID minecraftUuid) {
        AafLoginRecord record1 = AAF_LOGIN_RECORD.as("record1");
        AafLoginRecord record2 = AAF_LOGIN_RECORD.as("record2");
        Field<LocalDateTime> lastSharedLogin = max(record2.LAST_LOGIN);
        Result<Record2<String, LocalDateTime>> result = dsl
                .select(record2.MINECRAFT_UUID, lastSharedLogin)
                .from(
                    record1,
                    record2
                )
                .where(record1.MINECRAFT_UUID.eq(minecraftUuid.toString()))
                .and(record1.MINECRAFT_UUID.ne(record2.MINECRAFT_UUID))
                .and(record1.ADDRESS.eq(record2.ADDRESS))
                .groupBy(record2.MINECRAFT_UUID)
                .orderBy(lastSharedLogin.desc(), record2.MINECRAFT_UUID.asc())
                .fetch();

        return result.stream()
                .map(record -> UUID.fromString(record.get(record2.MINECRAFT_UUID)))
                .toList();
    }

    public int getLoginCount(UUID minecraftUuid, InetAddress ip) {
        String encryptedAddress = ipEncryption.encrypt(ip.getHostAddress());
        Integer count = dsl.selectFrom(AAF_LOGIN_RECORD)
                .where(AAF_LOGIN_RECORD.MINECRAFT_UUID.eq(minecraftUuid.toString()))
                .and(AAF_LOGIN_RECORD.ADDRESS.eq(encryptedAddress))
                .fetchOne(AAF_LOGIN_RECORD.LOGINS);
        return count != null ? count : 0;
    }

    public void saveLogin(UUID minecraftUuid, InetAddress address) {
        String encryptedAddress = ipEncryption.encrypt(address.getHostAddress());
        dsl.insertInto(AAF_LOGIN_RECORD)
                .set(AAF_LOGIN_RECORD.MINECRAFT_UUID, minecraftUuid.toString())
                .set(AAF_LOGIN_RECORD.ADDRESS, encryptedAddress)
                .set(AAF_LOGIN_RECORD.LOGINS, 1)
                .set(AAF_LOGIN_RECORD.FIRST_LOGIN, LocalDateTime.now(UTC))
                .set(AAF_LOGIN_RECORD.LAST_LOGIN, LocalDateTime.now(UTC))
                .onConflict(AAF_LOGIN_RECORD.MINECRAFT_UUID, AAF_LOGIN_RECORD.ADDRESS).doUpdate()
                .set(AAF_LOGIN_RECORD.LOGINS, AAF_LOGIN_RECORD.LOGINS.plus(1))
                .set(AAF_LOGIN_RECORD.LAST_LOGIN, LocalDateTime.now(UTC))
                .execute();
    }
}
