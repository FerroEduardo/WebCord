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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MessageListener extends ListenerAdapter {

    public static final String COMMAND_PREFIX = "dcc.";
    private final EntityManagerFactory factory;

    public MessageListener(EntityManagerFactory factory) {
        this.factory = factory;
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
            StringBuilder desctiptionStringBuilder = new StringBuilder();
            desctiptionStringBuilder.append("dcc.help - Comandos\n");
            desctiptionStringBuilder.append("dcc.ping - Ping\n");
            desctiptionStringBuilder.append("dcc.add - Adiciona canal atual para receber avisos\n");
            desctiptionStringBuilder.append("dcc.remove - Remove canal atual e deixa de receber avisos\n");
            desctiptionStringBuilder.append("dcc.invite - Convite do bot");
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(new Color((int) (Math.random() * 0x1000000)));
            eb.setTitle("Comandos");
            eb.setDescription(desctiptionStringBuilder.toString());
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
        }
    }
}
