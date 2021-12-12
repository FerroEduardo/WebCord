package com.ferroeduardo.webcord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferroeduardo.webcord.entity.ProgramProperties;
import com.ferroeduardo.webcord.listener.*;
import com.ferroeduardo.webcord.service.GuildInfoService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebCord {

    private static final String PROPERTIES_FILE_NAME = "webcord.json";
    private static final Logger LOGGER = LogManager.getLogger(WebCord.class);
    private GuildInfoService guildInfoService;

    private JDA jda;
    private ProgramProperties properties;

    private WebCord() {
        try {
            properties = getProgramProperties();

            Map<String, Object> databaseProperties = getDatabaseProperties(properties);

            guildInfoService = new GuildInfoService(databaseProperties);

            MessageListener messageListener = new MessageListener(guildInfoService, properties.getInfos());

            initJDA(messageListener);

            Util.checkDatabaseDataIntegrity(jda, guildInfoService);

            initWebObservers(messageListener);
        } catch (LoginException e) {
            LOGGER.error("Falha ao fazer login. Talvez o token esteja incorreto", e);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            LOGGER.error(e);
        }
    }

    private static ProgramProperties getProgramProperties() throws URISyntaxException, IOException {
        LOGGER.info(String.format("Carregando arquivo de configurações '%s'", PROPERTIES_FILE_NAME));
        ProgramProperties properties;
        Path currentPath = new File(Program.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath().getParent();
        Path propertiesPath = Paths.get(currentPath.toString(), File.separator, PROPERTIES_FILE_NAME);
        ObjectMapper mapper = new ObjectMapper();

        File propertiesFile = propertiesPath.toFile();
        boolean fileExists = propertiesFile.exists();
        boolean isFile = propertiesFile.isFile();
        if (isFile && fileExists) {
            properties = mapper.readValue(Files.newInputStream(propertiesPath), ProgramProperties.class);
            if (!properties.isOk()) {
                throw LOGGER.throwing(new IllegalStateException("Falha ao tentar obter os dados do arquivo: " + PROPERTIES_FILE_NAME));
            }
        } else {
            boolean success = propertiesPath.toFile().createNewFile();
            if (success) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(Files.newOutputStream(propertiesPath), new ProgramProperties());
                throw LOGGER.throwing(new IllegalStateException(String.format("Preencha os dados do servidor no arquivo %s", PROPERTIES_FILE_NAME)));
            } else {
                throw LOGGER.throwing(new IllegalStateException(String.format("Falha ao tentar criar o arquivo properties %s", PROPERTIES_FILE_NAME)));
            }
        }
        LOGGER.info("Arquivo de configurações carregado com sucesso");
        return properties;
    }

    private static Map<String, Object> getDatabaseProperties(ProgramProperties properties) {
        LOGGER.info("Carregando propriedades do banco de dados");
        Map<String, Object> databaseProperties = new HashMap<>();
        databaseProperties.put("javax.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/" + properties.getDatabaseName());
        databaseProperties.put("javax.persistence.jdbc.user", properties.getDatabaseUsername());
        databaseProperties.put("javax.persistence.jdbc.password", properties.getDatabasePassword());
        databaseProperties.put("show_sql", false);
        databaseProperties.put("hibernate.archive.autodetection", "class, hbm");
        databaseProperties.put("packagesToScan", "com.ferroeduardo.webcord.entity");
        LOGGER.info("Propriedades do banco de dados carregadas com sucesso");
        return databaseProperties;
    }

    private void initWebObservers(MessageListener messageListener) {
        LOGGER.info("Inicializando WebObservers");
        Map<String, WebObserver> webObservers = new HashMap<>();
        UpdatePresenceListener presenceListener = () -> {
            LOGGER.trace("Iniciando processo de atualização de presença");
            boolean isSomethingWrong = webObservers.values()
                    .parallelStream()
                    .map(WebObserver::getCurrentWebsiteStatus)
                    .anyMatch(websiteStatus -> websiteStatus.equals(WebsiteStatus.ERROR) || websiteStatus.equals(WebsiteStatus.TIMEOUT));
            Presence presence = jda.getPresence();
            if (isSomethingWrong) {
                presence.setStatus(WebsiteStatus.ERROR.status);
            } else {
                presence.setStatus(WebsiteStatus.ONLINE.status);
            }
            LOGGER.trace("Atualizando presença para " + presence.getStatus().name());
        };
        properties.getWebsites()
                .forEach((name, url) -> {
                    webObservers.put(name, new WebObserver(jda, guildInfoService, properties.getTimeoutSeconds(), properties.getSchedulerSeconds(), name, url, presenceListener));
                });
        messageListener.setWebObservers(webObservers);
    }

    private void initJDA(MessageListener messageListener) throws LoginException, InterruptedException {
        LOGGER.info("Inicializando JDA");
        jda = JDABuilder.createLight(properties.getToken())
                .setEnabledIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new ReadyListener())
                .addEventListeners(messageListener)
                .setActivity(Activity.playing(String.format("%shelp", MessageListener.COMMAND_PREFIX)))
                .setStatus(OnlineStatus.IDLE)
                .build();

        LOGGER.trace("Atualizando slash commands");
        jda.upsertCommand("help", "Ajuda").queue();
        jda.upsertCommand("ping", "Pong").queue();
        jda.upsertCommand("invite", "Convite do bot").queue();
        jda.upsertCommand("status", "Estado atual dos sites").queue();
        jda.upsertCommand("add", "Adiciona canal atual para receber avisos").queue();
        jda.upsertCommand("remove", "Remove canal atual e deixa de receber avisos").queue();

        jda.awaitReady();

        LOGGER.info("Iniciando 'ScheduledExecutorService' da quantidade de servidores em que o BOT está presente");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            jda.getPresence().setActivity(Activity.playing(String.format("em %d servidores | %shelp", jda.getGuilds().size(), MessageListener.COMMAND_PREFIX)));
        }, 0, 1, TimeUnit.HOURS);
    }

    public static void start() {
        LOGGER.info("Iniciando WebCord");
        new WebCord();
    }
}
