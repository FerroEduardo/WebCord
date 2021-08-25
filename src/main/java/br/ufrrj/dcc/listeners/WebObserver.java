package br.ufrrj.dcc.listeners;

import br.ufrrj.dcc.entity.GuildInfo;
import net.dv8tion.jda.api.JDA;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebObserver {

    private final JDA jda;
    private final EntityManagerFactory factory;
    private final String websiteName;
    private final String url;
    private WebsiteStatus currentWebsiteStatus = WebsiteStatus.NONE;
    private int timeoutCount;
    private final int timeoutSeconds;
    private final int schedulerTimeRate;

    public WebObserver(JDA jda, EntityManagerFactory factory, int timeoutSeconds, int schedulerTimeRate, String websiteName, String url) {
        this.jda = jda;
        this.factory = factory;
        this.timeoutSeconds = timeoutSeconds;
        this.schedulerTimeRate = schedulerTimeRate;
        this.websiteName = websiteName;
        this.url = url;
        this.timeoutCount = 0;
        initScheduler();
    }

    private void initScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkWebsiteStatus, schedulerTimeRate, 10, TimeUnit.SECONDS);
    }

    public WebsiteStatus getCurrentWebsiteStatus() {
        return currentWebsiteStatus;
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

            System.out.printf("Tentando fazer a requisição para %s%n", this.url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Requisição concluída com sucesso");
            if (response.statusCode() == 200) {
                if (currentWebsiteStatus != WebsiteStatus.ONLINE) {
                    guilds.parallelStream().forEach(guild -> {
                        String message = String.format("%s online. Status code: %d%n", websiteName, response.statusCode());
                        guild.sendMessage(jda, message);
                    });
                    currentWebsiteStatus = WebsiteStatus.ONLINE;
                }
                System.out.printf("%s online. Status code: %d%n", websiteName, response.statusCode());
            } else {
                if (currentWebsiteStatus != WebsiteStatus.ERROR) {
                    guilds.parallelStream().forEach(guild -> {
                        String message = String.format("Algo de errado aconteceu com o %s. Status code: %d%n", websiteName, response.statusCode());
                        guild.sendMessage(jda, message);
                    });
                    currentWebsiteStatus = WebsiteStatus.ERROR;
                }
                System.out.printf("Algo de errado aconteceu com o %s. Status code: %d%n", websiteName, response.statusCode());
            }
            timeoutCount = 0; // reset count number
        } catch (IllegalArgumentException e) {
            if (currentWebsiteStatus != WebsiteStatus.ERROR) {
//                guilds.parallelStream().forEach(guild -> {
//                    String message = "URI informada está incorreta";
//                    guild.sendMessage(jda, message);
//                });
                currentWebsiteStatus = WebsiteStatus.ERROR;
            }
            e.printStackTrace();
            System.out.printf("URI do %s está incorreta%n", websiteName);
        } catch (HttpConnectTimeoutException e) {
            if (timeoutCount % 3 == 0) {
                if (currentWebsiteStatus != WebsiteStatus.TIMEOUT) {
                    guilds.parallelStream().forEach(guild -> {
                        String message = String.format("Timeout (%ds) ao tentar acessar o %s", timeoutSeconds, websiteName);
                        guild.sendMessage(jda, message);
                    });
                    currentWebsiteStatus = WebsiteStatus.TIMEOUT;
                }
                timeoutCount = 0; // reset count number
            }
            e.printStackTrace();
            timeoutCount++; // increment count number
            System.out.printf("Timeout #%s(%ds) ao tentar acessar o %s%n", timeoutCount, timeoutSeconds, websiteName);
        } catch (Exception e) {
            currentWebsiteStatus = WebsiteStatus.ERROR;
            e.printStackTrace();
        }
        presence.setStatus(currentWebsiteStatus.status);
    }

}
