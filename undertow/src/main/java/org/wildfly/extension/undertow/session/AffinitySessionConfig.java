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

import java.util.Map;

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
    private final Map<SessionCookieSource, SessionConfig> affinityConfigMap;
    private final AffinityLocator locator;

    public AffinitySessionConfig(SessionConfig sessionConfig, Map<SessionCookieSource, SessionConfig> affinityConfigMap, AffinityLocator locator) {
        this.sessionConfig = sessionConfig;
        this.affinityConfigMap = affinityConfigMap;
        this.locator = locator;
    }

    @Override
    public void setSessionId(HttpServerExchange exchange, String sessionId) {
        String requestedSessionId = this.sessionConfig.findSessionId(exchange);
        if (!sessionId.equals(requestedSessionId)) {
            this.sessionConfig.setSessionId(exchange, sessionId);
        }

        String affinity = this.locator.locate(sessionId);
        if (affinity != null) {
            // Always write affinity for every request if using cookies!!
            this.sessionConfigInUse(exchange).setSessionId(exchange, affinity);
        }
    }

    @Override
    public void clearSession(HttpServerExchange exchange, String sessionId) {
        this.sessionConfig.clearSession(exchange, sessionId);

        SessionConfig sessionConfigInUse = sessionConfigInUse(exchange);
        String existingAffinity = sessionConfigInUse.findSessionId(exchange);
        if (existingAffinity != null) {
            sessionConfigInUse.clearSession(exchange, existingAffinity);
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

        if (route != null) {
            if (url.equals(originalUrl)) {
                // Rewritten URLs is unchanged -> use SessionCookieSource.COOKIE
                return this.affinityConfigMap.get(SessionCookieSource.COOKIE).rewriteUrl(url, route);
            } else {
                // Rewritten URL is different from the original URL -> use SessionCookieSource.URL
                return this.affinityConfigMap.get(SessionCookieSource.URL).rewriteUrl(url, route);
            }
        }

        return url;
    }

    private SessionConfig sessionConfigInUse(HttpServerExchange exchange) {
        switch (sessionConfig.sessionCookieSource(exchange)) {
            case URL: {
                return this.affinityConfigMap.get(SessionCookieSource.URL);
            }
            default: {
                return this.affinityConfigMap.get(SessionCookieSource.COOKIE);
            }
        }
    }

}