package br.ufrrj.dcc.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProgramProperties {

    private String databaseName;
    private String databaseUsername;
    private String databasePassword;
    private String token;
    private Integer timeoutSeconds;
    private Integer schedulerSeconds;

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

    @JsonIgnore
    public boolean isOk() {
        return databaseName != null && databaseUsername != null && databasePassword != null && token != null && timeoutSeconds != null && schedulerSeconds != null;
    }
}
