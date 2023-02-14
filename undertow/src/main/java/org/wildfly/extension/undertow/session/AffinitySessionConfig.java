/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import org.jboss.as.web.session.AffinityLocator;

/**
 * Decorates {@link SessionConfig} with affinity encoding into a separate a cookie.
 *
 * @author Radoslav Husar
 */
public class AffinitySessionConfig implements SessionConfig {

    private final SessionConfig sessionConfig;
    private final SessionConfig affinityConfig;
    private final AffinityLocator locator;

    public AffinitySessionConfig(SessionConfig sessionConfig, SessionConfig affinityConfig, AffinityLocator locator) {
        this.sessionConfig = sessionConfig;
        this.affinityConfig = affinityConfig;
        this.locator = locator;
    }

    @Override
    public void setSessionId(HttpServerExchange exchange, String sessionId) {
        String existingSessionId = this.sessionConfig.findSessionId(exchange);
        if (!sessionId.equals(existingSessionId)) {
            this.sessionConfig.setSessionId(exchange, sessionId);
        }

        String route = this.locator.locate(sessionId);
        if (route != null) {
            // Always write affinity cookie!
            this.affinityConfig.setSessionId(exchange, route);
        }
    }

    @Override
    public void clearSession(HttpServerExchange exchange, String sessionId) {
        this.sessionConfig.clearSession(exchange, sessionId);
        String existingRoute = this.affinityConfig.findSessionId(exchange);
        if (existingRoute != null) {
            this.affinityConfig.clearSession(exchange, existingRoute);
        }
    }

    @Override
    public String findSessionId(HttpServerExchange exchange) {
        return this.sessionConfig.findSessionId(exchange);
    }

    @Override
    public SessionCookieSource sessionCookieSource(HttpServerExchange exchange) {
        return this.sessionConfig.sessionCookieSource(exchange);
    }

    @Override
    public String rewriteUrl(String originalUrl, String sessionId) {
        String url = this.sessionConfig.rewriteUrl(originalUrl, sessionId);

        String route = this.locator.locate(sessionId);
        return (route != null) ? this.affinityConfig.rewriteUrl(url, route) : url;
    }
}