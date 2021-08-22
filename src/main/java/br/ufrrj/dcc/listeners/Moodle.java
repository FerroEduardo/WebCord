package br.ufrrj.dcc.listeners;

import br.ufrrj.dcc.entity.GuildInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.Presence;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Moodle {

    final private JDA jda;
    private final EntityManagerFactory factory;
    private long timeMoodleOff;
    private MoodleStatus currentMoodleStatus = MoodleStatus.NONE;

    public Moodle(JDA jda, EntityManagerFactory factory) {
        this.factory = factory;
        this.timeMoodleOff = 0L;
        this.jda = jda;
        initScheduler();
    }

    private void initScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkMoodleStatus, 60, 10, TimeUnit.SECONDS);
    }

    public void checkMoodleStatus() {
        Presence presence = jda.getPresence();
        EntityManager manager = factory.createEntityManager();
        List<GuildInfo> guilds = manager.createQuery("SELECT g FROM GuildInfo AS g", GuildInfo.class).getResultList();
        manager.close();
        int timeoutSeconds = 10;
        try {
            String moodleUrl = "https://www.dcc.ufrrj.br/moodle/login/index.php";
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(moodleUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            System.out.printf("Tentando fazer a requisição para %s%n", moodleUrl);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Requisição concluída com sucesso");
            if (response.statusCode() == 200) {
                if (currentMoodleStatus != MoodleStatus.ONLINE) {
                    for (GuildInfo guildInfo : guilds) {
                        long guildId = guildInfo.getGuildId();
                        long guildChannelId = guildInfo.getGuildChannelId();
                        Guild dccGuild = Objects.requireNonNull(jda.getGuildById(guildId));
                        TextChannel textChannel = Objects.requireNonNull(dccGuild.getTextChannelById(guildChannelId));
                        textChannel.sendMessage(String.format("Moodle online. Status code: %d%n", response.statusCode())).queue();
                    }
                    currentMoodleStatus = MoodleStatus.ONLINE;
                }
                System.out.printf("Moodle online. Status code: %d%n", response.statusCode());
            } else {
                if (currentMoodleStatus != MoodleStatus.ERROR) {
                    for (GuildInfo guildInfo : guilds) {
                        long guildId = guildInfo.getGuildId();
                        long guildChannelId = guildInfo.getGuildChannelId();
                        Guild dccGuild = Objects.requireNonNull(jda.getGuildById(guildId));
                        TextChannel textChannel = Objects.requireNonNull(dccGuild.getTextChannelById(guildChannelId));
                        textChannel.sendMessage(String.format("Algo de errado aconteceu com o Moodle. Status code: %d%n", response.statusCode())).queue();
                    }
                    currentMoodleStatus = MoodleStatus.ERROR;
                }
                System.out.printf("Algo de errado aconteceu com o Moodle. Status code: %d%n", response.statusCode());
            }
        } catch (IllegalArgumentException e) {
            if (currentMoodleStatus != MoodleStatus.ERROR) {
                for (GuildInfo guildInfo : guilds) {
                    long guildId = guildInfo.getGuildId();
                    long guildChannelId = guildInfo.getGuildChannelId();
                    Guild dccGuild = Objects.requireNonNull(jda.getGuildById(guildId));
                    TextChannel textChannel = Objects.requireNonNull(dccGuild.getTextChannelById(guildChannelId));
                    textChannel.sendMessage("URI informada está incorreta").queue();
                }
                currentMoodleStatus = MoodleStatus.ERROR;
            }
            System.out.println("URI informada está incorreta");
        } catch (HttpConnectTimeoutException e) {
            if (currentMoodleStatus != MoodleStatus.TIMEOUT) {
                for (GuildInfo guildInfo : guilds) {
                    long guildId = guildInfo.getGuildId();
                    long guildChannelId = guildInfo.getGuildChannelId();
                    Guild dccGuild = Objects.requireNonNull(jda.getGuildById(guildId));
                    TextChannel textChannel = Objects.requireNonNull(dccGuild.getTextChannelById(guildChannelId));
                    textChannel.sendMessage(String.format("Timeout(%ds) ao tentar acessar o Moodle", timeoutSeconds)).queue();
                }
                currentMoodleStatus = MoodleStatus.TIMEOUT;
            }
            System.out.println("Timeout(10s) ao tentar acessar o Moodle");
        } catch (Exception e) {
            currentMoodleStatus = MoodleStatus.ERROR;
            e.printStackTrace();
        }

        presence.setStatus(currentMoodleStatus.onlineStatus);
    }

    enum MoodleStatus {
        ONLINE(OnlineStatus.ONLINE),
        TIMEOUT(OnlineStatus.DO_NOT_DISTURB),
        ERROR(OnlineStatus.DO_NOT_DISTURB),
        NONE(OnlineStatus.DO_NOT_DISTURB);

        private final OnlineStatus onlineStatus;
        MoodleStatus(OnlineStatus onlineStatus) {
            this.onlineStatus = onlineStatus;
        }
    }

}
