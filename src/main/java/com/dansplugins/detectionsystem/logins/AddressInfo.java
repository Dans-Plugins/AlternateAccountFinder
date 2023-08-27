package com.dansplugins.detectionsystem.logins;

import java.time.LocalDateTime;

public final class AddressInfo {

    private final int logins;
    private final LocalDateTime firstLogin;
    private final LocalDateTime lastLogin;

    public AddressInfo(int logins, LocalDateTime firstLogin, LocalDateTime lastLogin) {
        this.logins = logins;
        this.firstLogin = firstLogin;
        this.lastLogin = lastLogin;
    }

    public int getLogins() {
        return logins;
    }

    public LocalDateTime getFirstLogin() {
        return firstLogin;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

}
