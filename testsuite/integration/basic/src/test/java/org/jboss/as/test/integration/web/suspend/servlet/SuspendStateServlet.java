/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.suspend.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { SuspendStateServlet.SERVLET_PATH })
public class SuspendStateServlet extends HttpServlet {
    private static final long serialVersionUID = -2022928695253318550L;
    private static final String SERVLET_NAME = "sleep";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String EVENT_NAME = "service";

    private final BiConsumer<ServletRequest, String> recorder = new SuspendStateRecorder();

    public static URI createURI(URL baseURL, String event, Duration sleep) throws URISyntaxException {
        return baseURL.toURI().resolve(String.format("%s?%s=%s", SERVLET_NAME, event, sleep.toString()));
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.recorder.accept(request, EVENT_NAME);
        for (Map.Entry<String, String> event : SuspendStateRecorder.drainEvents()) {
            response.addHeader(event.getKey(), event.getValue());
        }
    }
}
