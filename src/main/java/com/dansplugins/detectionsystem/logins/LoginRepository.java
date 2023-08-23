package com.dansplugins.detectionsystem.logins;

import static com.dansplugins.detectionsystem.jooq.Tables.AAF_LOGIN_RECORD;
import static java.time.ZoneOffset.UTC;

import com.dansplugins.detectionsystem.jooq.tables.AafLoginRecord;
import com.dansplugins.detectionsystem.jooq.tables.records.AafLoginRecordRecord;
import org.bukkit.entity.Player;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class LoginRepository {

    private final DSLContext dsl;

    public LoginRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public AddressInfo getAddressInfo(InetAddress ip) {
        Result<AafLoginRecordRecord> result = dsl.selectFrom(AAF_LOGIN_RECORD)
                .where(AAF_LOGIN_RECORD.ADDRESS.eq(ip.getHostAddress()))
                .fetch();

        return new AddressInfo(
                ip,
                result.stream()
                        .collect(
                                Collectors.toMap(
                                        record -> UUID.fromString(record.getMinecraftUuid()),
                                        AafLoginRecordRecord::getLogins
                                )
                        )
        );
    }

    public AccountInfo getAccountInfo(UUID minecraftUuid) {
        Result<AafLoginRecordRecord> result = dsl.selectFrom(AAF_LOGIN_RECORD)
                .where(AAF_LOGIN_RECORD.MINECRAFT_UUID.eq(minecraftUuid.toString()))
                .fetch();

        return new AccountInfo(
                minecraftUuid,
                result.stream().collect(
                        Collectors.toMap(
                                record -> {
                                    try {
                                        return InetAddress.getByName(record.getAddress());
                                    } catch (UnknownHostException exception) {
                                        throw new RuntimeException(exception);
                                    }
                                },
                                AafLoginRecordRecord::getLogins
                        )
                )
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
        return dsl.selectFrom(AAF_LOGIN_RECORD)
                .where(AAF_LOGIN_RECORD.MINECRAFT_UUID.eq(minecraftUuid.toString()))
                .and(AAF_LOGIN_RECORD.ADDRESS.eq(ip.getHostAddress()))
                .fetchOne(AAF_LOGIN_RECORD.LOGINS);
    }

    public void saveLogin(Player player) {
        saveLogin(player.getUniqueId(), player.getAddress().getAddress());
    }

    private void saveLogin(UUID minecraftUuid, InetAddress address) {
        dsl.insertInto(AAF_LOGIN_RECORD)
                .set(AAF_LOGIN_RECORD.MINECRAFT_UUID, minecraftUuid.toString())
                .set(AAF_LOGIN_RECORD.ADDRESS, address.getHostAddress())
                .set(AAF_LOGIN_RECORD.LOGINS, 1)
                .set(AAF_LOGIN_RECORD.FIRST_LOGIN, LocalDateTime.now(UTC))
                .set(AAF_LOGIN_RECORD.LAST_LOGIN, LocalDateTime.now(UTC))
                .onConflict(AAF_LOGIN_RECORD.MINECRAFT_UUID, AAF_LOGIN_RECORD.ADDRESS).doUpdate()
                .set(AAF_LOGIN_RECORD.LOGINS, AAF_LOGIN_RECORD.LOGINS.plus(1))
                .set(AAF_LOGIN_RECORD.LAST_LOGIN, LocalDateTime.now(UTC))
                .execute();
    }
}
