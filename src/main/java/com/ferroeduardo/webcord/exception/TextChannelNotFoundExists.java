package com.ferroeduardo.webcord.exception;

public class TextChannelNotFoundExists extends Exception{

    public TextChannelNotFoundExists(String message) {
        super(message);
    }

    public TextChannelNotFoundExists(String message, Throwable cause) {
        super(message, cause);
    }

}
