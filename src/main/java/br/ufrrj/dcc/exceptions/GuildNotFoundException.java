package br.ufrrj.dcc.exceptions;

public class GuildNotFoundException extends Exception {

    public GuildNotFoundException(String message) {
        super(message);
    }

    public GuildNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
