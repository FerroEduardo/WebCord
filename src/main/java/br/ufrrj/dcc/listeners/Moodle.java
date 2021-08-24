package br.ufrrj.dcc.listeners;

import br.ufrrj.dcc.entity.GuildInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
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
    private MoodleStatus currentMoodleStatus = MoodleStatus.NONE;
    private int timeoutCount;
    private final int timeoutSeconds;
    private final int schedulerTimeRate;

    public Moodle(JDA jda, EntityManagerFactory factory, int timeoutSeconds, int schedulerTimeRate) {
        this.factory = factory;
        this.timeoutCount = 0;
        this.jda = jda;
        this.schedulerTimeRate = schedulerTimeRate;
        this.timeoutSeconds = timeoutSeconds;
        initScheduler();
    }

    private void initScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkMoodleStatus, schedulerTimeRate, 10, TimeUnit.SECONDS);
    }

    public void checkMoodleStatus() {
        Presence presence = jda.getPresence();
        EntityManager manager = factory.createEntityManager();
        List<GuildInfo> guilds = manager.createQuery("SELECT g FROM GuildInfo AS g", GuildInfo.class).getResultList();
        manager.close();
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
                    guilds.parallelStream().forEach(guild -> {
                        long guildId = guild.getGuildId();
                        long guildChannelId = guild.getGuildChannelId();
                        Guild dccGuild = Objects.requireNonNull(jda.getGuildById(guildId));
                        TextChannel textChannel = Objects.requireNonNull(dccGuild.getTextChannelById(guildChannelId));
                        textChannel.sendMessage(String.format("Moodle online. Status code: %d%n", response.statusCode())).queue();
                    });
                    currentMoodleStatus = MoodleStatus.ONLINE;
                }
                timeoutCount = 0; // reset count number
                System.out.printf("Moodle online. Status code: %d%n", response.statusCode());
            } else {
                if (currentMoodleStatus != MoodleStatus.ERROR) {
                    guilds.parallelStream().forEach(guild -> {
                        long guildId = guild.getGuildId();
                        long guildChannelId = guild.getGuildChannelId();
                        Guild dccGuild = Objects.requireNonNull(jda.getGuildById(guildId));
                        TextChannel textChannel = Objects.requireNonNull(dccGuild.getTextChannelById(guildChannelId));
                        textChannel.sendMessage(String.format("Algo de errado aconteceu com o Moodle. Status code: %d%n", response.statusCode())).queue();
                    });
                    currentMoodleStatus = MoodleStatus.ERROR;
                }
                System.out.printf("Algo de errado aconteceu com o Moodle. Status code: %d%n", response.statusCode());
            }
        } catch (IllegalArgumentException e) {
            if (currentMoodleStatus != MoodleStatus.ERROR) {
                guilds.parallelStream().forEach(guild -> {
                    long guildId = guild.getGuildId();
                    long guildChannelId = guild.getGuildChannelId();
                    Guild dccGuild = Objects.requireNonNull(jda.getGuildById(guildId));
                    TextChannel textChannel = Objects.requireNonNull(dccGuild.getTextChannelById(guildChannelId));
                    textChannel.sendMessage("URI informada está incorreta").queue();
                });
                currentMoodleStatus = MoodleStatus.ERROR;
            }
            System.out.println("URI informada está incorreta");
        } catch (HttpConnectTimeoutException e) {
            if (currentMoodleStatus != MoodleStatus.TIMEOUT) {
                if ((timeoutCount == 3) || ((timeoutCount % 3) == 0)) {
                    guilds.parallelStream().forEach(guild -> {
                        long guildId = guild.getGuildId();
                        long guildChannelId = guild.getGuildChannelId();
                        Guild dccGuild = Objects.requireNonNull(jda.getGuildById(guildId));
                        TextChannel textChannel = Objects.requireNonNull(dccGuild.getTextChannelById(guildChannelId));
                        textChannel.sendMessage(String.format("Timeout (%ds) ao tentar acessar o Moodle", timeoutSeconds)).queue();
                    });
                    currentMoodleStatus = MoodleStatus.TIMEOUT;
                }
            }
            timeoutCount++; // increment count number
            System.out.printf("Timeout #%s(%ds) ao tentar acessar o Moodle%n", timeoutCount, timeoutSeconds);
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
