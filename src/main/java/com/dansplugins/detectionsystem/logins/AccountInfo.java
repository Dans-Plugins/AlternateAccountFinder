package com.dansplugins.detectionsystem.logins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AccountInfo {
    private final UUID minecraftUuid;
    private final Map<InetAddress, Integer> logins;

    public AccountInfo(UUID minecraftUuid, Map<InetAddress, Integer> logins) {
        this.minecraftUuid = minecraftUuid;
        this.logins = logins;
    }

    public UUID getMinecraftUuid() {
        return minecraftUuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(minecraftUuid);
    }

    public int getLogins(InetAddress address) {
        return logins.getOrDefault(address, 0);
    }

    public List<InetAddress> getAddresses() {
        return logins.keySet().stream().toList();
    }
}
