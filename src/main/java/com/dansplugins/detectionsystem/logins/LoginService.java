package com.dansplugins.detectionsystem.logins;

import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public final class LoginService {

    private final LoginRepository repo;

    public LoginService(LoginRepository repo) {
        this.repo = repo;
    }

    public AddressAccountInfo getAddressInfo(InetAddress ip) {
        return repo.getAddressInfo(ip);
    }

    public AccountAddressInfo getAccountInfo(UUID minecraftUuid) {
        return repo.getAccountInfo(minecraftUuid);
    }

    public List<UUID> getPotentialAlts(UUID minecraftUuid) {
        return repo.getPotentialAlts(minecraftUuid);
    }

    public int getLoginCount(UUID minecraftUuid, InetAddress ip) {
        return repo.getLoginCount(minecraftUuid, ip);
    }

    public void saveLogin(Player player) {
        repo.saveLogin(player);
    }
}
