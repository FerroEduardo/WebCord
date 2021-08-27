package br.ufrrj.dcc.listeners;

import br.ufrrj.dcc.entity.GuildInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.managers.Presence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private final EntityManagerFactory factory;
    private final String websiteName;
    private final String url;
    private WebsiteStatus currentWebsiteStatus;
    private int timeoutCount;
    private final int timeoutSeconds;
    private final int schedulerTimeRate;
    private LocalDateTime latestStatusTime;

    public WebObserver(JDA jda, EntityManagerFactory factory, int timeoutSeconds, int schedulerTimeRate, String websiteName, String url) {
        this.jda = jda;
        this.factory = factory;
        this.timeoutSeconds = timeoutSeconds;
        this.schedulerTimeRate = schedulerTimeRate;
        this.websiteName = websiteName;
        this.url = url;
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
        Presence presence = jda.getPresence();
        EntityManager manager = factory.createEntityManager();
        List<GuildInfo> guilds = manager.createQuery("SELECT g FROM GuildInfo AS g", GuildInfo.class).getResultList();
        manager.close();
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(this.url))
                    .GET()
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            LOGGER.info(String.format("Tentando fazer a requisição para %s", this.url));
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("Requisição concluída com sucesso");
            timeoutCount = 0; // reset count number
            if (response.statusCode() == 200) {
                if (currentWebsiteStatus != WebsiteStatus.ONLINE) {
                    guilds.parallelStream().forEach(guild -> {
                        String message = String.format("%s online. Status code: %d%n", websiteName, response.statusCode());
                        guild.sendMessage(jda, message);
                    });
                    currentWebsiteStatus = WebsiteStatus.ONLINE;
                    latestStatusTime = LocalDateTime.now(ZoneId.of("GMT-3"));
                }
                LOGGER.info(String.format("%s online. Status code: %d", websiteName, response.statusCode()));
            } else {
                if (currentWebsiteStatus != WebsiteStatus.ERROR) {
                    guilds.parallelStream().forEach(guild -> {
                        String message = String.format("Algo de errado aconteceu com o %s. Status code: %d%n", websiteName, response.statusCode());
                        guild.sendMessage(jda, message);
                    });
                    currentWebsiteStatus = WebsiteStatus.ERROR;
                    latestStatusTime = LocalDateTime.now(ZoneId.of("GMT-3"));
                }
                LOGGER.info(String.format("Algo de errado aconteceu com o %s. Status code: %d%n", websiteName, response.statusCode()));
            }
        } catch (IllegalArgumentException e) {
            if (currentWebsiteStatus != WebsiteStatus.ERROR) {
//                guilds.parallelStream().forEach(guild -> {
//                    String message = "URI informada está incorreta";
//                    guild.sendMessage(jda, message);
//                });
                currentWebsiteStatus = WebsiteStatus.ERROR;
                latestStatusTime = LocalDateTime.now(ZoneId.of("GMT-3"));
            }
            LOGGER.trace(e);
            LOGGER.info(String.format("URI do %s está incorreta%n", websiteName));
        } catch (HttpConnectTimeoutException e) {
            timeoutCount++; // increment count number
            if (timeoutCount % 3 == 0) {
                if (currentWebsiteStatus != WebsiteStatus.TIMEOUT) {
                    guilds.parallelStream().forEach(guild -> {
                        String message = String.format("Timeout (%ds) ao tentar acessar o %s%n", timeoutSeconds, websiteName);
                        guild.sendMessage(jda, message);
                    });
                    currentWebsiteStatus = WebsiteStatus.TIMEOUT;
                    latestStatusTime = LocalDateTime.now(ZoneId.of("GMT-3"));
                }
            }
            LOGGER.trace(e);
            LOGGER.info(String.format("Timeout (%ds) ao tentar acessar o %s%n", timeoutSeconds, websiteName));
        } catch (Exception e) {
            LOGGER.trace(e);
            currentWebsiteStatus = WebsiteStatus.ERROR;
            latestStatusTime = LocalDateTime.now(ZoneId.of("GMT-3"));
        }
        presence.setStatus(currentWebsiteStatus.status);
    }

}
