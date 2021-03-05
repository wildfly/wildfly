/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.undertow.session;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;

/**
 * {@link SessionConfig} implementation that returns a fixed sessionId.
 * @author Paul Ferraro
 */
public class SimpleSessionConfig implements SessionConfig {

    private final String sessionId;

    public SimpleSessionConfig(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void setSessionId(HttpServerExchange exchange, String sessionId) {
        throw new IllegalStateException();
    }

    @Override
    public void clearSession(HttpServerExchange exchange, String sessionId) {
        throw new IllegalStateException();
    }

    @Override
    public String findSessionId(HttpServerExchange exchange) {
        return this.sessionId;
    }

    @Override
    public SessionCookieSource sessionCookieSource(HttpServerExchange exchange) {
        throw new IllegalStateException();
    }

    @Override
    public String rewriteUrl(String originalUrl, String sessionId) {
        throw new IllegalStateException();
    }
}
