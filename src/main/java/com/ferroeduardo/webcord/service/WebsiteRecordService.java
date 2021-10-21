package com.ferroeduardo.webcord.service;

import com.ferroeduardo.webcord.entity.WebsiteRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;

public class WebsiteRecordService {

    private static final Logger LOGGER = LogManager.getLogger(WebsiteRecordService.class);

    private final EntityManagerFactory factory;

    public WebsiteRecordService(Map<String, Object> databaseProperties) {
        LOGGER.info("Inicializando 'EntityManagerFactory' para persistencia dos dados de " + this.getClass().getSimpleName());
        this.factory = Persistence.createEntityManagerFactory("webcord", databaseProperties);
    }

    public WebsiteRecord save(WebsiteRecord websiteRecord) {
        LOGGER.trace("Preparando para salvar GuildRecord");
        EntityManager entityManager = factory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(websiteRecord);
        entityManager.flush();
        entityManager.getTransaction().commit();
        entityManager.close();
        LOGGER.trace("GuildRecord salva com sucesso");
        return websiteRecord;
    }

}
