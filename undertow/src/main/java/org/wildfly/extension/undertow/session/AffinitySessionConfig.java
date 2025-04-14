/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import java.util.Map;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;

/**
 * Decorates {@link SessionConfig} with affinity encoding into a separate a cookie.
 *
 * @author Radoslav Husar
 */
public class AffinitySessionConfig implements SessionConfig {

    private final SessionConfig sessionConfig;
    private final Map<SessionCookieSource, SessionConfig> affinityConfigMap;
    private final SessionAffinityProvider affinityProvider;

    public AffinitySessionConfig(SessionConfig sessionConfig, Map<SessionCookieSource, SessionConfig> affinityConfigMap, SessionAffinityProvider affinityProvider) {
        this.sessionConfig = sessionConfig;
        this.affinityConfigMap = affinityConfigMap;
        this.affinityProvider = affinityProvider;
    }

    @Override
    public void setSessionId(HttpServerExchange exchange, String sessionId) {
        String requestedSessionId = this.sessionConfig.findSessionId(exchange);
        if (!sessionId.equals(requestedSessionId)) {
            this.sessionConfig.setSessionId(exchange, sessionId);
        }

        String affinity = this.affinityProvider.getAffinity(sessionId).orElse(null);
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
        String route = this.affinityProvider.getAffinity(sessionId).orElse(null);

        if (route != null) {
            SessionCookieSource source = url.equals(originalUrl) ? SessionCookieSource.COOKIE : SessionCookieSource.URL;
            return this.affinityConfigMap.get(source).rewriteUrl(url, route);
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