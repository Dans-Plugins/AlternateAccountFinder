package com.dansplugins.detectionsystem.logins;

import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AddressInfo {

    private final InetAddress ip;
    private final Map<UUID, Integer> logins;

    public AddressInfo(
            InetAddress ip,
            Map<UUID, Integer> logins
    ) {
        this.ip = ip;
        this.logins = logins;
    }

    public AddressInfo(Player player) {
        this(
                player.getAddress().getAddress(),
                Map.of(player.getUniqueId(), 1)
        );
    }

    public InetAddress getIP() {
        return ip;
    }

    public List<UUID> getAccounts() {
        return logins.keySet().stream().toList();
    }

    public int getLogins(UUID uuid) {
        return logins.getOrDefault(uuid, 0);
    }

}
