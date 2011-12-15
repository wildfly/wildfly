/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.http.server;

import static org.jboss.as.domain.http.server.Constants.GET;
import static org.jboss.as.domain.http.server.Constants.LOCATION;
import static org.jboss.as.domain.http.server.Constants.METHOD_NOT_ALLOWED;
import static org.jboss.as.domain.http.server.Constants.MOVED_PERMENANTLY;
import static org.jboss.as.domain.http.server.Constants.NOT_FOUND;

import java.io.IOException;
import java.net.URI;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpServer;

/**
 * A simple handler on the root context to redirect to the console handler.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RootHandler implements ManagementHttpHandler {

    public static final String ROOT_CONTEXT = "/";

    private final ResourceHandler consoleHandler;

    RootHandler(ResourceHandler consoleHandler){
        this.consoleHandler = consoleHandler;
    }

    public void start(HttpServer httpServer, SecurityRealm securityRealm) {
        httpServer.createContext(ROOT_CONTEXT, this);
    }

    public void stop(HttpServer httpServer) {
        httpServer.removeContext(ROOT_CONTEXT);
    }

    public void handle(HttpExchange http) throws IOException {
        final URI uri = http.getRequestURI();
        final String requestMethod = http.getRequestMethod();

        // only GET supported
        if (!GET.equals(requestMethod)) {
            http.sendResponseHeaders(METHOD_NOT_ALLOWED, -1);
            return;
        }

        String path = uri.getPath();
        if (path.equals("/") && consoleHandler != null) {
            Headers responseHeaders = http.getResponseHeaders();
            responseHeaders.add(LOCATION, consoleHandler.getDefaultUrl());
            http.sendResponseHeaders(MOVED_PERMENANTLY, 0);
            http.close();
        } else {
            http.sendResponseHeaders(NOT_FOUND, -1);
        }
    }
}
