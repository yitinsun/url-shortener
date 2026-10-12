package com.yitinsun.urlshortener.web;

public class ResourceNotFoundException extends RuntimeException {

    private final String shortCode;

    public ResourceNotFoundException(String shortCode) {
        super("Short code not found or expired");
        this.shortCode = shortCode;
    }

    public String getShortCode() {
        return shortCode;
    }
}
