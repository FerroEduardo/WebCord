package br.ufrrj.dcc.listeners;

import br.ufrrj.dcc.entity.GuildInfo;
import br.ufrrj.dcc.exceptions.AlreadyExistsException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MessageListener extends ListenerAdapter {

    public static final String COMMAND_PREFIX = "dcc.";
    private final EntityManagerFactory factory;
    Map<String, WebObserver> webObservers;

    public MessageListener(EntityManagerFactory factory) {
        this.factory = factory;
    }

    public void setWebObservers(Map<String, WebObserver> webObservers) {
        this.webObservers = webObservers;
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;
        Member member = event.getMember();
        Message msg = event.getMessage();
        Guild guild = event.getGuild();
        TextChannel channel = event.getChannel();
        User author = event.getAuthor();
        String content = msg.getContentRaw();

        Consumer<Message> deleteMessagesAfterTime = message -> {
            message.delete().queueAfter(5, TimeUnit.SECONDS);
            msg.delete().queueAfter(5, TimeUnit.SECONDS);
        };

        if (content.equals(COMMAND_PREFIX + "ping")) {
            long time = System.currentTimeMillis();
            channel.sendMessage("Pong!")
                    .queue(response -> {
                        response.editMessageFormat("Pong: %d ms", System.currentTimeMillis() - time).queue();
                    });
        } else if (content.equals(COMMAND_PREFIX + "help")) {
            StringBuilder descriptionStringBuilder = new StringBuilder();
            descriptionStringBuilder.append("Quando fico:\n" +
                                            "Online - Tudo funcionando perfeitamente\n" +
                                            "Ocupado - Algum serviço está fora do ar\n" +
                                            "Ausente - Inicializando bot\n" +
                                            "Invisível - Estou fora do ar\n\n");
            descriptionStringBuilder.append("dcc.help - Comandos\n");
            descriptionStringBuilder.append("dcc.ping - Ping\n");
            descriptionStringBuilder.append("dcc.status - Estado atual dos sites cadastrados\n");
            descriptionStringBuilder.append("dcc.add - Adiciona canal atual para receber avisos\n");
            descriptionStringBuilder.append("dcc.remove - Remove canal atual e deixa de receber avisos\n");
            descriptionStringBuilder.append("dcc.invite - Convite do bot");
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTimestamp(event.getMessage().getTimeCreated());
            eb.setColor(new Color((int) (Math.random() * 0x1000000)));
            eb.setTitle("Comandos");
            eb.setDescription(descriptionStringBuilder.toString());
            msg.reply(eb.build()).queue();
        } else if (content.equals(COMMAND_PREFIX + "add")) {
            Optional<Role> optionalRole = member.getRoles().stream().filter(role -> role.hasPermission(Permission.ADMINISTRATOR)).findFirst();
            boolean isAdmin = optionalRole.isPresent();
            if (isAdmin) {
                long guildId = guild.getIdLong();
                long channelId = channel.getIdLong();
                EntityManager entityManager = factory.createEntityManager();
                try {
                    GuildInfo singleResult = entityManager
                            .createQuery("SELECT g FROM GuildInfo AS g WHERE g.guildId=?1 and g.guildChannelId=?2", GuildInfo.class)
                            .setParameter(1, guildId)
                            .setParameter(2, channelId)
                            .setMaxResults(1)
                            .getSingleResult();
                    if (singleResult != null) {
                        throw new AlreadyExistsException("Canal já foi adicionado anteriormente");
                    }
                } catch (NoResultException e) {
//                    e.printStackTrace();
                    GuildInfo guildInfo = new GuildInfo(guildId, channelId);
                    entityManager.getTransaction().begin();
                    entityManager.persist(guildInfo);
                    entityManager.getTransaction().commit();
                    entityManager.close();
                    msg.reply("Configurado com sucesso").queue(deleteMessagesAfterTime);
                } catch (AlreadyExistsException e) {
                    msg.reply(e.getMessage()).queue(message -> {
                                message.delete().queueAfter(5, TimeUnit.SECONDS);
                                msg.delete().queueAfter(5, TimeUnit.SECONDS);
                            }
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.reply("Falha ao adicionar o canal").queue(deleteMessagesAfterTime);
                }
            } else {
                msg.reply("Você não é um administrador para realizar essa ação").queue(deleteMessagesAfterTime);
            }
        } else if (content.equals(COMMAND_PREFIX + "remove")) {
            boolean isAdmin = member.getRoles().stream().anyMatch(role -> role.hasPermission(Permission.ADMINISTRATOR));
            if (isAdmin) {
                long guildId = guild.getIdLong();
                long channelId = channel.getIdLong();
                EntityManager entityManager = factory.createEntityManager();
                try {
                    GuildInfo singleResult = entityManager
                            .createQuery("SELECT g FROM GuildInfo AS g WHERE g.guildId=?1 and g.guildChannelId=?2", GuildInfo.class)
                            .setParameter(1, guildId)
                            .setParameter(2, channelId)
                            .setMaxResults(1)
                            .getSingleResult();

                    entityManager.getTransaction().begin();
                    entityManager.remove(singleResult);
                    entityManager.getTransaction().commit();
                    msg.reply("Canal removido com sucesso").queue(deleteMessagesAfterTime);
                } catch (NoResultException e) {
//                    e.printStackTrace();
                    msg.reply("Parece que esse canal não está cadastrado").queue(deleteMessagesAfterTime);
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.reply("Falha ao remover o canal").queue(deleteMessagesAfterTime);
                }
            } else {
                msg.reply("Você não é um administrador para realizar essa ação").queue(deleteMessagesAfterTime);
            }
        } else if (content.equals(COMMAND_PREFIX + "invite")) {
            long clientId = event.getJDA().getSelfUser().getIdLong();
            int permission = 2048;
            String link = String.format("https://discord.com/api/oauth2/authorize?client_id=%d&permissions=%d", clientId, permission) + "&scope=bot%20applications.commands";
            msg.reply("Convite: " + link).queue();
        } else if (content.equals(COMMAND_PREFIX + "status")) {
            if (webObservers == null) {
                msg.reply("Aguarde um momento...").queue(deleteMessagesAfterTime);
            } else {
                StringBuilder websiteStatusStringBuilder = new StringBuilder();
                webObservers.forEach((name, webObserver) -> {
                    WebsiteStatus currentWebsiteStatus = webObserver.getCurrentWebsiteStatus();
                    if (currentWebsiteStatus != WebsiteStatus.NONE) {
                        LocalDateTime latestStatusTime = webObserver.getLatestStatusTime();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                        websiteStatusStringBuilder.append(String.format(
                                "%s:\n" +
                                "- Status: %s\n",
                                name, currentWebsiteStatus.name()));
                        if (currentWebsiteStatus == WebsiteStatus.TIMEOUT) {
                            websiteStatusStringBuilder.append(String.format(
                                    "- Quantidade de Timeouts: %d\n",
                                    webObserver.getTimeoutCount()));
                        }
                        websiteStatusStringBuilder.append(String.format(
                                "- Desde: %s\n\n",
                                latestStatusTime.format(formatter)));
                    } else {
                        websiteStatusStringBuilder.append(String.format(
                                "%s:\n" +
                                "- Status: AGUARDE\n\n",
                                name));
                    }
                });
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTimestamp(event.getMessage().getTimeCreated());
                eb.setColor(new Color((int) (Math.random() * 0x1000000)));
                eb.setTitle("Site - Status");
                eb.setDescription(websiteStatusStringBuilder.toString());
                msg.reply(eb.build()).queue();
            }
        }
    }
}
