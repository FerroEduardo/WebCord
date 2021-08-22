package br.ufrrj.dcc.entity;

import javax.persistence.*;

@Entity
@Table(name = "guilds")
public class GuildInfo {

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

    @Override
    public String toString() {
        return "Guild{" +
                "id=" + id +
                ", guildId=" + guildId +
                ", guildChannelId=" + guildChannelId +
                '}';
    }
}
