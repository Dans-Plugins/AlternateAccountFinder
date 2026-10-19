package com.dansplugins.detectionsystem.logins;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AddressAccountInfoTest {

    @Test
    void returnsTheAddressItWasBuiltFor() throws UnknownHostException {
        InetAddress ip = InetAddress.getByName("192.168.1.1");

        AddressAccountInfo info = new AddressAccountInfo(ip, Map.of());

        assertEquals(ip, info.getIP());
    }

    @Test
    void listsAccountsInTheOrderOfTheMapItWasGiven() throws UnknownHostException {
        // LoginRepository#getAddressInfo hands over a LinkedHashMap ordered most recently seen
        // first, and /aaf accounts prints getAccounts() as-is, so the order must survive.
        // UUIDs whose natural order is the reverse of the insertion order make a sorted or
        // hash-ordered result observable.
        UUID newest = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UUID middle = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID oldest = UUID.fromString("00000000-0000-0000-0000-000000000001");
        LocalDateTime now = LocalDateTime.of(2026, 10, 19, 12, 0);
        Map<UUID, AccountInfo> accounts = new LinkedHashMap<>();
        accounts.put(newest, new AccountInfo(1, now, now));
        accounts.put(middle, new AccountInfo(2, now.minusDays(2), now.minusDays(1)));
        accounts.put(oldest, new AccountInfo(3, now.minusDays(4), now.minusDays(3)));

        AddressAccountInfo info = new AddressAccountInfo(InetAddress.getByName("192.168.1.1"), accounts);

        assertEquals(List.of(newest, middle, oldest), info.getAccounts());
    }

    @Test
    void returnsTheAccountInfoRecordedForAnAccount() throws UnknownHostException {
        UUID account = UUID.randomUUID();
        AccountInfo accountInfo = new AccountInfo(5,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0));

        AddressAccountInfo info = new AddressAccountInfo(InetAddress.getByName("2001:db8::1"),
                Map.of(account, accountInfo));

        assertSame(accountInfo, info.getAccountInfo(account));
    }

    @Test
    void returnsNullForAnAccountNotOnRecord() throws UnknownHostException {
        AddressAccountInfo info = new AddressAccountInfo(InetAddress.getByName("192.168.1.1"),
                Map.of(UUID.randomUUID(), new AccountInfo(1, LocalDateTime.now(), LocalDateTime.now())));

        assertNull(info.getAccountInfo(UUID.randomUUID()));
    }

    @Test
    void hasNoAccountsWhenNothingWasRecorded() throws UnknownHostException {
        AddressAccountInfo info = new AddressAccountInfo(InetAddress.getByName("192.168.1.1"), Map.of());

        assertEquals(List.of(), info.getAccounts());
    }
}
