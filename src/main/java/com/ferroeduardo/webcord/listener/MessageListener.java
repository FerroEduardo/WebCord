package com.ferroeduardo.webcord.listener;

import com.ferroeduardo.webcord.Util;
import com.ferroeduardo.webcord.entity.GuildInfo;
import com.ferroeduardo.webcord.exception.AlreadyExistsException;
import com.ferroeduardo.webcord.service.GuildInfoService;
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

import javax.annotation.Nullable;
import javax.persistence.NoResultException;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

public class MessageListener extends ListenerAdapter {

    public static final String COMMAND_PREFIX = "\\";
    private static final Logger LOGGER = LogManager.getLogger(MessageListener.class);
    private Map<String, WebObserver> webObservers;
    private Map<String, String> infos;
    private RandomGenerator randomGenerator;
    private GuildInfoService guildInfoService;

    public MessageListener(GuildInfoService guildInfoService, @Nullable Map<String, String> infos) {
        this.guildInfoService = guildInfoService;
        this.randomGenerator =  RandomGenerator.getDefault();
        this.infos = infos;
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
            eb.setColor(new Color((int) (randomGenerator.nextDouble() * 0x1000000)));
            eb.setTitle("Ajuda");
            eb.setDescription(descriptionStringBuilder.toString());
            msg.replyEmbeds(eb.build()).queue();
        } else if (content.equals(COMMAND_PREFIX + "invite")) {
            msg.reply("Convite: " + Util.getInviteLink(event.getJDA())).queue();
        } else if (content.equals(COMMAND_PREFIX + "status")) {
            if (webObservers == null) {
                msg.reply("Aguarde um momento...").queue(deleteMessagesAfterTime);
            } else {
                if (webObservers.isEmpty()) {
                    msg.reply("Nenhum site foi cadastrado no bot").queue();
                } else {
                    MessageBuilder mb = getWebObserversStatusMessageBuilder(event.getMessage().getTimeCreated());
                    msg.reply(mb.build()).queue();
                }
            }
        }

