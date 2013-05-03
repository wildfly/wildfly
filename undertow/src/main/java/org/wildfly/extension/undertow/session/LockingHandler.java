/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.extension.undertow.session;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.servlet.http.HttpServletResponse;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletAttachments;
import org.jboss.logging.Logger;

/**
 * Generic valve that applies a given lock to a request.
 *
 * @author Paul Ferraro
 */
public class LockingHandler implements HttpHandler {

    protected static final Logger log = Logger.getLogger(LockingHandler.class);

    private final Lock lock;
    private final HttpHandler next;

    public LockingHandler(Lock lock, final HttpHandler next) {
        this.lock = lock;
        this.next = next;
    }


    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (log.isTraceEnabled()) {
            log.tracef("handling request %s", exchange.getRequestURI());
        }

        try {
            if (this.lock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    this.next.handleRequest(exchange);
                } finally {
                    this.lock.unlock();
                }
            } else {
                HttpServletResponse response = (HttpServletResponse) exchange.getAttachment(ServletAttachments.ATTACHMENT_KEY).getServletResponse();
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
