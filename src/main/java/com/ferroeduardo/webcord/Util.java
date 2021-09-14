package com.ferroeduardo.webcord;

import com.ferroeduardo.webcord.entity.GuildInfo;
import com.ferroeduardo.webcord.service.GuildInfoService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Util {

    private static final Logger LOGGER = LogManager.getLogger(Util.class);

    public static String getInviteLink(JDA jda) {
        long clientId = jda.getSelfUser().getIdLong();
        int permission = Permission.MESSAGE_WRITE.getOffset();
        return String.format("https://discord.com/api/oauth2/authorize?client_id=%d&permissions=%d", clientId, permission) + "&scope=bot%20applications.commands";
    }

    public static void checkDatabaseDataIntegrity(JDA jda, GuildInfoService guildInfoService) {
        List<GuildInfo> guilds = guildInfoService.findAll();
        Set<Long> rowsToRemove = new HashSet<>();
        guilds.parallelStream().forEach(guildInfo -> {
            long guildId = guildInfo.getGuildId();
            long channelId = guildInfo.getGuildChannelId();
            if (!checkChannelExistence(jda, guildId, channelId)) {
                LOGGER.debug(String.format("Canal '%d' não existe", channelId));
                rowsToRemove.add(guildInfo.getId());
            }
        });
        int removedRows = guildInfoService.delete(rowsToRemove);
        LOGGER.debug(String.format("%d canais/servidores inválidos removidos do banco de dados", removedRows));
    }

    private static boolean checkChannelExistence(JDA jda, long guildId, long channelId) {
        Optional<Guild> optionalGuild = checkGuildExistence(jda, guildId);
        if (optionalGuild.isPresent()) {
            Guild guild = optionalGuild.get();
            Optional<TextChannel> optionalTextChannel = Optional.ofNullable(guild.getTextChannelById(channelId));
            return optionalTextChannel.isPresent();
        }
        LOGGER.debug(String.format("Servidor '%d' não existe", guildId));
        return false;
    }

    private static Optional<Guild> checkGuildExistence(JDA jda, long guildId) {
        return Optional.ofNullable(jda.getGuildById(guildId));
    }

}
