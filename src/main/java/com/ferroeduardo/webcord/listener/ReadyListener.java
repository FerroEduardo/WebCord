package com.ferroeduardo.webcord.listener;

import com.ferroeduardo.webcord.Util;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

public class ReadyListener implements EventListener {

    private static final Logger LOGGER = LogManager.getLogger(ReadyListener.class);

    @Override
    public void onEvent(@Nonnull GenericEvent event) {
        if (event instanceof ReadyEvent) {
            LOGGER.info("API is ready! Invite link: " + Util.getInviteLink(event.getJDA()));
        }
    }

}
