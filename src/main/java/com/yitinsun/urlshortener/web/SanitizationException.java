package com.yitinsun.urlshortener.web;

public class SanitizationException extends IllegalArgumentException {

    public SanitizationException(String message) {
        super(message);
    }
}
