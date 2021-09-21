package com.ferroeduardo.webcord.listener;

import com.ferroeduardo.webcord.entity.GuildInfo;
import com.ferroeduardo.webcord.entity.WebsiteRecord;
import com.ferroeduardo.webcord.service.GuildInfoService;
import com.ferroeduardo.webcord.service.WebsiteRecordService;
import net.dv8tion.jda.api.JDA;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private ZonedDateTime latestStatusTime;
    private final UpdatePresenceListener presenceListener;
    private final GuildInfoService guildInfoService;
    private WebsiteRecordService websiteRecordService;
    private final ZoneId zone;

    public WebObserver(JDA jda, GuildInfoService guildInfoService, int timeoutSeconds, int schedulerTimeRate, String websiteName, String url, UpdatePresenceListener presenceListener, WebsiteRecordService websiteRecordService, ZoneId zone) {
        this.jda = jda;
        this.guildInfoService = guildInfoService;
        this.timeoutSeconds = timeoutSeconds;
        this.schedulerTimeRate = schedulerTimeRate;
        this.websiteName = websiteName;
        this.url = url;
        this.presenceListener = presenceListener;
        this.zone = zone;
        this.timeoutCount = 0;
        this.currentWebsiteStatus = WebsiteStatus.NONE;
        this.websiteRecordService = websiteRecordService;
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

    public ZonedDateTime getLatestStatusTime() {
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
                    latestStatusTime = ZonedDateTime.now(zone);
                }
                recordWebsiteStatus(ZonedDateTime.now(zone), this.websiteName, WebsiteStatus.ONLINE);
                LOGGER.info(message);
            } else {
                String message = String.format("Algo de errado aconteceu com o %s. Status code: %d", websiteName, response.statusCode());
                if (currentWebsiteStatus != WebsiteStatus.ERROR) {
                    guilds.parallelStream().forEach(guild -> {
                        guild.sendMessage(jda, message);
                    });
                    currentWebsiteStatus = WebsiteStatus.ERROR;
                    latestStatusTime = ZonedDateTime.now(zone);
                }
                recordWebsiteStatus(ZonedDateTime.now(zone), this.websiteName, WebsiteStatus.ERROR);
                LOGGER.info(message.trim());
            }
        } catch (IllegalArgumentException e) {
            if (currentWebsiteStatus != WebsiteStatus.ERROR) {
                currentWebsiteStatus = WebsiteStatus.ERROR;
                latestStatusTime = ZonedDateTime.now(zone);
            }
            recordWebsiteStatus(ZonedDateTime.now(zone), this.websiteName, WebsiteStatus.ERROR);
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
                    latestStatusTime = ZonedDateTime.now(zone);
                }
            }
            recordWebsiteStatus(ZonedDateTime.now(zone), this.websiteName, WebsiteStatus.TIMEOUT);
            LOGGER.debug(message, e);
        } catch (Exception e) {
            String message = String.format("Ocorreu algo inesperado ao tentar acessar o %s", websiteName);
            LOGGER.error(message, e);
            guilds.parallelStream().forEach(guild -> {
                guild.sendMessage(jda, message);
            });
            currentWebsiteStatus = WebsiteStatus.ERROR;
            latestStatusTime = ZonedDateTime.now(zone);
            recordWebsiteStatus(latestStatusTime, this.websiteName, currentWebsiteStatus);
        }
        presenceListener.updatePresence();
    }

    private void recordWebsiteStatus(ZonedDateTime dateTime, String websiteName, WebsiteStatus websiteStatus) {
        if (websiteRecordService != null) {
            LOGGER.trace("Salvando registro do status");
            WebsiteRecord websiteRecord = new WebsiteRecord(null, dateTime, websiteName, websiteStatus);
            websiteRecordService.save(websiteRecord);
        }
    }

}
