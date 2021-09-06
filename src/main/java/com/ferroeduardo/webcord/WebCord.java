package com.ferroeduardo.webcord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferroeduardo.webcord.entity.ProgramProperties;
import com.ferroeduardo.webcord.listener.MessageListener;
import com.ferroeduardo.webcord.listener.ReadyListener;
import com.ferroeduardo.webcord.listener.WebObserver;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class WebCord {

    private static final String PROPERTIES_FILE_NAME = "webcord.json";
    private static final Logger LOGGER = LogManager.getLogger(WebCord.class);

    private JDA jda;
    private ProgramProperties properties;

    private WebCord() {
        try {
            LOGGER.info(String.format("Carregando arquivo de configurações '%s'", PROPERTIES_FILE_NAME));
            properties = getProgramProperties();

            LOGGER.info("Carregando propriedades do banco de dados");
            Map<String, Object> databaseProperties = getDatabaseProperties(properties);

            LOGGER.info("Inicializando 'EntityManagerFactory' para persistencia dos dados");
            EntityManagerFactory factory = Persistence.createEntityManagerFactory("webcord", databaseProperties);
            MessageListener messageListener = new MessageListener(factory);

            LOGGER.info("Inicializando JDA");
            initJDA(messageListener);

            LOGGER.info("Verificando existência de canais cadastrados no banco de dados");
            Util.checkDatabaseDataIntegrity(jda, factory);

            LOGGER.info("Inicializando WebObservers");
            initWebObservers(factory, messageListener);
        } catch (LoginException e) {
            LOGGER.error("Falha ao fazer login. Talvez o token esteja incorreto", e);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            LOGGER.error(e);
        }
    }

    private static ProgramProperties getProgramProperties() throws URISyntaxException, IOException {
        ProgramProperties properties;
        Path currentPath = new File(Program.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath().getParent();
        Path propertiesPath = Paths.get(currentPath.toString(), File.separator, PROPERTIES_FILE_NAME);
        ObjectMapper mapper = new ObjectMapper();

        //If file exists
        if (propertiesPath.toFile().isFile()) {
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
        Map<String, Object> databaseProperties = new HashMap<>();
        databaseProperties.put("javax.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/" + properties.getDatabaseName());
        databaseProperties.put("javax.persistence.jdbc.user", properties.getDatabaseUsername());
        databaseProperties.put("javax.persistence.jdbc.password", properties.getDatabasePassword());
        databaseProperties.put("show_sql", false);
        LOGGER.info("Propriedades do banco de dados carregadas com sucesso");
        return databaseProperties;
    }

    private void initWebObservers(EntityManagerFactory factory, MessageListener messageListener) {
        Map<String, WebObserver> webObservers = new HashMap<>();
        properties.getWebsites().forEach((name, url) -> webObservers.put(name, new WebObserver(jda, factory, properties.getTimeoutSeconds(), properties.getSchedulerSeconds(), name, url)));
        messageListener.setWebObservers(webObservers);
    }

    private void initJDA(MessageListener messageListener) throws LoginException, InterruptedException {
        jda = JDABuilder.createLight(properties.getToken())
                .setEnabledIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new ReadyListener())
                .addEventListeners(messageListener)
                .setActivity(Activity.of(Activity.ActivityType.WATCHING, MessageListener.COMMAND_PREFIX + "help"))
                .setStatus(OnlineStatus.IDLE)
                .build();

        jda.upsertCommand("help", "Ajuda").queue();
        jda.upsertCommand("ping", "Pong").queue();
        jda.upsertCommand("invite", "Convite do bot").queue();
        jda.upsertCommand("status", "Estado atual dos sites").queue();

        jda.awaitReady();
    }

    public static void start() {
        new WebCord();
    }
}
