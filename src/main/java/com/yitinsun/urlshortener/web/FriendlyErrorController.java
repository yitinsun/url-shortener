package com.yitinsun.urlshortener.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class FriendlyErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<String> error(HttpServletRequest request) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = 404;
        if (statusAttr instanceof Integer code) {
            status = code;
        } else if (statusAttr instanceof String text) {
            try {
                status = Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                status = 404;
            }
        }
        if (status < 400) {
            status = 404;
        }
        Object uriAttr = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String uri = uriAttr == null ? request.getRequestURI() : String.valueOf(uriAttr);
        String reason = status == 404
                ? "Unknown or disallowed URL"
                : "The server could not complete this request.";
        return FriendlyErrorPages.entity(status, reason, uri);
    }
}
