package com.ferroeduardo.webcord;

import com.ferroeduardo.webcord.entity.GuildInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Util {

    private static final Logger LOGGER = LogManager.getLogger(Util.class);

    public static void checkDatabaseDataIntegrity(JDA jda, EntityManagerFactory factory) {
        EntityManager manager = factory.createEntityManager();
        List<GuildInfo> guilds = manager.createQuery("SELECT g FROM GuildInfo AS g", GuildInfo.class).getResultList();
        Set<Long> rowsToRemove = new HashSet<>();
        guilds.parallelStream().forEach(guildInfo -> {
            long guildId = guildInfo.getGuildId();
            long channelId = guildInfo.getGuildChannelId();
            if (!checkChannelExistence(jda, guildId, channelId)) {
                LOGGER.debug(String.format("Canal '%d' não existe", channelId));
                rowsToRemove.add(guildInfo.getId());
            }
        });
        manager.getTransaction().begin();
        Query deleteQuery = manager.createQuery("DELETE FROM GuildInfo g WHERE g.id in (?1)");
        deleteQuery.setParameter(1, rowsToRemove);
        int removedRows = deleteQuery.executeUpdate();
        manager.getTransaction().commit();
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
