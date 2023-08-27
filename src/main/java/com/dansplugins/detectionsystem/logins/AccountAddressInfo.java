package com.dansplugins.detectionsystem.logins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AccountAddressInfo {
    private final UUID minecraftUuid;
    private final Map<InetAddress, AddressInfo> addressInfo;

    public AccountAddressInfo(UUID minecraftUuid, Map<InetAddress, AddressInfo> addressInfo) {
        this.minecraftUuid = minecraftUuid;
        this.addressInfo = addressInfo;
    }

    public UUID getMinecraftUuid() {
        return minecraftUuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(minecraftUuid);
    }

    public AddressInfo getAddressInfo(InetAddress address) {
        return addressInfo.get(address);
    }

    public List<InetAddress> getAddresses() {
        return addressInfo.keySet().stream().toList();
    }
}
