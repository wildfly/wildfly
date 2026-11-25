/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.async.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { AsyncServlet.SERVLET_PATH }, asyncSupported = true)
public class AsyncServlet extends HttpServlet {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final long serialVersionUID = -5308818413653125145L;

    private static final String SERVLET_NAME = "async";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String VALUE_HEADER = "value";
    public static final String SESSION_ID_HEADER = "sessionId";
    static final String ATTRIBUTE = "count";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        AtomicInteger value = (AtomicInteger) session.getAttribute(ATTRIBUTE);
        if (value == null) {
            value = new AtomicInteger(0);
            session.setAttribute(ATTRIBUTE, value);
        }
        int id = COUNTER.incrementAndGet();
        this.getServletContext().log("Starting AsyncContext #" + id);
        AsyncContext context = request.startAsync(request, response);
        context.start(new AsyncTask(context, value));
    }

    private static class AsyncTask implements Runnable {
        private final AsyncContext context;
        private final AtomicInteger value;

        AsyncTask(AsyncContext context, AtomicInteger value) {
            this.context = context;
            this.value = value;
        }

        @Override
        public void run() {
            HttpServletResponse response = (HttpServletResponse) this.context.getResponse();
            try {
                TimeUnit.SECONDS.sleep(1);
                response.setIntHeader(VALUE_HEADER, this.value.incrementAndGet());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.context.complete();
            }
        }
    }
}
