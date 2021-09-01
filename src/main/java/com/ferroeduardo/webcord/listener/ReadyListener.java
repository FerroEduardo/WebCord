package com.ferroeduardo.webcord.listener;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ReadyListener implements EventListener {

    private static final Logger LOGGER = LogManager.getLogger(ReadyListener.class);

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof ReadyEvent) {
            long clientId = event.getJDA().getSelfUser().getIdLong();
            int permission = 2048;
            String link = String.format("https://discord.com/api/oauth2/authorize?client_id=%d&permissions=%d", clientId, permission) + "&scope=bot%20applications.commands";
            LOGGER.info("API is ready! Invite link: " + link);
        }
    }

}
