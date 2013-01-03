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
package org.jboss.as.web.session;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;
import org.jboss.servlet.http.HttpEvent;

/**
 * Generic valve that applies a given lock to a request.
 *
 * @author Paul Ferraro
 */
public class LockingValve extends ValveBase {

    protected static final Logger log = Logger.getLogger(LockingValve.class);

    private final Lock lock;

    public LockingValve(Lock lock) {
        this.lock = lock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.tracef("handling request %s", request.getRequestURI());
        }

        try {
            if (this.lock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    this.next.invoke(request, response);
                } finally {
                    this.lock.unlock();
                }
            } else {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void event(Request request, Response response, HttpEvent event) throws IOException, ServletException {
        try {
            if (this.lock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    this.next.event(request, response, event);
                } finally {
                    this.lock.unlock();
                }
            } else {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}