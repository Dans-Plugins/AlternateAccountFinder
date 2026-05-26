package com.dansplugins.detectionsystem.logins;

import static com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD;
import static java.time.ZoneOffset.UTC;

import com.dansplugins.detectionsystem.encryption.IpEncryption;
import com.dansplugins.detectionsystem.jooq.tables.AafLoginRecord;
import com.dansplugins.detectionsystem.jooq.tables.records.AafLoginRecordRecord;
import org.bukkit.entity.Player;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.AbstractMap;
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

    public AddressAccountInfo getAddressInfo(InetAddress ip) {
        String encryptedAddress = ipEncryption.encrypt(ip.getHostAddress());
        Result<AafLoginRecordRecord> result = dsl.selectFrom(AAF_LOGIN_RECORD)
                .where(AAF_LOGIN_RECORD.ADDRESS.eq(encryptedAddress))
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
                                        )
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

    public List<UUID> getPotentialAlts(UUID minecraftUuid) {
        AafLoginRecord record1 = AAF_LOGIN_RECORD.as("record1");
        AafLoginRecord record2 = AAF_LOGIN_RECORD.as("record2");
        Result<Record1<String>> result = dsl
                .select(record2.MINECRAFT_UUID)
                .from(
                    record1,
                    record2
                )
                .where(record1.MINECRAFT_UUID.eq(minecraftUuid.toString()))
                .and(record1.MINECRAFT_UUID.ne(record2.MINECRAFT_UUID))
                .and(record1.ADDRESS.eq(record2.ADDRESS))
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

    public void saveLogin(Player player) {
        saveLogin(player.getUniqueId(), player.getAddress().getAddress());
    }

    private void saveLogin(UUID minecraftUuid, InetAddress address) {
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
