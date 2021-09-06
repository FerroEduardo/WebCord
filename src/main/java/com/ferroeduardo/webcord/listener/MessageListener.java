package com.ferroeduardo.webcord.listener;

import com.ferroeduardo.webcord.Util;
import com.ferroeduardo.webcord.entity.GuildInfo;
import com.ferroeduardo.webcord.exception.AlreadyExistsException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOGGER = LogManager.getLogger(MessageListener.class);

    public static final String COMMAND_PREFIX = "webcord.";
    private final EntityManagerFactory factory;
    private Map<String, WebObserver> webObservers;

    public MessageListener(EntityManagerFactory factory) {
        this.factory = factory;
    }

    public void setWebObservers(Map<String, WebObserver> webObservers) {
        this.webObservers = webObservers;
    }


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        final Message msg = event.getMessage();
        final ChannelType channelType = event.getChannelType();
        final MessageChannel messageChannel = event.getChannel();
        final User author = event.getAuthor();
        final String content = msg.getContentRaw();

        final Consumer<Message> deleteMessagesAfterTime = message -> {
            message.delete().queueAfter(5, TimeUnit.SECONDS);
            msg.delete().queueAfter(5, TimeUnit.SECONDS);
        };

        if (content.equals(COMMAND_PREFIX + "ping")) {
            long time = System.currentTimeMillis();
            messageChannel.sendMessage("Pong!")
                    .queue(response -> {
                        response.editMessageFormat("Pong: %d ms", System.currentTimeMillis() - time).queue();
                    });
        } else if (content.equals(COMMAND_PREFIX + "help")) {
            StringBuilder descriptionStringBuilder = getHelpStringBuilder();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTimestamp(event.getMessage().getTimeCreated());
            eb.setColor(new Color((int) (Math.random() * 0x1000000)));
            eb.setTitle("Ajuda");
            eb.setDescription(descriptionStringBuilder.toString());
            msg.reply(eb.build()).queue();
        } else if (content.equals(COMMAND_PREFIX + "invite")) {
            msg.reply("Convite: " + Util.getInviteLink(event.getJDA())).queue();
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
                        if (currentWebsiteStatus == WebsiteStatus.TIMEOUT) {
                            websiteStatusStringBuilder.append(String.format(
                                    "%s:\n" +
                                            "- Status: %s\n" +
                                            "- Quantidade de Timeouts: %d\n" +
                                            "- Desde: %s\n\n",
                                    name, currentWebsiteStatus.name(), webObserver.getTimeoutCount(), latestStatusTime.format(formatter)));
                        } else {
                            websiteStatusStringBuilder.append(String.format(
                                    "%s:\n" +
                                            "- Status: %s\n\n",
                                    name, currentWebsiteStatus.name()));
                        }
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

        if (channelType == ChannelType.TEXT) {
            TextChannel textChannel = event.getTextChannel();
            Member member = event.getMember();
            Guild guild = event.getGuild();
            if (content.equals(COMMAND_PREFIX + "add")) {
                Optional<Role> optionalRole = member.getRoles().stream().filter(role -> role.hasPermission(Permission.ADMINISTRATOR)).findFirst();
                boolean isAdmin = optionalRole.isPresent();
                if (isAdmin) {
                    long guildId = guild.getIdLong();
                    long channelId = textChannel.getIdLong();
                    EntityManager entityManager = factory.createEntityManager();
                    try {
                        GuildInfo singleResult = entityManager
                                .createQuery("SELECT g FROM GuildInfo AS g WHERE g.guildId=?1 and g.guildChannelId=?2", GuildInfo.class)
                                .setParameter(1, guildId)
                                .setParameter(2, channelId)
                                .setMaxResults(1)
                                .getSingleResult();
                        if (singleResult != null) {
                            throw LOGGER.throwing(new AlreadyExistsException("Canal já foi adicionado anteriormente"));
                        }
                    } catch (NoResultException e) {
                        LOGGER.trace(String.format("Nenhum canal foi encontrado, cadastrando canal '%d' do servidor '%d' no banco de dados", channelId, guildId), e);
                        GuildInfo guildInfo = new GuildInfo(guildId, channelId);
                        entityManager.getTransaction().begin();
                        entityManager.persist(guildInfo);
                        entityManager.getTransaction().commit();
                        entityManager.close();
                        msg.reply("Configurado com sucesso").queue(deleteMessagesAfterTime);
                    } catch (AlreadyExistsException e) {
                        LOGGER.debug(e);
                        msg.reply(e.getMessage()).queue(deleteMessagesAfterTime);
                    } catch (Exception e) {
                        LOGGER.warn(e);
                        msg.reply("Ocorreu uma falha ao adicionar o canal").queue(deleteMessagesAfterTime);
                    }
                } else {
                    msg.reply("Você não é um administrador para realizar essa ação").queue(deleteMessagesAfterTime);
                }
            } else if (content.equals(COMMAND_PREFIX + "remove")) {
                boolean isAdmin = member.getRoles().stream().anyMatch(role -> role.hasPermission(Permission.ADMINISTRATOR));
                if (isAdmin) {
                    long guildId = guild.getIdLong();
                    long channelId = textChannel.getIdLong();
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
                        entityManager.close();
                        msg.reply("Canal removido com sucesso").queue(deleteMessagesAfterTime);
                    } catch (NoResultException e) {
                        LOGGER.debug(e);
                        msg.reply("Parece que esse canal não está cadastrado para poder ser removido").queue(deleteMessagesAfterTime);
                    } catch (Exception e) {
                        LOGGER.warn(e);
                        msg.reply("Ocorreu uma falha ao remover o canal").queue(deleteMessagesAfterTime);
                    }
                } else {
                    msg.reply("Você não é um administrador para poder realizar essa ação").queue(deleteMessagesAfterTime);
                }
            }
        }
    }

    private StringBuilder getHelpStringBuilder() {
        StringBuilder descriptionStringBuilder = new StringBuilder();
        descriptionStringBuilder.append("Quando fico:\n" +
                                        "Online - Tudo funcionando perfeitamente\n" +
                                        "Ocupado - Algum serviço está fora do ar\n" +
                                        "Ausente - Inicializando bot\n" +
                                        "Invisível - Estou fora do ar\n\n");
        descriptionStringBuilder.append(String.format("%sping - Ping\n", COMMAND_PREFIX));
        descriptionStringBuilder.append(String.format("%shelp - Comandos\n", COMMAND_PREFIX));
        descriptionStringBuilder.append(String.format("%sinvite - Convite do bot\n", COMMAND_PREFIX));
        descriptionStringBuilder.append(String.format("%sstatus - Estado atual dos sites cadastrados\n", COMMAND_PREFIX));
        descriptionStringBuilder.append("\nSomente servidores---------------------------------------\n");
        descriptionStringBuilder.append(String.format("%sadd - Adiciona canal atual para receber avisos\n", COMMAND_PREFIX));
        descriptionStringBuilder.append(String.format("%sremove - Remove canal atual e deixa de receber avisos\n", COMMAND_PREFIX));
        return descriptionStringBuilder;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        String eventName = event.getName();
        if (eventName.equals("help")) {
            StringBuilder descriptionStringBuilder = getHelpStringBuilder();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTimestamp(event.getTimeCreated());
            eb.setColor(new Color((int) (Math.random() * 0x1000000)));
            eb.setTitle("Ajuda");
            eb.setDescription(descriptionStringBuilder.toString());
            MessageBuilder mb = new MessageBuilder(eb);
            event.reply(mb.build()).queue();
        } else if (eventName.equals("ping")) {
            long time = System.currentTimeMillis();
            event.reply("Pong!")
                    .queue(response -> {
                        response.editOriginal(String.format("Pong: %d ms", System.currentTimeMillis() - time)).queue();
                    });
        } else if (eventName.equals("invite")) {
            event.reply(Util.getInviteLink(event.getJDA())).queue();
        } else if (eventName.equals("status")) {
            if (webObservers == null) {
                event.reply("Aguarde um momento...").queue();
            } else {
                StringBuilder websiteStatusStringBuilder = new StringBuilder();
                webObservers.forEach((name, webObserver) -> {
                    WebsiteStatus currentWebsiteStatus = webObserver.getCurrentWebsiteStatus();
                    if (currentWebsiteStatus != WebsiteStatus.NONE) {
                        LocalDateTime latestStatusTime = webObserver.getLatestStatusTime();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                        if (currentWebsiteStatus == WebsiteStatus.TIMEOUT) {
                            websiteStatusStringBuilder.append(String.format(
                                    "%s:\n" +
                                            "- Status: %s\n" +
                                            "- Quantidade de Timeouts: %d\n" +
                                            "- Desde: %s\n\n",
                                    name, currentWebsiteStatus.name(), webObserver.getTimeoutCount(), latestStatusTime.format(formatter)));
                        } else {
                            websiteStatusStringBuilder.append(String.format(
                                    "%s:\n" +
                                            "- Status: %s\n\n",
                                    name, currentWebsiteStatus.name()));
                        }
                    } else {
                        websiteStatusStringBuilder.append(String.format(
                                "%s:\n" +
                                        "- Status: AGUARDE\n\n",
                                name));
                    }
                });
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTimestamp(event.getTimeCreated());
                eb.setColor(new Color((int) (Math.random() * 0x1000000)));
                eb.setTitle("Site - Status");
                eb.setDescription(websiteStatusStringBuilder.toString());
                MessageBuilder mb = new MessageBuilder(eb);
                event.reply(mb.build()).queue();
            }
        }
    }
}
