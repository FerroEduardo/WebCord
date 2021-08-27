package br.ufrrj.dcc;

import br.ufrrj.dcc.entity.ProgramProperties;
import br.ufrrj.dcc.listeners.MessageListener;
import br.ufrrj.dcc.listeners.ReadyListener;
import br.ufrrj.dcc.listeners.WebObserver;
import com.fasterxml.jackson.databind.ObjectMapper;
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

            LOGGER.debug("Inicializando 'EntityManagerFactory' para persistencia dos dados");
            EntityManagerFactory factory = Persistence.createEntityManagerFactory("dcc_bot", databaseProperties);
            MessageListener messageListener = new MessageListener(factory);

            LOGGER.debug("Inicializando Build do JDA");
            jda = JDABuilder.createDefault(properties.getToken())
                    .addEventListeners(new ReadyListener())
                    .addEventListeners(messageListener)
                    .build();

            jda.awaitReady();
            jda.getPresence().setActivity(Activity.of(Activity.ActivityType.WATCHING, "dcc.help"));
            jda.getPresence().setStatus(OnlineStatus.IDLE);
            LOGGER.debug("Inicializando WebObservers");
            Map<String, WebObserver> webObservers = new HashMap<>();
            properties.getWebsites().forEach((name, url) -> webObservers.put(name, new WebObserver(jda, factory, properties.getTimeoutSeconds(), properties.getSchedulerSeconds(), name, url)));
            messageListener.setWebObservers(webObservers);
        } catch (LoginException e) {
            System.out.println("Falha ao fazer login. Talvez o token esteja incorreto");
            e.printStackTrace();
        } catch (IOException | URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Object> getDatabaseProperties(ProgramProperties properties) {
        LOGGER.debug("Carregando propriedades do banco de dados");
        Map<String, Object> databaseProperties = new HashMap<>();
        databaseProperties.put("javax.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/" + properties.getDatabaseName());
        databaseProperties.put("javax.persistence.jdbc.user", properties.getDatabaseUsername());
        databaseProperties.put("javax.persistence.jdbc.password", properties.getDatabasePassword());
        databaseProperties.put("show_sql", false);
        LOGGER.debug("Propriedades do banco de dados carregadas com suecsso");
        return databaseProperties;
    }

    private static ProgramProperties getProgramProperties() throws URISyntaxException, IOException {
        String propertiesFileName = "dcc-bot.json";
        LOGGER.debug(String.format("Carregando arquivo de configurações '%s'", propertiesFileName));
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
        LOGGER.debug("Arquivo de configurações carregado com sucesso");
        return properties;
    }
}
