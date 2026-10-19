package com.dansplugins.detectionsystem.logins;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AddressAccountInfo {

    private final InetAddress ip;
    private final Map<UUID, AccountInfo> logins;

    public AddressAccountInfo(
            InetAddress ip,
            Map<UUID, AccountInfo> accountInfo
    ) {
        this.ip = ip;
        this.logins = accountInfo;
    }

    public InetAddress getIP() {
        return ip;
    }

    public List<UUID> getAccounts() {
        return logins.keySet().stream().toList();
    }

    public AccountInfo getAccountInfo(UUID uuid) {
        return logins.get(uuid);
    }

}
