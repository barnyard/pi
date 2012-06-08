package com.bt.pi.app.common.entities;

public class DuplicateRegionException extends RuntimeException {
    private static final long serialVersionUID = 166689859236800664L;

    public DuplicateRegionException(String message) {
        super(message);
    }
}
