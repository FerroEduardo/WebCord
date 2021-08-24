package br.ufrrj.dcc.exceptions;

public class TextChannelNotFoundExists extends Exception{

    public TextChannelNotFoundExists(String message) {
        super(message);
    }

    public TextChannelNotFoundExists(String message, Throwable cause) {
        super(message, cause);
    }

}
