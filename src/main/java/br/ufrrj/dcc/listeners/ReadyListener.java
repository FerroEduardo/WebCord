package br.ufrrj.dcc.listeners;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

public class ReadyListener implements EventListener {

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof ReadyEvent) {
            long clientId = event.getJDA().getSelfUser().getIdLong();
            int permission = 2048;
            String link = String.format("https://discord.com/api/oauth2/authorize?client_id=%d&permissions=%d", clientId, permission) + "&scope=bot%20applications.commands";
            System.out.println("API is ready! Invite link: " + link);
        }
    }

}
