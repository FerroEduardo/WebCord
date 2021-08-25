package br.ufrrj.dcc.listeners;

import net.dv8tion.jda.api.OnlineStatus;

enum WebsiteStatus {
    ONLINE(OnlineStatus.ONLINE),
    TIMEOUT(OnlineStatus.DO_NOT_DISTURB),
    ERROR(OnlineStatus.DO_NOT_DISTURB),
    NONE(OnlineStatus.DO_NOT_DISTURB);
    public final OnlineStatus status;
    WebsiteStatus(OnlineStatus status) {
        this.status = status;
    }
}
