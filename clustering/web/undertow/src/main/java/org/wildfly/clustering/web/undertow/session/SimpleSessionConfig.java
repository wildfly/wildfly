/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        // No-op
    }

    @Override
    public void clearSession(HttpServerExchange exchange, String sessionId) {
        // No-op
    }

    @Override
    public String findSessionId(HttpServerExchange exchange) {
        return this.sessionId;
    }

    @Override
    public SessionCookieSource sessionCookieSource(HttpServerExchange exchange) {
        return SessionCookieSource.NONE;
    }

    @Override
    public String rewriteUrl(String originalUrl, String sessionId) {
        return originalUrl;
    }
}