        if (channelType == ChannelType.TEXT) {
            TextChannel textChannel = event.getTextChannel();
            Member member = event.getMember();
            Guild guild = event.getGuild();
            boolean isAdmin = member.getRoles().stream().anyMatch(role -> role.hasPermission(Permission.ADMINISTRATOR));
            if (content.equals(COMMAND_PREFIX + "add")) {
                if (isAdmin) {
                    long guildId = guild.getIdLong();
                    long channelId = textChannel.getIdLong();
                    try {
                        GuildInfo guildInfo = guildInfoService.find(guildId, channelId);
                        if (guildInfo != null) {
                            throw LOGGER.throwing(new AlreadyExistsException("Canal já foi adicionado anteriormente"));
                        }
                    } catch (NoResultException e) {
                        LOGGER.trace(String.format("Nenhum canal foi encontrado, cadastrando canal '%d' do servidor '%d' no banco de dados", channelId, guildId), e);
                        guildInfoService.save(new GuildInfo(guildId, channelId));
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
                if (isAdmin) {
                    long guildId = guild.getIdLong();
                    long channelId = textChannel.getIdLong();
                    try {
                        guildInfoService.delete(guildId, channelId);
                        msg.reply("Canal removido com sucesso").queue(deleteMessagesAfterTime);
                    } catch (NoResultException e) {
                        String message = "Parece que esse canal não está cadastrado para poder ser removido";
                        LOGGER.debug(message, e);
                        msg.reply(message).queue(deleteMessagesAfterTime);
                    } catch (Exception e) {
                        String message = "Ocorreu uma falha ao remover o canal";
                        LOGGER.warn(message, e);
                        msg.reply(message).queue(deleteMessagesAfterTime);
                    }
                } else {
                    msg.reply("Você não é um administrador para poder realizar essa ação").queue(deleteMessagesAfterTime);
                }
            }
        }
    }

    private StringBuilder getHelpStringBuilder() {
        StringBuilder descriptionStringBuilder = new StringBuilder();
        descriptionStringBuilder.append(String.format(
                """
                        Quando fico:
                        Online - Tudo funcionando perfeitamente
                        Ocupado - Algum serviço está fora do ar
                        Ausente - Inicializando bot
                        Invisível - Estou fora do ar
                                        
                        %1$sping - Ping
                        %1$shelp - Comandos
                        %1$sinvite - Convite do bot
                        %1$sstatus - Estado atual dos sites cadastrados
                                        
                        Somente servidores---------------------------------------
                        %1$sadd - Adiciona canal atual para receber avisos
                        %1$sremove - Remove canal atual e deixa de receber avisos
                        """, COMMAND_PREFIX));
        if (!webObservers.isEmpty()) {
            descriptionStringBuilder.append("\nSites cadastrados----------------------------------------\n");
            webObservers.keySet().forEach(key -> {
                descriptionStringBuilder.append(String.format("%s\n", key));
            });
        } else {
            descriptionStringBuilder.append(
                    """
                            Sites cadastrados----------------------------------------
                            Nenhum site foi cadastrado no bot
                            """);
        }
        if (this.infos != null && !this.infos.isEmpty()) {
            descriptionStringBuilder.append("------------------------------\n");
            this.infos.forEach((key, value) -> descriptionStringBuilder.append(String.format("%s: %s\n", key, value)));
        }
        return descriptionStringBuilder;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        String eventName = event.getName();
        if (eventName.equals("help")) {
            StringBuilder descriptionStringBuilder = getHelpStringBuilder();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTimestamp(event.getTimeCreated());
            eb.setColor(new Color((int) (randomGenerator.nextDouble() * 0x1000000)));
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
                if (webObservers.isEmpty()) {
                    event.reply("Nenhum site foi cadastrado no bot").queue();
                } else {
                    MessageBuilder mb = getWebObserversStatusMessageBuilder(event.getTimeCreated());
                    event.reply(mb.build()).queue();
                }
            }
        }
    }

    private MessageBuilder getWebObserversStatusMessageBuilder(OffsetDateTime timeCreated) {
        StringBuilder websiteStatusStringBuilder = getWebObserversStatusStringBuilder();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTimestamp(timeCreated);
        eb.setColor(new Color((int) (randomGenerator.nextDouble() * 0x1000000)));
        eb.setTitle("Site - Status");
        eb.setDescription(websiteStatusStringBuilder.toString());
        return new MessageBuilder(eb);
    }

    private StringBuilder getWebObserversStatusStringBuilder() {
        StringBuilder websiteStatusStringBuilder = new StringBuilder();
        webObservers.forEach((name, webObserver) -> {
            WebsiteStatus currentWebsiteStatus = webObserver.getCurrentWebsiteStatus();
            if (currentWebsiteStatus != WebsiteStatus.NONE) {
                LocalDateTime latestStatusTime = webObserver.getLatestStatusTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                if (currentWebsiteStatus == WebsiteStatus.TIMEOUT) {
                    websiteStatusStringBuilder.append(String.format(
                            """
                                    %s:
                                    - Status: %s
                                    - Quantidade de Timeouts: %d
                                    - Desde: %s
                                                                        
                                    """,
                            name, currentWebsiteStatus.name(), webObserver.getTimeoutCount(), latestStatusTime.format(formatter)));
                } else {
                    websiteStatusStringBuilder.append(String.format(
                            """                            
                                    %s:
                                    - Status: %s
                                                                        
                                    """,
                            name, currentWebsiteStatus.name()));
                }
            } else {
                websiteStatusStringBuilder.append(String.format(
                        """
                                %s:
                                - Status: AGUARDE
                                                                
                                """,
                        name));
            }
        });
        return websiteStatusStringBuilder;
    }
}
