package com.ferroeduardo.webcord.listener;

import com.ferroeduardo.webcord.entity.GuildInfo;
import com.ferroeduardo.webcord.service.GuildInfoService;
import net.dv8tion.jda.api.JDA;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebObserver {

    private static final Logger LOGGER = LogManager.getLogger(WebObserver.class);

    private final JDA jda;
    private final String websiteName;
    private final String url;
    private WebsiteStatus currentWebsiteStatus;
    private int timeoutCount;
    private final int timeoutSeconds;
    private final int schedulerTimeRate;
    private LocalDateTime latestStatusTime;
    private final UpdatePresenceListener presenceListener;
    private final GuildInfoService guildInfoService;

    public WebObserver(JDA jda, GuildInfoService guildInfoService, int timeoutSeconds, int schedulerTimeRate, String websiteName, String url, UpdatePresenceListener presenceListener) {
        this.jda = jda;
        this.guildInfoService = guildInfoService;
        this.timeoutSeconds = timeoutSeconds;
        this.schedulerTimeRate = schedulerTimeRate;
        this.websiteName = websiteName;
        this.url = url;
        this.presenceListener = presenceListener;
        this.timeoutCount = 0;
        this.currentWebsiteStatus = WebsiteStatus.NONE;
        LOGGER.info(String.format("WebObserver '%s' inicializado", websiteName));
        initScheduler();
    }

    private void initScheduler() {
        LOGGER.info(String.format("Iniciando 'ScheduledExecutorService' do WebObserver '%s'", websiteName));
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkWebsiteStatus, 10, schedulerTimeRate, TimeUnit.SECONDS);
    }

    public WebsiteStatus getCurrentWebsiteStatus() {
        return currentWebsiteStatus;
    }

    public LocalDateTime getLatestStatusTime() {
        return latestStatusTime;
    }

    public int getTimeoutCount() {
        return timeoutCount;
    }

    private void checkWebsiteStatus() {
        List<GuildInfo> guilds = guildInfoService.findAll();
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(this.url))
                    .GET()
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            LOGGER.trace(String.format("Tentando fazer a requisição para %s", this.url));
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.trace(String.format("Requisição para %s foi concluída com sucesso", this.url));
            timeoutCount = 0;
            if (response.statusCode() == 200) {
                String message = String.format("%s online. Status code: %d", websiteName, response.statusCode());
                if (currentWebsiteStatus != WebsiteStatus.ONLINE) {
                    guilds.parallelStream().forEach(guild -> {
                        guild.sendMessage(jda, message);
                    });
                    currentWebsiteStatus = WebsiteStatus.ONLINE;
                    latestStatusTime = LocalDateTime.now(ZoneId.of("GMT-3"));
                }
                LOGGER.info(message);
            } else {
                String message = String.format("Algo de errado aconteceu com o %s. Status code: %d", websiteName, response.statusCode());
                if (currentWebsiteStatus != WebsiteStatus.ERROR) {
                    guilds.parallelStream().forEach(guild -> {
                        guild.sendMessage(jda, message);
                    });
                    currentWebsiteStatus = WebsiteStatus.ERROR;
                    latestStatusTime = LocalDateTime.now(ZoneId.of("GMT-3"));
                }
                LOGGER.info(message.trim());
            }
        } catch (IllegalArgumentException e) {
            if (currentWebsiteStatus != WebsiteStatus.ERROR) {
                currentWebsiteStatus = WebsiteStatus.ERROR;
                latestStatusTime = LocalDateTime.now(ZoneId.of("GMT-3"));
            }
            LOGGER.error(String.format("URI do %s está incorreta", websiteName), e);
        } catch (HttpTimeoutException e) {
            timeoutCount++;
            int neededAttemptsToDetectTimeout = 3;
            String message = String.format("Timeout (%ds) após %d tentativa(s) de acessar o %s", timeoutSeconds, timeoutCount, websiteName);
            if (timeoutCount % neededAttemptsToDetectTimeout == 0) {
                if (currentWebsiteStatus != WebsiteStatus.TIMEOUT) {
                    guilds.parallelStream().forEach(guild -> {
                        guild.sendMessage(jda, message);
                    });
                    currentWebsiteStatus = WebsiteStatus.TIMEOUT;
                    latestStatusTime = LocalDateTime.now(ZoneId.of("GMT-3"));
                }
            }
            LOGGER.debug(message, e);
        } catch (Exception e) {
            String message = String.format("Ocorreu algo inesperado ao tentar acessar o %s", websiteName);
            LOGGER.error(message, e);
            guilds.parallelStream().forEach(guild -> {
                guild.sendMessage(jda, message);
            });
            currentWebsiteStatus = WebsiteStatus.ERROR;
            latestStatusTime = LocalDateTime.now(ZoneId.of("GMT-3"));
        }
        presenceListener.updatePresence();
    }

}
