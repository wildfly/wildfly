/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.session;

import org.jboss.as.web.session.SessionIdentifierCodec;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;

/**
 * {@link SessionConfig} decorator that performs encoding/decoding of the session identifier.
 * In this way, routing is completely opaque to the request, session, and session manager.
 * @author Paul Ferraro
 */
public class CodecSessionConfig implements SessionConfig {

    private final SessionConfig config;
    private final SessionIdentifierCodec codec;

    public CodecSessionConfig(SessionConfig config, SessionIdentifierCodec codec) {
        this.config = config;
        this.codec = codec;
    }

    @Override
    public void setSessionId(HttpServerExchange exchange, String sessionId) {
        CharSequence encodedSessionId = this.codec.encode(sessionId);
        String requestedSessionId = this.config.findSessionId(exchange);
        // Apply only if identifier changed
        if (!encodedSessionId.equals(requestedSessionId)) {
            this.config.setSessionId(exchange, encodedSessionId.toString());
        }
    }

    @Override
    public void clearSession(HttpServerExchange exchange, String sessionId) {
        CharSequence encodedSessionId = this.codec.encode(sessionId);
        this.config.clearSession(exchange, encodedSessionId.toString());
    }

    @Override
    public String findSessionId(HttpServerExchange exchange) {
        String requestedSessionId = this.config.findSessionId(exchange);
        return (requestedSessionId != null) ? this.codec.decode(requestedSessionId).toString() : null;
    }

    @Override
    public SessionCookieSource sessionCookieSource(HttpServerExchange exchange) {
        return this.config.sessionCookieSource(exchange);
    }

    @Override
    public String rewriteUrl(String originalUrl, String sessionId) {
        CharSequence encodedSessionId = this.codec.encode(sessionId);
        return this.config.rewriteUrl(originalUrl, encodedSessionId.toString());
    }
}
