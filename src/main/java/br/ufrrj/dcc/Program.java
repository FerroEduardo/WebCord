package br.ufrrj.dcc;

import br.ufrrj.dcc.entity.ProgramProperties;
import br.ufrrj.dcc.listeners.MessageListener;
import br.ufrrj.dcc.listeners.ReadyListener;
import br.ufrrj.dcc.listeners.WebObserver;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

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
    public static void main(String[] args) {
        JDA jda;
        ProgramProperties properties;
        try {
            Path currentPath = new File(Testes.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath().getParent();
            String propertiesFileName = "dcc-bot.json";
            Path propertiesPath = Paths.get(currentPath.toString(), File.separator, propertiesFileName);
            ObjectMapper mapper = new ObjectMapper();

            //If file exists
            if (propertiesPath.toFile().isFile()) {
                properties = mapper.readValue(Files.newInputStream(propertiesPath), ProgramProperties.class);
                if (!properties.isOk()) {
                    throw new IllegalStateException("Falha ao tentar obter os dados do arquivo: " + propertiesFileName);
                }
            } else {
                boolean success = propertiesPath.toFile().createNewFile();
                if (success) {
                    mapper.writerWithDefaultPrettyPrinter().writeValue(Files.newOutputStream(propertiesPath), new ProgramProperties());
                    throw new IllegalStateException(String.format("Preencha os dados do servidor no arquivo %s", propertiesFileName));
                } else {
                    throw new IllegalStateException(String.format("Falha ao tentar criar o arquivo properties %s", propertiesFileName));
                }
            }

            Map<String, Object> databaseProperties = new HashMap<>();
            databaseProperties.put("javax.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/" + properties.getDatabaseName());
            databaseProperties.put("javax.persistence.jdbc.user", properties.getDatabaseUsername());
            databaseProperties.put("javax.persistence.jdbc.password", properties.getDatabasePassword());

            EntityManagerFactory factory = Persistence.createEntityManagerFactory("dcc_bot", databaseProperties);
            jda = JDABuilder.createDefault(properties.getToken())
                    .addEventListeners(new ReadyListener())
                    .addEventListeners(new MessageListener(factory))
                    .build();

            jda.awaitReady();
            jda.getPresence().setActivity(Activity.of(Activity.ActivityType.WATCHING, "dcc.help"));
            properties.getWebsites().forEach((name, url) -> new WebObserver(jda, factory, properties.getTimeoutSeconds(), properties.getSchedulerSeconds(), name, url));
        } catch (LoginException e) {
            System.out.println("Falha ao fazer login. Talvez o token esteja incorreto");
            e.printStackTrace();
        } catch (IOException | URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
