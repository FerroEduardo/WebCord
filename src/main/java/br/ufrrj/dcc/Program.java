package br.ufrrj.dcc;

import br.ufrrj.dcc.listeners.MessageListener;
import br.ufrrj.dcc.listeners.Moodle;
import br.ufrrj.dcc.listeners.ReadyListener;
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
import java.util.Properties;

public class Program {
    public static void main(String[] args) {
        JDA jda;
        Properties properties = new Properties();
        final String token;
        final String databaseName;
        final String databaseUsername;
        final String databasePassword;
        final int timeoutSeconds;
        final int schedulerTimeRate;
        EntityManagerFactory factory = null;
        try {
            Path currentPath = new File(Program.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath().getParent();
            String propertiesFileName = "dcc-bot.properties";
            Path propertiesPath = Paths.get(currentPath.toString(), File.separator, propertiesFileName);

            //If .properties file exists
            if (propertiesPath.toFile().isFile()) {
                properties.load(Files.newInputStream(propertiesPath));
                databaseName = String.valueOf(properties.get("database_name"));
                databaseUsername = String.valueOf(properties.get("database_username"));
                databasePassword = String.valueOf(properties.get("database_password"));
                token = String.valueOf(properties.get("token"));
                String timeout = String.valueOf(properties.get("timeout_seconds"));
                String scheduler = String.valueOf(properties.get("scheduler_seconds"));
                if (databaseName.equals("NULL") || databaseUsername.equals("NULL")
                        || databasePassword.equals("NULL") || token.equals("NULL")
                        || timeout.equals("NULL") || scheduler.equals("NULL")) {
                    throw new IllegalStateException("Falha ao tentar obter os dados do arquivo: " + propertiesFileName);
                }
                timeoutSeconds = Integer.parseInt(timeout);
                schedulerTimeRate = Integer.parseInt(scheduler);
            } else {
                boolean success = propertiesPath.toFile().createNewFile();
                if (success) {
                    properties.load(Files.newInputStream(propertiesPath));
                    properties.put("database_name", "NULL");
                    properties.put("database_username", "NULL");
                    properties.put("database_password", "NULL");
                    properties.put("token", "NULL");
                    properties.put("timeout_seconds", "NULL");
                    properties.put("scheduler_seconds", "NULL");
//                    properties.put("aaaaaaaaa", Arrays.toString(new Object[]{"aaaa", "bbb"}));
                    properties.store(Files.newOutputStream(propertiesPath), null);
                    throw new IllegalStateException(String.format("Preencha os dados do servidor no arquivo %s", propertiesFileName));
                } else {
                    throw new IllegalStateException(String.format("Falha ao tentar criar o arquivo properties %s", propertiesFileName));
                }
            }

            Map<String, Object> databaseProperties = new HashMap<>();
            databaseProperties.put("javax.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/" + databaseName);
            databaseProperties.put("javax.persistence.jdbc.user", databaseUsername);
            databaseProperties.put("javax.persistence.jdbc.password", databasePassword);
            factory = Persistence.createEntityManagerFactory("dcc_bot", databaseProperties);
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(new ReadyListener())
                    .addEventListeners(new MessageListener(factory))
                    .build();

            jda.awaitReady();
            jda.getPresence().setActivity(Activity.of(Activity.ActivityType.WATCHING, "dcc.help"));

            Moodle moodle = new Moodle(jda, factory, timeoutSeconds, schedulerTimeRate);
        } catch (LoginException e) {
            System.out.println("Falha ao fazer login. Talvez o token esteja incorreto");
            e.printStackTrace();
        } catch (IOException | URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
