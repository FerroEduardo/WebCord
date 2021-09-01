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

public class Program {

    private static final Logger LOGGER = LogManager.getLogger(Program.class);

    public static void main(String[] args) {
        JDA jda;
        ProgramProperties properties;
        try {
            properties = getProgramProperties();

            Map<String, Object> databaseProperties = getDatabaseProperties(properties);

            LOGGER.info("Inicializando 'EntityManagerFactory' para persistencia dos dados");
            EntityManagerFactory factory = Persistence.createEntityManagerFactory("webcord", databaseProperties);
            MessageListener messageListener = new MessageListener(factory);

            LOGGER.info("Inicializando JDA");
            jda = JDABuilder.createDefault(properties.getToken())
                    .addEventListeners(new ReadyListener())
                    .addEventListeners(messageListener)
                    .build();

            jda.awaitReady();
            jda.getPresence().setActivity(Activity.of(Activity.ActivityType.WATCHING, MessageListener.COMMAND_PREFIX + "help"));
            jda.getPresence().setStatus(OnlineStatus.IDLE);
            LOGGER.info("Verificando existência de canais cadastrados no banco de dados");
            Util.checkDatabaseDataIntegrity(jda, factory);

            LOGGER.info("Inicializando WebObservers");
            Map<String, WebObserver> webObservers = new HashMap<>();
            properties.getWebsites().forEach((name, url) -> webObservers.put(name, new WebObserver(jda, factory, properties.getTimeoutSeconds(), properties.getSchedulerSeconds(), name, url)));
            messageListener.setWebObservers(webObservers);
        } catch (LoginException e) {
            LOGGER.error("Falha ao fazer login. Talvez o token esteja incorreto", e);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            LOGGER.error(e);
        }
    }

    private static Map<String, Object> getDatabaseProperties(ProgramProperties properties) {
        LOGGER.info("Carregando propriedades do banco de dados");
        Map<String, Object> databaseProperties = new HashMap<>();
        databaseProperties.put("javax.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/" + properties.getDatabaseName());
        databaseProperties.put("javax.persistence.jdbc.user", properties.getDatabaseUsername());
        databaseProperties.put("javax.persistence.jdbc.password", properties.getDatabasePassword());
        databaseProperties.put("show_sql", false);
        LOGGER.info("Propriedades do banco de dados carregadas com sucesso");
        return databaseProperties;
    }

    private static ProgramProperties getProgramProperties() throws URISyntaxException, IOException {
        String propertiesFileName = "webcord.json";
        LOGGER.info(String.format("Carregando arquivo de configurações '%s'", propertiesFileName));
        ProgramProperties properties;
        Path currentPath = new File(Program.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath().getParent();
        Path propertiesPath = Paths.get(currentPath.toString(), File.separator, propertiesFileName);
        ObjectMapper mapper = new ObjectMapper();

        //If file exists
        if (propertiesPath.toFile().isFile()) {
            properties = mapper.readValue(Files.newInputStream(propertiesPath), ProgramProperties.class);
            if (!properties.isOk()) {
                throw LOGGER.throwing(new IllegalStateException("Falha ao tentar obter os dados do arquivo: " + propertiesFileName));
            }
        } else {
            boolean success = propertiesPath.toFile().createNewFile();
            if (success) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(Files.newOutputStream(propertiesPath), new ProgramProperties());
                throw LOGGER.throwing(new IllegalStateException(String.format("Preencha os dados do servidor no arquivo %s", propertiesFileName)));
            } else {
                throw LOGGER.throwing(new IllegalStateException(String.format("Falha ao tentar criar o arquivo properties %s", propertiesFileName)));
            }
        }
        LOGGER.info("Arquivo de configurações carregado com sucesso");
        return properties;
    }
}
