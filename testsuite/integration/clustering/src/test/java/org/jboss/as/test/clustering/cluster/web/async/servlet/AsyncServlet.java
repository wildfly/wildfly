/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.clustering.cluster.web.async.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { AsyncServlet.SERVLET_PATH }, asyncSupported = true)
public class AsyncServlet extends HttpServlet {
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
        AsyncContext context = request.startAsync(request, response);
        context.start(new AsyncTask(context));
    }

    private static class AsyncTask implements Runnable {
        private final AsyncContext context;

        AsyncTask(AsyncContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                TimeUnit.SECONDS.sleep(1);
                HttpServletRequest request = (HttpServletRequest) this.context.getRequest();
                HttpServletResponse response = (HttpServletResponse) this.context.getResponse();
                AtomicInteger value = (AtomicInteger) request.getSession().getAttribute(ATTRIBUTE);
                response.setIntHeader(VALUE_HEADER, value.incrementAndGet());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.context.complete();
            }
        }
    }
}
