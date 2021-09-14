package com.ferroeduardo.webcord.service;

import com.ferroeduardo.webcord.entity.GuildInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuildInfoService {

    private static final Logger LOGGER = LogManager.getLogger(GuildInfoService.class);

    private final EntityManagerFactory factory;

    public GuildInfoService(Map<String, Object> databaseProperties) {
        LOGGER.info("Inicializando 'EntityManagerFactory' para persistencia dos dados");
        this.factory = Persistence.createEntityManagerFactory("webcord", databaseProperties);
    }

    public GuildInfo find(long guildId, long channelId) throws NoResultException {
        LOGGER.trace("Preparando para encontrar GuildInfo");
        EntityManager entityManager = factory.createEntityManager();
        GuildInfo singleResult = entityManager
                .createQuery("SELECT g FROM GuildInfo AS g WHERE g.guildId=?1 and g.guildChannelId=?2", GuildInfo.class)
                .setParameter(1, guildId)
                .setParameter(2, channelId)
                .setMaxResults(1)
                .getSingleResult();

        entityManager.close();
        LOGGER.trace("GuildInfo encontrada com sucesso");
        return singleResult;
    }

    public List<GuildInfo> findAll() {
        LOGGER.trace("Preparando para encontrar todas as GuildInfo");
        EntityManager entityManager = factory.createEntityManager();
        List<GuildInfo> guildInfoList = entityManager.createQuery("SELECT g FROM GuildInfo AS g", GuildInfo.class).getResultList();
        entityManager.close();
        LOGGER.trace("Todas as GuildInfo encontradas com sucesso");
        return guildInfoList;
    }

    public int delete(Set<Long> rowsToRemove) {
        LOGGER.trace("Preparando para apagar multiplas GuildInfos");
        EntityManager entityManager = factory.createEntityManager();
        entityManager.getTransaction().begin();
        Query deleteQuery = entityManager.createQuery("DELETE FROM GuildInfo g WHERE g.id in (?1)");
        deleteQuery.setParameter(1, rowsToRemove);
        int removedRows = deleteQuery.executeUpdate();
        entityManager.getTransaction().commit();
        LOGGER.trace("GuildInfos apagadas com sucesso");
        return removedRows;
    }

    public void delete(long guildId, long channelId) throws NoResultException {
        LOGGER.trace("Preparando para apagar GuildInfo");
        EntityManager entityManager = factory.createEntityManager();
        GuildInfo guildInfo = entityManager
                .createQuery("SELECT g FROM GuildInfo AS g WHERE g.guildId=?1 and g.guildChannelId=?2", GuildInfo.class)
                .setParameter(1, guildId)
                .setParameter(2, channelId)
                .setMaxResults(1)
                .getSingleResult();

        entityManager.getTransaction().begin();
        entityManager.remove(guildInfo);
        entityManager.getTransaction().commit();
        entityManager.close();
        LOGGER.trace("GuildInfo apagada com sucesso");
    }

    public GuildInfo save(GuildInfo guildInfo) {
        LOGGER.trace("Preparando para salvar GuildInfo");
        EntityManager entityManager = factory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(guildInfo);
        entityManager.flush();
        entityManager.getTransaction().commit();
        entityManager.close();
        LOGGER.trace("GuildInfo salva com sucesso");
        return guildInfo;
    }

}
