package com.ferroeduardo.webcord.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public class ProgramProperties {

    private String databaseName;
    private String databaseUsername;
    private String databasePassword;
    private String token;
    private Integer timeoutSeconds;
    private Integer schedulerSeconds;
    private Map<String, String> websites;
    private Map<String, String> infos;
    private Integer timeoutDetection;

    public ProgramProperties() {
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public void setDatabaseUsername(String databaseUsername) {
        this.databaseUsername = databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getSchedulerSeconds() {
        return schedulerSeconds;
    }

    public void setSchedulerSeconds(Integer schedulerSeconds) {
        this.schedulerSeconds = schedulerSeconds;
    }

    public Map<String, String> getWebsites() {
        return websites;
    }

    public void setWebsites(Map<String, String> websites) {
        this.websites = websites;
    }

    public Map<String, String> getInfos() {
        return infos;
    }

    public void setInfos(Map<String, String> infos) {
        this.infos = infos;
    }

    public Integer getTimeoutDetection() {
        return timeoutDetection;
    }

    public void setTimeoutDetection(Integer timeoutDetection) {
        this.timeoutDetection = timeoutDetection;
    }

    @JsonIgnore
    public boolean isOk() {
        return databaseName != null && databaseUsername != null && databasePassword != null && token != null && timeoutSeconds != null && schedulerSeconds != null;
    }

    @Override
    public String toString() {
        return "ProgramProperties{" +
                "databaseName='" + databaseName + '\'' +
                ", databaseUsername='" + databaseUsername + '\'' +
                ", databasePassword='" + databasePassword + '\'' +
                ", token='" + token + '\'' +
                ", timeoutSeconds=" + timeoutSeconds +
                ", schedulerSeconds=" + schedulerSeconds +
                ", websites=" + websites +
                '}';
    }

}
