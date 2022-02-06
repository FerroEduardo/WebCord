package com.ferroeduardo.webcord.entity;

import com.ferroeduardo.webcord.exception.GuildNotFoundException;
import com.ferroeduardo.webcord.exception.TextChannelNotFoundExists;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;
import java.util.Optional;

@Entity
@Table(name = "guilds")
public class GuildInfo {

    private static final Logger LOGGER = LogManager.getLogger(GuildInfo.class);

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private long id;

    @Column(nullable = false, name = "guild_id")
    private long guildId;

    @Column(nullable = false, name = "guild_channel_id")
    private long guildChannelId;

    public GuildInfo() {
    }

    public GuildInfo(long guildId, long guildChannelId) {
        this.guildId = guildId;
        this.guildChannelId = guildChannelId;
    }

    public GuildInfo(long id, long guildId, long guildChannelId) {
        this.id = id;
        this.guildId = guildId;
        this.guildChannelId = guildChannelId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGuildId() {
        return guildId;
    }

    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }

    public long getGuildChannelId() {
        return guildChannelId;
    }

    public void setGuildChannelId(long guildChannelId) {
        this.guildChannelId = guildChannelId;
    }

    public void sendMessage(JDA jda, String message) {
        try {
            Optional<Guild> optionalGuild = Optional.ofNullable(jda.getGuildById(guildId));
            Optional<TextChannel> textChannel = Optional.ofNullable(optionalGuild.orElseThrow(() ->
                    new GuildNotFoundException(String.format("Guild '%d' not found", guildId))
            ).getTextChannelById(guildChannelId));
            textChannel.orElseThrow(() ->
                    new TextChannelNotFoundExists(String.format("TextChannel '%d' not found in Guild '%d'", guildChannelId, guildId))
            ).sendMessage(message).queue();
        } catch (GuildNotFoundException e) {
            LOGGER.debug(String.format("Failed to send message to Guild '%d' because this Guild does not exists anymore%n", guildId), e);
        } catch (TextChannelNotFoundExists e) {
            LOGGER.debug(String.format("Failed to send message to Guild '%d' and TextChannel '%d' because this TextChannel does not exists anymore%n", guildId, guildChannelId), e);
        }
    }

    @Override
    public String toString() {
        return "Guild{" +
                "id=" + id +
                ", guildId=" + guildId +
                ", guildChannelId=" + guildChannelId +
                '}';
    }
}
