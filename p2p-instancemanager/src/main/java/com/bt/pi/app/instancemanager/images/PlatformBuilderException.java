package com.bt.pi.app.instancemanager.images;

public class PlatformBuilderException extends RuntimeException {

    private static final long serialVersionUID = 434545334674936588L;

    public PlatformBuilderException(String message, Exception e) {
        super(message, e);
    }
}
